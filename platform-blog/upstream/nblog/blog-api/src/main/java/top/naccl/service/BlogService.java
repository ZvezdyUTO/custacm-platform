package top.naccl.service;

import top.naccl.entity.Blog;
import top.naccl.model.vo.BlogDetail;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.PageResult;
import top.naccl.model.vo.SearchBlog;

import java.util.List;
import java.util.Map;

public interface BlogService {
	List<Blog> getListByTitleAndCategoryId(String title, Integer categoryId);

	List<SearchBlog> getSearchBlogListByQueryAndIsPublished(String query, boolean includeInternal);

	PageResult<BlogInfo> getBlogInfoListByIsPublished(Integer pageNum, boolean includeInternal);

	PageResult<BlogInfo> getBlogInfoListByCategoryNameAndIsPublished(String categoryName, Integer pageNum,
			boolean includeInternal);

	PageResult<BlogInfo> getBlogInfoListByTagNameAndIsPublished(String tagName, Integer pageNum,
			boolean includeInternal);

	void deleteBlogTagByBlogId(Long blogId);

	void saveBlog(top.naccl.model.dto.Blog blog);

	void saveBlogTag(Long blogId, Long tagId);

	BlogDetail getBlogByIdAndIsPublished(Long id);

	BlogDetail getInternalBlogById(Long id);

	void updateBlog(top.naccl.model.dto.Blog blog);

	Boolean getCommentEnabledByBlogId(Long blogId);

	Boolean getPublishedByBlogId(Long blogId);

	Boolean getInternalByBlogId(Long blogId);
}
