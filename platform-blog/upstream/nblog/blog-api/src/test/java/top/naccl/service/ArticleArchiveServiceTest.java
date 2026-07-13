package top.naccl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class ArticleArchiveServiceTest {
	@TempDir Path uploadRoot;
	private final BlogMapper blogMapper = mock(BlogMapper.class);
	private final CommentMapper commentMapper = mock(CommentMapper.class);
	private final ImageAssetMapper imageAssetMapper = mock(ImageAssetMapper.class);
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private ArticleArchiveService service;

	@BeforeEach
	void setUp() {
		UploadProperties properties = new UploadProperties();
		properties.setPath(uploadRoot.toString());
		service = new ArticleArchiveService(blogMapper, commentMapper, imageAssetMapper, properties, objectMapper);
	}

	@Test
	void packagesOneArticleWithManagedImagesAndOfflineMarkdownLinks() throws Exception {
		Blog blog = article();
		ImageAsset cover = imageAsset(11L, 5L, "article-cover", "COVER");
		ImageAsset image = imageAsset(101L, 5L, "article-image", "CONTENT");
		writeAssetFiles(cover, "cover-original", "cover-thumbnail");
		writeAssetFiles(image, "original-image", "thumbnail-image");
		when(imageAssetMapper.findByBlogId(5L)).thenReturn(List.of(image, cover));

		Map<String, byte[]> entries = archive(output -> service.writeSingleArticle(blog, output));

		assertFalse(entries.containsKey("README.txt"));
		assertTrue(entries.containsKey("article.md"));
		assertEquals("# 最短路\n\n> **简介**\n>\n> 说明\n\n---\n\n"
				+ "![图](images/content-1-original.jpg)", text(entries, "article.md"));
		assertArrayEquals("original-image".getBytes(StandardCharsets.UTF_8),
				entries.get("images/content-1-original.jpg"));
		assertArrayEquals("cover-original".getBytes(StandardCharsets.UTF_8),
				entries.get("images/cover-original.jpg"));
		assertTrue(entries.keySet().stream().noneMatch(name -> name.matches("images/\\d+/.*")));
		assertTrue(text(entries, "metadata.json").contains("originalArchivePath"));
	}

	@Test
	void preservesMultilineDescriptionInReadableMarkdownHeader() throws Exception {
		Blog blog = article();
		blog.setDescription("第一行\r\n\r\n第二行");
		blog.setContent("正文");
		when(imageAssetMapper.findByBlogId(5L)).thenReturn(List.of());

		Map<String, byte[]> entries = archive(output -> service.writeSingleArticle(blog, output));

		assertEquals("# 最短路\n\n> **简介**\n>\n> 第一行\n> \n> 第二行\n\n---\n\n正文",
				text(entries, "article.md"));
	}

	@Test
	void backsUpAllArticleStatesCommentsAuthorsAvatarsAndImagesWithoutSecrets() throws Exception {
		Blog blog = article();
		blog.setDeletedAt(new Date());
		ImageAsset image = imageAsset(101L, 5L, "article-image", "CONTENT");
		ImageAsset avatar = imageAsset(202L, null, "author-avatar", null);
		writeAssetFiles(image, "article-original", "article-thumbnail");
		writeAssetFiles(avatar, "avatar-original", "avatar-thumbnail");
		ArticleBackupComment comment = new ArticleBackupComment();
		comment.setId(301L);
		comment.setBlogId(5L);
		comment.setUserId(7L);
		comment.setUsername("alice");
		comment.setNickname("Alice");
		comment.setContent("写得很好");
		comment.setPublished(true);
		comment.setParentCommentId(-1L);

		when(blogMapper.getAllBlogsForBackup()).thenReturn(List.of(blog));
		when(commentMapper.getArticleBackupComments(List.of(5L))).thenReturn(List.of(comment));
		when(imageAssetMapper.findByBlogIds(List.of(5L))).thenReturn(List.of(image));
		when(imageAssetMapper.findByIds(List.of(202L))).thenReturn(List.of(avatar));

		Map<String, byte[]> entries = archive(service::writeAllArticlesBackup);

		assertFalse(entries.containsKey("README.txt"));
		JsonNode manifest = objectMapper.readTree(entries.get("manifest.json"));
		assertEquals(1, manifest.get("articleCount").asInt());
		assertEquals(1, manifest.get("commentCount").asInt());
		assertEquals(1, manifest.get("authorCount").asInt());
		String authors = text(entries, "authors.json");
		assertTrue(authors.contains("alice"));
		assertFalse(authors.contains("password"));
		String comments = text(entries, "articles/5/comments.json");
		assertTrue(comments.contains("写得很好"));
		assertFalse(comments.contains("ip"));
		assertFalse(comments.contains("email"));
		assertEquals("# 最短路\n\n> **简介**\n>\n> 说明\n\n---\n\n"
				+ "![图](images/content-1-original.jpg)", text(entries, "articles/5/article.md"));
		assertArrayEquals("article-original".getBytes(StandardCharsets.UTF_8),
				entries.get("articles/5/images/content-1-original.jpg"));
		assertArrayEquals("avatar-original".getBytes(StandardCharsets.UTF_8),
				entries.get("avatars/7/avatar-original.jpg"));
	}

	private Blog article() {
		User author = new User();
		author.setId(7L);
		author.setUsername("alice");
		author.setNickname("Alice");
		author.setPassword("must-not-be-exported");
		author.setEmail("alice@example.com");
		author.setSignature("训练中");
		author.setRole("ROLE_player");
		author.setAvatarAssetId(202L);
		Category category = new Category();
		category.setId(2L);
		category.setName("题解");
		Tag tag = new Tag();
		tag.setId(3L);
		tag.setName("图论");
		Blog blog = new Blog();
		blog.setId(5L);
		blog.setTitle("最短路");
		blog.setContent("![图](/api/image/assets/article-image/original.jpg)");
		blog.setDescription("说明");
		blog.setPublished(true);
		blog.setInternal(false);
		blog.setUser(author);
		blog.setCategory(category);
		blog.setTags(List.of(tag));
		return blog;
	}

	private ImageAsset imageAsset(Long id, Long blogId, String publicId, String role) {
		ImageAsset asset = new ImageAsset();
		asset.setId(id);
		asset.setBlogId(blogId);
		asset.setPublicId(publicId);
		asset.setPurpose(blogId == null ? "AVATAR" : "ARTICLE_CONTENT");
		asset.setReferenceRole(role);
		asset.setOriginalPath("assets/" + publicId + "/original.jpg");
		asset.setThumbnailPath("assets/" + publicId + "/thumbnail.jpg");
		asset.setOriginalUrl("/api/image/assets/" + publicId + "/original.jpg");
		asset.setThumbnailUrl("/api/image/assets/" + publicId + "/thumbnail.jpg");
		asset.setMimeType("image/jpeg");
		return asset;
	}

	private void writeAssetFiles(ImageAsset asset, String original, String thumbnail) throws Exception {
		Path originalPath = uploadRoot.resolve(asset.getOriginalPath());
		Files.createDirectories(originalPath.getParent());
		Files.writeString(originalPath, original);
		Files.writeString(uploadRoot.resolve(asset.getThumbnailPath()), thumbnail);
	}

	private static Map<String, byte[]> archive(ArchiveWriter writer) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		writer.write(output);
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()),
				StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				entries.put(entry.getName(), zip.readAllBytes());
			}
		}
		return entries;
	}

	private static String text(Map<String, byte[]> entries, String name) {
		return new String(entries.get(name), StandardCharsets.UTF_8);
	}

	@FunctionalInterface
	private interface ArchiveWriter {
		void write(ByteArrayOutputStream output) throws Exception;
	}
}
