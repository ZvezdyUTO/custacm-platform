package top.naccl.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.Blog;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.BlogMapper;
import top.naccl.model.vo.BlogDetail;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.PageResult;
import top.naccl.model.vo.RandomBlog;
import top.naccl.model.vo.SearchBlog;
import top.naccl.service.BlogService;
import top.naccl.service.RedisService;
import top.naccl.service.TagService;
import top.naccl.util.markdown.MarkdownUtils;

import java.util.List;

/**
 * @Description: 博客文章业务层实现
 * @Author: Naccl
 * @Date: 2020-07-29
 */
@Service
public class BlogServiceImpl implements BlogService {
	@Autowired
	BlogMapper blogMapper;
	@Autowired
	TagService tagService;
	@Autowired
	RedisService redisService;
	//随机博客显示5条
	private static final int randomBlogLimitNum = 5;
	//每页显示5条博客简介
	private static final int pageSize = 5;
	//博客简介列表排序方式
	private static final String orderBy = "is_top desc, create_time desc";

	@Override
	public List<Blog> getListByTitleAndCategoryId(String title, Integer categoryId) {
		return blogMapper.getListByTitleAndCategoryId(title, categoryId);
	}

	@Override
	public List<SearchBlog> getSearchBlogListByQueryAndIsPublished(String query, boolean includeInternal) {
		return blogMapper.getSearchBlogListByQueryAndIsPublished(query, includeInternal);
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByIsPublished(Integer pageNum, boolean includeInternal) {
		if (includeInternal) {
			PageHelper.startPage(pageNum, pageSize, orderBy);
			return pageResult(blogMapper.getBlogInfoListByIsPublished(true));
		}
		String redisKey = RedisKeyConstants.HOME_BLOG_INFO_LIST;
		//redis已有当前页缓存
		PageResult<BlogInfo> pageResultFromRedis = redisService.getBlogInfoPageResultByHash(redisKey, pageNum);
		if (pageResultFromRedis != null) {
			return pageResultFromRedis;
		}
		//redis没有缓存，从数据库查询，并添加缓存
		PageHelper.startPage(pageNum, pageSize, orderBy);
		PageResult<BlogInfo> pageResult = pageResult(blogMapper.getBlogInfoListByIsPublished(false));
		//添加首页缓存
		redisService.saveKVToHash(redisKey, pageNum, pageResult);
		return pageResult;
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByCategoryNameAndIsPublished(String categoryName, Integer pageNum,
			boolean includeInternal) {
		PageHelper.startPage(pageNum, pageSize, orderBy);
		return pageResult(blogMapper.getBlogInfoListByCategoryNameAndIsPublished(categoryName, includeInternal));
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoListByTagNameAndIsPublished(String tagName, Integer pageNum,
			boolean includeInternal) {
		PageHelper.startPage(pageNum, pageSize, orderBy);
		return pageResult(blogMapper.getBlogInfoListByTagNameAndIsPublished(tagName, includeInternal));
	}

	private PageResult<BlogInfo> pageResult(List<BlogInfo> blogInfos) {
		List<BlogInfo> processed = processBlogInfos(blogInfos);
		PageInfo<BlogInfo> pageInfo = new PageInfo<>(processed);
		return new PageResult<>(pageInfo.getPages(), pageInfo.getList());
	}

	private List<BlogInfo> processBlogInfos(List<BlogInfo> blogInfos) {
		var tagsByBlogId = tagService.getTagListsByBlogIds(
				blogInfos.stream().map(BlogInfo::getId).toList()
		);
		for (BlogInfo blogInfo : blogInfos) {
			blogInfo.setDescription(MarkdownUtils.markdownToHtmlExtensions(blogInfo.getDescription()));
			blogInfo.setTags(tagsByBlogId.getOrDefault(blogInfo.getId(), List.of()));
		}
		return blogInfos;
	}

	@Override
	public List<RandomBlog> getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(boolean includeInternal) {
		return blogMapper.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(randomBlogLimitNum, includeInternal);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteBlogTagByBlogId(Long blogId) {
		//没有标签也是合法状态，删除 0 行不代表失败。
		blogMapper.deleteBlogTagByBlogId(blogId);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveBlog(top.naccl.model.dto.Blog blog) {
		if (blogMapper.saveBlog(blog) != 1) {
			throw new PersistenceException("添加博客失败");
		}
		deleteBlogRedisCache();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveBlogTag(Long blogId, Long tagId) {
		if (blogMapper.saveBlogTag(blogId, tagId) != 1) {
			throw new PersistenceException("维护博客标签关联表失败");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlogRecommendById(Long blogId, Boolean recommend) {
		if (blogMapper.updateBlogRecommendById(blogId, recommend) != 1) {
			throw new PersistenceException("操作失败");
		}
	}

	@Override
	public BlogDetail getBlogByIdAndIsPublished(Long id) {
		BlogDetail blog = blogMapper.getBlogByIdAndIsPublished(id);
		return prepareBlogDetail(blog);
	}

	@Override
	public BlogDetail getInternalBlogById(Long id) {
		return prepareBlogDetail(blogMapper.getInternalBlogById(id));
	}

	private BlogDetail prepareBlogDetail(BlogDetail blog) {
		if (blog == null) {
			throw new NotFoundException("该博客不存在");
		}
		blog.setContent(MarkdownUtils.markdownToHtmlExtensions(blog.getContent()));
		return blog;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateBlog(top.naccl.model.dto.Blog blog) {
		if (blogMapper.updateBlog(blog) != 1) {
			throw new PersistenceException("更新博客失败");
		}
		deleteBlogRedisCache();
	}

	@Override
	public int countBlogByCategoryId(Long categoryId) {
		return blogMapper.countBlogByCategoryId(categoryId);
	}

	@Override
	public int countBlogByTagId(Long tagId) {
		return blogMapper.countBlogByTagId(tagId);
	}

	@Override
	public Boolean getCommentEnabledByBlogId(Long blogId) {
		return blogMapper.getCommentEnabledByBlogId(blogId);
	}

	@Override
	public Boolean getPublishedByBlogId(Long blogId) {
		return blogMapper.getPublishedByBlogId(blogId);
	}

	@Override
	public Boolean getInternalByBlogId(Long blogId) {
		return blogMapper.getInternalByBlogId(blogId);
	}

	/**
	 * 删除首页文章缓存
	 */
	private void deleteBlogRedisCache() {
		redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}
}
