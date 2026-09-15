package top.naccl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Blog;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.ImageAssetMapper;
import top.naccl.model.vo.ArticleBackupComment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在同一个数据库快照中读完归档元数据，返回后释放连接；文件读取与 ZIP 输出由归档服务完成。
 * 全站元数据仍一次驻留内存，图片文件不加载进快照。
 *
 * @author huangbingrui.awa
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ArticleArchiveSnapshotReader {
	private final BlogMapper blogMapper;
	private final CommentMapper commentMapper;
	private final ImageAssetMapper imageAssetMapper;

	public ArticleArchiveSnapshotReader(BlogMapper blogMapper, CommentMapper commentMapper,
			ImageAssetMapper imageAssetMapper) {
		this.blogMapper = blogMapper;
		this.commentMapper = commentMapper;
		this.imageAssetMapper = imageAssetMapper;
	}

	public List<ImageAsset> readArticleAssets(Long blogId) {
		return List.copyOf(imageAssetMapper.findByBlogId(blogId));
	}

	public Snapshot readAllArticles() {
		List<Blog> blogs = blogMapper.getAllBlogsForBackup();
		List<Long> blogIds = blogs.stream().map(Blog::getId).toList();
		List<ArticleBackupComment> comments = blogIds.isEmpty()
				? List.of() : commentMapper.getArticleBackupComments(blogIds);
		List<ImageAsset> articleAssets = blogIds.isEmpty()
				? List.of() : imageAssetMapper.findByBlogIds(blogIds);
		Map<Long, User> authors = new LinkedHashMap<>();
		for (Blog blog : blogs) {
			User author = blog.getUser();
			if (author != null && author.getId() != null) {
				authors.putIfAbsent(author.getId(), author);
			}
		}
		List<Long> avatarIds = authors.values().stream()
				.map(User::getAvatarAssetId).filter(Objects::nonNull).distinct().toList();
		Map<Long, ImageAsset> avatars = avatarIds.isEmpty() ? Map.of()
				: imageAssetMapper.findByIds(avatarIds).stream()
				.collect(Collectors.toMap(ImageAsset::getId, Function.identity()));
		return new Snapshot(List.copyOf(blogs), List.copyOf(comments), List.copyOf(articleAssets),
				Collections.unmodifiableMap(authors), Map.copyOf(avatars));
	}

	public record Snapshot(List<Blog> blogs, List<ArticleBackupComment> comments, List<ImageAsset> articleAssets,
			Map<Long, User> authors, Map<Long, ImageAsset> avatars) {
	}
}
