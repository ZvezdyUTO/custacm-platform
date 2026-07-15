package top.naccl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.naccl.entity.Comment;
import top.naccl.model.vo.ArticleBackupComment;
import top.naccl.model.vo.PageComment;

import java.util.List;

/**
 * @Description: 博客评论持久层接口
 * @Author: Naccl
 * @Date: 2020-08-03
 */
@Mapper
@Repository
public interface CommentMapper {
	List<PageComment> getPageCommentListByPageAndParentCommentId(Integer page, Long blogId, Long parentCommentId);

	List<PageComment> getPublishedReplyListForRootComments(@Param("page") Integer page,
	                                                     @Param("blogId") Long blogId,
	                                                     @Param("rootCommentIds") List<Long> rootCommentIds,
	                                                     @Param("replyLimit") int replyLimit);

	Comment getCommentById(Long id);

	List<ArticleBackupComment> getArticleBackupComments(List<Long> blogIds);

	int deleteCommentsByBlogId(Long blogId);

	int countByPageAndIsPublished(Integer page, Long blogId, Boolean isPublished);

	int saveComment(top.naccl.model.dto.Comment comment);
}
