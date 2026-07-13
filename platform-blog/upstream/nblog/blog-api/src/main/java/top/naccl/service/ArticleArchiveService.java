package top.naccl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.config.properties.UploadProperties;
import top.naccl.entity.Blog;
import top.naccl.entity.Category;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.Tag;
import top.naccl.entity.User;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.ImageAssetMapper;
import top.naccl.model.vo.ArticleBackupComment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 将单篇文章或全站文章数据流式写入可迁移 ZIP，图片只读取本地托管资产。
 *
 * @author huangbingrui.awa
 */
@Service
public class ArticleArchiveService {
	private final BlogMapper blogMapper;
	private final CommentMapper commentMapper;
	private final ImageAssetMapper imageAssetMapper;
	private final UploadProperties uploadProperties;
	private final ObjectWriter jsonWriter;

	public ArticleArchiveService(BlogMapper blogMapper, CommentMapper commentMapper,
			ImageAssetMapper imageAssetMapper, UploadProperties uploadProperties, ObjectMapper objectMapper) {
		this.blogMapper = blogMapper;
		this.commentMapper = commentMapper;
		this.imageAssetMapper = imageAssetMapper;
		this.uploadProperties = uploadProperties;
		this.jsonWriter = objectMapper.writerWithDefaultPrettyPrinter();
	}

	@Transactional(readOnly = true)
	public void writeSingleArticle(Blog blog, OutputStream output) throws IOException {
		List<ImageAsset> assets = imageAssetMapper.findByBlogId(blog.getId());
		List<String> warnings = new ArrayList<>();
		List<ArchivedAsset> archivedAssets = prepareArticleAssets("", assets, warnings);
		try (ZipOutputStream zip = zip(output)) {
			writeText(zip, "article.md", renderArticleMarkdown(blog, archivedAssets));
			writeJson(zip, "metadata.json", articleMetadata(blog, archivedAssets));
			if (!warnings.isEmpty()) {
				writeJson(zip, "warnings.json", warnings);
			}
			writeAssetFiles(zip, archivedAssets);
		}
	}

	@Transactional(readOnly = true)
	public void writeAllArticlesBackup(OutputStream output) throws IOException {
		List<Blog> blogs = blogMapper.getAllBlogsForBackup();
		List<Long> blogIds = blogs.stream().map(Blog::getId).toList();
		List<ArticleBackupComment> comments = blogIds.isEmpty()
				? List.of() : commentMapper.getArticleBackupComments(blogIds);
		List<ImageAsset> articleAssets = blogIds.isEmpty()
				? List.of() : imageAssetMapper.findByBlogIds(blogIds);
		Map<Long, User> authors = authorsById(blogs);
		List<Long> avatarIds = authors.values().stream()
				.map(User::getAvatarAssetId).filter(Objects::nonNull).distinct().toList();
		Map<Long, ImageAsset> avatars = avatarIds.isEmpty() ? Map.of()
				: imageAssetMapper.findByIds(avatarIds).stream()
				.collect(Collectors.toMap(ImageAsset::getId, Function.identity()));

		Map<Long, List<ArticleBackupComment>> commentsByBlog = comments.stream()
				.collect(Collectors.groupingBy(ArticleBackupComment::getBlogId, LinkedHashMap::new, Collectors.toList()));
		Map<Long, List<ImageAsset>> assetsByBlog = articleAssets.stream()
				.collect(Collectors.groupingBy(ImageAsset::getBlogId, LinkedHashMap::new, Collectors.toList()));
		List<String> warnings = new ArrayList<>();
		Map<Long, List<ArchivedAsset>> archivedAssetsByBlog = new LinkedHashMap<>();
		for (Blog blog : blogs) {
			String root = "articles/" + blog.getId() + "/";
			archivedAssetsByBlog.put(blog.getId(),
					prepareArticleAssets(root, assetsByBlog.getOrDefault(blog.getId(), List.of()), warnings));
		}
		Map<Long, ArchivedAsset> archivedAvatars = new LinkedHashMap<>();
		for (User author : authors.values()) {
			ImageAsset avatar = avatars.get(author.getAvatarAssetId());
			if (avatar != null) {
				archivedAvatars.put(author.getId(), prepareAsset(avatar, "",
						"avatars/" + author.getId() + "/avatar", warnings));
			}
		}

		Map<String, Object> manifest = linkedMap();
		manifest.put("schemaVersion", 1);
		manifest.put("generatedAt", Instant.now().toString());
		manifest.put("scope", "all-database-articles-including-drafts-internal-and-recycle-bin");
		manifest.put("articleCount", blogs.size());
		manifest.put("commentCount", comments.size());
		manifest.put("authorCount", authors.size());
		manifest.put("articleImageAssetCount", articleAssets.size());
		manifest.put("authorAvatarAssetCount", archivedAvatars.size());
		manifest.put("excludedSensitiveFields", List.of(
				"user.password", "token", "comment.ip", "comment.email", "comment.notice", "comment.qq"));
		manifest.put("warnings", warnings);

		try (ZipOutputStream zip = zip(output)) {
			writeJson(zip, "manifest.json", manifest);
			writeJson(zip, "authors.json", authors.values().stream()
					.map(author -> authorMetadata(author, archivedAvatars.get(author.getId())))
					.toList());
			for (Blog blog : blogs) {
				String root = "articles/" + blog.getId() + "/";
				List<ArchivedAsset> archivedAssets = archivedAssetsByBlog.getOrDefault(blog.getId(), List.of());
				writeText(zip, root + "article.md", renderArticleMarkdown(blog, archivedAssets));
				writeJson(zip, root + "metadata.json", articleMetadata(blog, archivedAssets));
				writeJson(zip, root + "comments.json", commentsByBlog.getOrDefault(blog.getId(), List.of()));
				writeAssetFiles(zip, archivedAssets);
			}
			writeAssetFiles(zip, archivedAvatars.values());
		}
	}

	private List<ArchivedAsset> prepareArticleAssets(String archiveRoot, List<ImageAsset> assets,
			List<String> warnings) {
		List<ImageAsset> sortedAssets = assets.stream()
				.sorted(Comparator.comparing(ImageAsset::getId))
				.toList();
		List<ArchivedAsset> archivedAssets = new ArrayList<>(sortedAssets.size());
		int coverIndex = 0;
		int contentIndex = 0;
		int otherIndex = 0;
		for (ImageAsset asset : sortedAssets) {
			String stem;
			if ("COVER".equals(asset.getReferenceRole())) {
				coverIndex++;
				stem = coverIndex == 1 ? "cover" : "cover-" + coverIndex;
			} else if ("CONTENT".equals(asset.getReferenceRole())) {
				stem = "content-" + (++contentIndex);
			} else {
				stem = "asset-" + (++otherIndex);
			}
			archivedAssets.add(prepareAsset(asset, archiveRoot, "images/" + stem, warnings));
		}
		return List.copyOf(archivedAssets);
	}

	private ArchivedAsset prepareAsset(ImageAsset asset, String archiveRoot, String relativeFilePrefix,
			List<String> warnings) {
		Path original = resolveManagedFile(asset.getOriginalPath());
		Path thumbnail = resolveManagedFile(asset.getThumbnailPath());
		String originalRelative = original == null ? null
				: relativeFilePrefix + "-" + safeLeafName(asset.getOriginalPath(), "original.bin");
		String thumbnailRelative = thumbnail == null ? null
				: relativeFilePrefix + "-" + safeLeafName(asset.getThumbnailPath(), "thumbnail.bin");
		if (original == null) {
			warnings.add("imageAsset " + asset.getId() + " original file is missing");
		}
		if (thumbnail == null) {
			warnings.add("imageAsset " + asset.getId() + " thumbnail file is missing");
		}
		return new ArchivedAsset(asset, original, thumbnail,
				originalRelative, thumbnailRelative,
				originalRelative == null ? null : archiveRoot + originalRelative,
				thumbnailRelative == null ? null : archiveRoot + thumbnailRelative);
	}

	private Path resolveManagedFile(String relativePath) {
		if (relativePath == null || relativePath.isBlank() || uploadProperties.getPath() == null) {
			return null;
		}
		try {
			Path root = Path.of(uploadProperties.getPath()).toAbsolutePath().normalize();
			Path candidate = root.resolve(relativePath).normalize();
			if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
				return null;
			}
			Path realRoot = root.toRealPath();
			Path realCandidate = candidate.toRealPath();
			return realCandidate.startsWith(realRoot) ? realCandidate : null;
		} catch (IOException | InvalidPathException exception) {
			return null;
		}
	}

	private static Map<Long, User> authorsById(List<Blog> blogs) {
		Map<Long, User> authors = new LinkedHashMap<>();
		for (Blog blog : blogs) {
			User author = blog.getUser();
			if (author != null && author.getId() != null) {
				authors.putIfAbsent(author.getId(), author);
			}
		}
		return authors;
	}

	private static String rewriteManagedImageUrls(String markdown, List<ArchivedAsset> assets) {
		String rewritten = markdown == null ? "" : markdown;
		for (ArchivedAsset archived : assets) {
			ImageAsset asset = archived.asset();
			if (archived.originalRelativePath() != null && asset.getOriginalUrl() != null) {
				rewritten = rewritten.replace(asset.getOriginalUrl(), archived.originalRelativePath());
			}
			if (archived.thumbnailRelativePath() != null && asset.getThumbnailUrl() != null) {
				rewritten = rewritten.replace(asset.getThumbnailUrl(), archived.thumbnailRelativePath());
			}
		}
		return rewritten;
	}

	private static String renderArticleMarkdown(Blog blog, List<ArchivedAsset> assets) {
		String title = blog.getTitle() == null ? "" : blog.getTitle().replaceAll("\\R+", " ").trim();
		String description = blog.getDescription() == null ? ""
				: blog.getDescription().replace("\r\n", "\n").replace('\r', '\n').strip();
		String content = rewriteManagedImageUrls(blog.getContent(), assets);
		StringBuilder markdown = new StringBuilder();
		if (!title.isBlank()) {
			markdown.append("# ").append(title).append("\n\n");
		}
		if (!description.isBlank()) {
			markdown.append("> **简介**\n>\n");
			for (String line : description.split("\n", -1)) {
				markdown.append("> ").append(line).append('\n');
			}
			markdown.append('\n');
		}
		if (!content.isBlank() && !markdown.isEmpty()) {
			markdown.append("---\n\n");
		}
		return markdown.append(content).toString();
	}

	private static Map<String, Object> articleMetadata(Blog blog, List<ArchivedAsset> assets) {
		Map<String, Object> metadata = linkedMap();
		metadata.put("id", blog.getId());
		metadata.put("title", blog.getTitle());
		metadata.put("description", blog.getDescription());
		metadata.put("firstPicture", blog.getFirstPicture());
		metadata.put("firstPictureAssetId", blog.getFirstPictureAssetId());
		metadata.put("published", blog.getPublished());
		metadata.put("internal", blog.getInternal());
		metadata.put("recommend", blog.getRecommend());
		metadata.put("appreciation", blog.getAppreciation());
		metadata.put("commentEnabled", blog.getCommentEnabled());
		metadata.put("top", blog.getTop());
		metadata.put("createTime", blog.getCreateTime());
		metadata.put("updateTime", blog.getUpdateTime());
		metadata.put("deletedAt", blog.getDeletedAt());
		metadata.put("words", blog.getWords());
		metadata.put("readTime", blog.getReadTime());
		metadata.put("authorId", blog.getUser() == null ? null : blog.getUser().getId());
		metadata.put("authorUsername", blog.getUser() == null ? null : blog.getUser().getUsername());
		metadata.put("category", categoryMetadata(blog.getCategory()));
		metadata.put("tags", blog.getTags() == null ? List.of() : blog.getTags().stream()
				.filter(Objects::nonNull).map(ArticleArchiveService::tagMetadata).toList());
		metadata.put("imageAssets", assets.stream().map(ArticleArchiveService::assetMetadata).toList());
		return metadata;
	}

	private static Map<String, Object> authorMetadata(User author, ArchivedAsset avatar) {
		Map<String, Object> metadata = linkedMap();
		metadata.put("id", author.getId());
		metadata.put("username", author.getUsername());
		metadata.put("nickname", author.getNickname());
		metadata.put("signature", author.getSignature());
		metadata.put("email", author.getEmail());
		metadata.put("role", author.getRole());
		metadata.put("avatar", author.getAvatar());
		metadata.put("avatarAssetId", author.getAvatarAssetId());
		metadata.put("createTime", author.getCreateTime());
		metadata.put("updateTime", author.getUpdateTime());
		metadata.put("avatarAsset", avatar == null ? null : assetMetadata(avatar));
		return metadata;
	}

	private static Map<String, Object> categoryMetadata(Category category) {
		if (category == null || category.getId() == null) {
			return null;
		}
		Map<String, Object> metadata = linkedMap();
		metadata.put("id", category.getId());
		metadata.put("name", category.getName());
		metadata.put("color", category.getColor());
		return metadata;
	}

	private static Map<String, Object> tagMetadata(Tag tag) {
		Map<String, Object> metadata = linkedMap();
		metadata.put("id", tag.getId());
		metadata.put("name", tag.getName());
		metadata.put("color", tag.getColor());
		return metadata;
	}

	private static Map<String, Object> assetMetadata(ArchivedAsset archived) {
		ImageAsset asset = archived.asset();
		Map<String, Object> metadata = linkedMap();
		metadata.put("id", asset.getId());
		metadata.put("publicId", asset.getPublicId());
		metadata.put("purpose", asset.getPurpose());
		metadata.put("referenceRole", asset.getReferenceRole());
		metadata.put("mimeType", asset.getMimeType());
		metadata.put("width", asset.getWidth());
		metadata.put("height", asset.getHeight());
		metadata.put("originalBytes", asset.getOriginalBytes());
		metadata.put("thumbnailBytes", asset.getThumbnailBytes());
		metadata.put("originalUrl", asset.getOriginalUrl());
		metadata.put("thumbnailUrl", asset.getThumbnailUrl());
		metadata.put("originalArchivePath", archived.originalEntryName());
		metadata.put("thumbnailArchivePath", archived.thumbnailEntryName());
		return metadata;
	}

	private void writeJson(ZipOutputStream zip, String name, Object value) throws IOException {
		writeBytes(zip, name, jsonWriter.writeValueAsBytes(value));
	}

	private static void writeText(ZipOutputStream zip, String name, String value) throws IOException {
		writeBytes(zip, name, value.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeBytes(ZipOutputStream zip, String name, byte[] value) throws IOException {
		putEntry(zip, name);
		zip.write(value);
		zip.closeEntry();
	}

	private static void writeAssetFiles(ZipOutputStream zip, Iterable<ArchivedAsset> assets) throws IOException {
		for (ArchivedAsset asset : assets) {
			writeFile(zip, asset.originalEntryName(), asset.originalFile());
			writeFile(zip, asset.thumbnailEntryName(), asset.thumbnailFile());
		}
	}

	private static void writeFile(ZipOutputStream zip, String name, Path file) throws IOException {
		if (name == null || file == null) {
			return;
		}
		try (InputStream input = Files.newInputStream(file)) {
			putEntry(zip, name);
			input.transferTo(zip);
			zip.closeEntry();
		}
	}

	private static void putEntry(ZipOutputStream zip, String name) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setTime(0L);
		zip.putNextEntry(entry);
	}

	private static ZipOutputStream zip(OutputStream output) {
		ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8);
		zip.setLevel(Deflater.BEST_SPEED);
		return zip;
	}

	private static String safeLeafName(String relativePath, String fallback) {
		try {
			String leaf = Path.of(relativePath).getFileName().toString()
					.replaceAll("[^A-Za-z0-9._-]", "_");
			return leaf.isBlank() ? fallback : leaf;
		} catch (InvalidPathException | NullPointerException exception) {
			return fallback;
		}
	}

	private static Map<String, Object> linkedMap() {
		return new LinkedHashMap<>();
	}

	private record ArchivedAsset(ImageAsset asset, Path originalFile, Path thumbnailFile,
			String originalRelativePath, String thumbnailRelativePath,
			String originalEntryName, String thumbnailEntryName) {
	}
}
