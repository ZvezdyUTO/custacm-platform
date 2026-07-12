package top.naccl.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.UserMapper;
import top.naccl.util.StringUtils;
import top.naccl.util.BlogContentLimits;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Player 文章用例。作者和受管理员控制的字段只在服务端决定。
 *
 * @author huangbingrui.awa
 */
@Service
public class PlayerBlogService {
	@Autowired private BlogMapper blogMapper;
	@Autowired private UserMapper userMapper;
	@Autowired private CategoryService categoryService;
	@Autowired private TagService tagService;
	@Autowired private BlogService blogService;
	@Autowired private CommentService commentService;
	@Autowired private ImageAssetService imageAssetService;

	public PageInfo<top.naccl.entity.Blog> list(String username, String title, Integer categoryId,
	                                           int pageNum, int pageSize) {
		User user = requireUser(username);
		PageHelper.startPage(pageNum, pageSize, "create_time desc");
		return new PageInfo<>(blogMapper.getListByTitleAndCategoryIdAndUserId(title, categoryId, user.getId()));
	}

	public top.naccl.entity.Blog get(String username, Long blogId) {
		top.naccl.entity.Blog blog = requireOwnedBlog(requireUser(username), blogId);
		blog.setCoverImage(imageAssetService.response(blog.getFirstPictureAssetId()));
		return blog;
	}

	@Transactional(rollbackFor = Exception.class)
	public Long create(String username, top.naccl.model.dto.Blog blog) {
		validateCommonFields(blog);
		normalizeOptionalFields(blog);
		User user = requireUser(username);
		ImageAssetService.PreparedBlogAssets assets = imageAssetService.prepareBlogAssets(
				user.getId(), null, blog.getFirstPictureAssetId(), blog.getContent());
		blog.setFirstPicture(assets.coverThumbnailUrl(blog.getFirstPicture()));
		List<Tag> tags = resolveTaxonomy(blog);
		Date now = new Date();
		blog.setUser(user);
		blog.setCreateTime(now);
		blog.setUpdateTime(now);
		blog.setViews(0);
		blog.setReadTime(normalizeReadTime(blog));
		blog.setPublished(Boolean.TRUE.equals(blog.getPublished()));
		blog.setInternal(blog.getPublished() && Boolean.TRUE.equals(blog.getInternal()));
		blog.setCommentEnabled(Boolean.TRUE.equals(blog.getCommentEnabled()));
		blog.setTop(false);
		blog.setRecommend(false);
		blog.setAppreciation(false);
		blogService.saveBlog(blog);
		imageAssetService.bindBlogAssets(blog.getId(), assets);
		saveTags(blog.getId(), tags);
		return blog.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void update(String username, top.naccl.model.dto.Blog blog) {
		validateCommonFields(blog);
		normalizeOptionalFields(blog);
		if (blog.getId() == null) {
			throw new BadRequestException("文章 id 不能为空");
		}
		User user = requireUser(username);
		top.naccl.entity.Blog stored = requireOwnedBlog(user, blog.getId());
		ImageAssetService.PreparedBlogAssets assets = imageAssetService.prepareBlogAssets(
				user.getId(), blog.getId(), blog.getFirstPictureAssetId(), blog.getContent());
		blog.setFirstPicture(assets.coverThumbnailUrl(blog.getFirstPicture()));
		List<Tag> tags = resolveTaxonomy(blog);
		blog.setUser(user);
		blog.setCreateTime(stored.getCreateTime());
		blog.setUpdateTime(new Date());
		blog.setViews(stored.getViews());
		blog.setReadTime(normalizeReadTime(blog));
		blog.setPublished(Boolean.TRUE.equals(blog.getPublished()));
		blog.setInternal(blog.getPublished() && Boolean.TRUE.equals(blog.getInternal()));
		blog.setCommentEnabled(Boolean.TRUE.equals(blog.getCommentEnabled()));
		blog.setTop(stored.getTop());
		blog.setRecommend(stored.getRecommend());
		blog.setAppreciation(stored.getAppreciation());
		blogService.updateBlog(blog);
		imageAssetService.bindBlogAssets(blog.getId(), assets);
		blogService.deleteBlogTagByBlogId(blog.getId());
		saveTags(blog.getId(), tags);
	}

	@Transactional(rollbackFor = Exception.class)
	public void delete(String username, Long blogId) {
		User user = requireUser(username);
		requireOwnedBlog(user, blogId);
		blogService.deleteBlogTagByBlogId(blogId);
		commentService.deleteCommentsByBlogId(blogId);
		blogService.deleteBlogById(blogId);
	}

	private User requireUser(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	private top.naccl.entity.Blog requireOwnedBlog(User user, Long blogId) {
		top.naccl.entity.Blog blog = blogMapper.getBlogByIdAndUserId(blogId, user.getId());
		if (blog == null) {
			throw new NotFoundException("文章不存在或不属于当前用户");
		}
		return blog;
	}

	private void validateCommonFields(top.naccl.model.dto.Blog blog) {
		String lengthError = BlogContentLimits.validate(blog);
		if (lengthError != null) {
			throw new BadRequestException(lengthError);
		}
		if (StringUtils.isEmpty(blog.getTitle(), blog.getContent(), blog.getDescription())
				|| blog.getWords() == null || blog.getWords() < 0 || blog.getTagList() == null) {
			throw new BadRequestException("文章参数有误");
		}
	}

	private void normalizeOptionalFields(top.naccl.model.dto.Blog blog) {
		blog.setFirstPicture(blog.getFirstPicture() == null ? "" : blog.getFirstPicture().trim());
	}

	private int normalizeReadTime(top.naccl.model.dto.Blog blog) {
		if (blog.getReadTime() == null || blog.getReadTime() < 0) {
			return (int) Math.round(blog.getWords() / 200.0);
		}
		return blog.getReadTime();
	}

	private List<Tag> resolveTaxonomy(top.naccl.model.dto.Blog blog) {
		Object cate = blog.getCate();
		if (cate instanceof Number) {
			Category category = categoryService.getCategoryById(((Number) cate).longValue());
			blog.setCategory(category);
		} else if (cate instanceof String && !StringUtils.isEmpty(((String) cate).trim())) {
			String name = ((String) cate).trim();
			if (categoryService.getCategoryByName(name) != null) {
				throw new BadRequestException("不可添加已存在的分类");
			}
			Category category = new Category();
			category.setName(name);
			categoryService.saveCategory(category);
			blog.setCategory(category);
		} else {
			throw new BadRequestException("分类不正确");
		}

		List<Tag> tags = new ArrayList<>();
		for (Object value : blog.getTagList()) {
			if (value instanceof Number) {
				tags.add(tagService.getTagById(((Number) value).longValue()));
			} else if (value instanceof String && !StringUtils.isEmpty(((String) value).trim())) {
				String name = ((String) value).trim();
				if (tagService.getTagByName(name) != null) {
					throw new BadRequestException("不可添加已存在的标签");
				}
				Tag tag = new Tag();
				tag.setName(name);
				tagService.saveTag(tag);
				tags.add(tag);
			} else {
				throw new BadRequestException("标签不正确");
			}
		}
		return tags;
	}

	private void saveTags(Long blogId, List<Tag> tags) {
		for (Tag tag : tags) {
			blogService.saveBlogTag(blogId, tag.getId());
		}
	}
}
