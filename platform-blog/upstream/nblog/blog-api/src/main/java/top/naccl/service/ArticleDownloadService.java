package top.naccl.service;

import org.springframework.stereotype.Service;
import top.naccl.entity.Blog;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;

/**
 * 读取可归档的已发布文章，并在普通用户下载前执行跨文章频率限制。
 *
 * @author huangbingrui.awa
 */
@Service
public class ArticleDownloadService {
	private final BlogMapper blogMapper;
	private final ArticleDownloadRateLimiter rateLimiter;

	public ArticleDownloadService(BlogMapper blogMapper, ArticleDownloadRateLimiter rateLimiter) {
		this.blogMapper = blogMapper;
		this.rateLimiter = rateLimiter;
	}

	public Blog download(String username, boolean admin, Long blogId) {
		Blog blog = blogMapper.getPublishedBlogForDownload(blogId);
		if (blog == null) {
			throw new NotFoundException("该博客不存在");
		}
		if (!admin) {
			rateLimiter.acquire(username);
		}
		return blog;
	}
}
