package top.naccl.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.Blog;
import top.naccl.entity.User;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.UserMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * 文章回收站用例，固定保留七天后才允许物理清理。
 *
 * @author huangbingrui.awa
 */
@Service
public class ArticleRecycleBinService {
	public static final Duration RETENTION = Duration.ofDays(7);

	private final BlogMapper blogMapper;
	private final CommentMapper commentMapper;
	private final UserMapper userMapper;
	private final ImageAssetService imageAssetService;
	private final RedisService redisService;
	private final Clock clock;

	@Autowired
	public ArticleRecycleBinService(BlogMapper blogMapper, CommentMapper commentMapper, UserMapper userMapper,
			ImageAssetService imageAssetService, RedisService redisService) {
		this(blogMapper, commentMapper, userMapper, imageAssetService, redisService, Clock.systemUTC());
	}

	ArticleRecycleBinService(BlogMapper blogMapper, CommentMapper commentMapper, UserMapper userMapper,
			ImageAssetService imageAssetService, RedisService redisService, Clock clock) {
		this.blogMapper = blogMapper;
		this.commentMapper = commentMapper;
		this.userMapper = userMapper;
		this.imageAssetService = imageAssetService;
		this.redisService = redisService;
		this.clock = clock;
	}

	public PageInfo<Blog> listForAdmin(String title, Integer categoryId, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize, "deleted_at desc");
		return new PageInfo<>(blogMapper.getRecycleBinList(title, categoryId, cutoff()));
	}

	public PageInfo<Blog> listForOwner(String username, String title, Integer categoryId,
			int pageNum, int pageSize) {
		User user = requireUser(username);
		PageHelper.startPage(pageNum, pageSize, "deleted_at desc");
		return new PageInfo<>(blogMapper.getRecycleBinListByUserId(
				title, categoryId, user.getId(), cutoff()));
	}

	@Transactional(rollbackFor = Exception.class)
	public void moveToRecycleBin(Long blogId) {
		if (blogMapper.moveBlogToRecycleBin(blogId, now()) != 1) {
			throw new NotFoundException("文章不存在或已在回收站");
		}
		clearArticleCache();
	}

	@Transactional(rollbackFor = Exception.class)
	public void moveOwnedToRecycleBin(String username, Long blogId) {
		User user = requireUser(username);
		if (blogMapper.moveOwnedBlogToRecycleBin(blogId, user.getId(), now()) != 1) {
			throw new NotFoundException("文章不存在、不属于当前用户或已在回收站");
		}
		clearArticleCache();
	}

	@Transactional(rollbackFor = Exception.class)
	public void restore(Long blogId) {
		if (blogMapper.restoreBlogFromRecycleBin(blogId, cutoff()) != 1) {
			throw new NotFoundException("文章不存在或已超过七天恢复期限");
		}
		clearArticleCache();
	}

	@Transactional(rollbackFor = Exception.class)
	public void restoreOwned(String username, Long blogId) {
		User user = requireUser(username);
		if (blogMapper.restoreOwnedBlogFromRecycleBin(blogId, user.getId(), cutoff()) != 1) {
			throw new NotFoundException("文章不存在、不属于当前用户或已超过七天恢复期限");
		}
		clearArticleCache();
	}

	/**
	 * 清理已满七天的文章。过期文章先在同一事务中加行锁，避免与恢复请求竞态。
	 */
	@Transactional(rollbackFor = Exception.class)
	public int purgeExpired() {
		Date cutoff = cutoff();
		List<Long> blogIds = blogMapper.findExpiredRecycleBinBlogIds(cutoff);
		for (Long blogId : blogIds) {
			imageAssetService.prepareBlogDeletion(blogId);
			commentMapper.deleteCommentsByBlogId(blogId);
			if (blogMapper.deleteBlogById(blogId) != 1) {
				throw new PersistenceException("回收站文章清理失败");
			}
		}
		if (!blogIds.isEmpty()) {
			clearArticleCache();
		}
		return blogIds.size();
	}

	private User requireUser(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	private Date now() {
		return Date.from(clock.instant());
	}

	private Date cutoff() {
		return Date.from(clock.instant().minus(RETENTION));
	}

	private void clearArticleCache() {
		redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}
}
