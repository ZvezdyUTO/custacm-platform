package top.naccl.service;

import top.naccl.entity.Comment;
import top.naccl.model.vo.PageComment;

import java.util.List;

public interface CommentService {
	List<PageComment> getPageCommentList(Integer page, Long blogId, Long parentCommentId);

	Comment getCommentById(Long id);

	int countByPageAndIsPublished(Integer page, Long blogId, Boolean isPublished);

	void saveComment(top.naccl.model.dto.Comment comment);
}
