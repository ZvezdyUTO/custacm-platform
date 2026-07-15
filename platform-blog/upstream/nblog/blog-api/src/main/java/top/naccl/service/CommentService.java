package top.naccl.service;

import top.naccl.entity.Comment;
import top.naccl.model.vo.PageCommentPage;

public interface CommentService {
	PageCommentPage getPageComments(Integer page, Long blogId, Long parentCommentId,
	                                int pageNum, int pageSize);

	Comment getCommentById(Long id);

	int countByPageAndIsPublished(Integer page, Long blogId, Boolean isPublished);

	void saveComment(top.naccl.model.dto.Comment comment);
}
