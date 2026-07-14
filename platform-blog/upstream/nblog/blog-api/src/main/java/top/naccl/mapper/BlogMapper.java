package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.Blog;
import top.naccl.model.vo.BlogDetail;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.RandomBlog;
import top.naccl.model.vo.SearchBlog;

import java.util.List;
import java.util.Date;

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

	List<Blog> getAllBlogsForBackup();

	List<Blog> getRecycleBinList(@Param("title") String title, @Param("categoryId") Integer categoryId,
			@Param("cutoff") Date cutoff);

	List<Blog> getRecycleBinListByUserId(@Param("title") String title,
			@Param("categoryId") Integer categoryId, @Param("userId") Long userId,
			@Param("cutoff") Date cutoff);

	List<SearchBlog> getSearchBlogListByQueryAndIsPublished(@Param("query") String query,
			@Param("includeInternal") boolean includeInternal);

	List<BlogInfo> getBlogInfoListByIsPublished(@Param("includeInternal") boolean includeInternal);

	List<BlogInfo> getBlogInfoListByCategoryNameAndIsPublished(@Param("categoryName") String categoryName,
			@Param("includeInternal") boolean includeInternal);

	List<BlogInfo> getBlogInfoListByTagNameAndIsPublished(@Param("tagName") String tagName,
			@Param("includeInternal") boolean includeInternal);

	List<RandomBlog> getFeaturedBlogList(@Param("limitNum") Integer limitNum,
			@Param("includeInternal") boolean includeInternal);

	int deleteBlogById(Long id);

	int moveBlogToRecycleBin(@Param("id") Long id, @Param("deletedAt") Date deletedAt);

	int moveOwnedBlogToRecycleBin(@Param("id") Long id, @Param("userId") Long userId,
			@Param("deletedAt") Date deletedAt);

	int restoreBlogFromRecycleBin(@Param("id") Long id, @Param("cutoff") Date cutoff);

	int restoreOwnedBlogFromRecycleBin(@Param("id") Long id, @Param("userId") Long userId,
			@Param("cutoff") Date cutoff);

	List<Long> findExpiredRecycleBinBlogIds(@Param("cutoff") Date cutoff);

	int deleteBlogTagByBlogId(Long blogId);

	int saveBlog(top.naccl.model.dto.Blog blog);

	int saveBlogTag(Long blogId, Long tagId);

	int updateBlogRecommendById(Long blogId, Boolean recommend);

	Blog getBlogByIdAndUserId(Long id, Long userId);

	BlogDetail getBlogByIdAndIsPublished(Long id);

	BlogDetail getInternalBlogById(Long id);

	Blog getPublishedBlogForDownload(Long id);

	int updateBlog(top.naccl.model.dto.Blog blog);

	int countBlogByCategoryId(Long categoryId);

	int countBlogByTagId(Long tagId);

	Boolean getCommentEnabledByBlogId(Long blogId);

	Boolean getPublishedByBlogId(Long blogId);

	Boolean getInternalByBlogId(Long blogId);

}
