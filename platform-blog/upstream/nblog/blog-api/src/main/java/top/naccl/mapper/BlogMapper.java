package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.Blog;
import top.naccl.model.dto.BlogVisibility;
import top.naccl.model.vo.BlogDetail;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.CategoryBlogCount;
import top.naccl.model.vo.RandomBlog;
import top.naccl.model.vo.SearchBlog;

import java.util.List;

/**
 * @Description: 博客文章持久层接口
 * @Author: Naccl
 * @Date: 2020-07-26
 */
@Mapper
@Repository
public interface BlogMapper {
	List<Blog> getListByTitleAndCategoryId(String title, Integer categoryId);

	List<Blog> getListByTitleAndCategoryIdAndUserId(String title, Integer categoryId, Long userId);

	List<SearchBlog> getSearchBlogListByQueryAndIsPublished(@Param("query") String query,
			@Param("includeInternal") boolean includeInternal);

	List<Blog> getIdAndTitleList();

	List<BlogInfo> getBlogInfoListByIsPublished(@Param("includeInternal") boolean includeInternal);

	List<BlogInfo> getBlogInfoListByCategoryNameAndIsPublished(@Param("categoryName") String categoryName,
			@Param("includeInternal") boolean includeInternal);

	List<BlogInfo> getBlogInfoListByTagNameAndIsPublished(@Param("tagName") String tagName,
			@Param("includeInternal") boolean includeInternal);

	List<RandomBlog> getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(@Param("limitNum") Integer limitNum,
			@Param("includeInternal") boolean includeInternal);

	int deleteBlogById(Long id);

	int deleteBlogTagByBlogId(Long blogId);

	int saveBlog(top.naccl.model.dto.Blog blog);

	int saveBlogTag(Long blogId, Long tagId);

	int updateBlogRecommendById(Long blogId, Boolean recommend);

	int updateBlogVisibilityById(Long blogId, BlogVisibility bv);

	int updateBlogTopById(Long blogId, Boolean top);

	Blog getBlogById(Long id);

	Blog getBlogByIdAndUserId(Long id, Long userId);

	String getTitleByBlogId(Long id);

	BlogDetail getBlogByIdAndIsPublished(Long id);

	BlogDetail getInternalBlogById(Long id);

	int updateBlog(top.naccl.model.dto.Blog blog);

	int countBlog();

	int countBlogByIsPublished();

	int countBlogByCategoryId(Long categoryId);

	int countBlogByTagId(Long tagId);

	Boolean getCommentEnabledByBlogId(Long blogId);

	Boolean getPublishedByBlogId(Long blogId);

	Boolean getInternalByBlogId(Long blogId);

	List<CategoryBlogCount> getCategoryBlogCountList();
}
