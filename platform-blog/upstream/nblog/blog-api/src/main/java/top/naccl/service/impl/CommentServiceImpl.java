package top.naccl.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Comment;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.CommentMapper;
import top.naccl.model.vo.PageComment;
import top.naccl.service.CommentService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description: 博客评论业务层实现
 * @Author: Naccl
 * @Date: 2020-08-03
 */
@Service
public class CommentServiceImpl implements CommentService {
	@Autowired
	CommentMapper commentMapper;

	@Override
	public List<PageComment> getPageCommentList(Integer page, Long blogId, Long parentCommentId) {
		List<PageComment> comments = commentMapper.getPageCommentListByPageAndParentCommentId(page, blogId, parentCommentId);
		if (comments.isEmpty()) {
			return comments;
		}
		Map<String, List<PageComment>> repliesByParentId = new HashMap<>();
		for (PageComment reply : commentMapper.getPublishedReplyList(page, blogId)) {
			repliesByParentId.computeIfAbsent(reply.getParentCommentId(), ignored -> new ArrayList<>()).add(reply);
		}
		for (PageComment c : comments) {
			List<PageComment> tmpComments = new ArrayList<>();
			collectReplyComments(tmpComments, repliesByParentId, String.valueOf(c.getId()), new HashSet<>());
			//对于两列评论来说，按时间顺序排列应该比树形更合理些
			//排序一下
			Comparator<PageComment> comparator = Comparator.comparing(PageComment::getCreateTime)
					.thenComparing(PageComment::getId);
			tmpComments.sort(comparator);

			c.setReplyComments(tmpComments);
		}
		return comments;
	}

	@Override
	public Comment getCommentById(Long id) {
		Comment comment = commentMapper.getCommentById(id);
		if (comment == null) {
			throw new PersistenceException("评论不存在");
		}
		return comment;
	}

	private void collectReplyComments(List<PageComment> replies,
	                                  Map<String, List<PageComment>> repliesByParentId,
	                                  String parentCommentId,
	                                  Set<Long> visitedIds) {
		Deque<String> pendingParentIds = new ArrayDeque<>();
		pendingParentIds.add(parentCommentId);
		while (!pendingParentIds.isEmpty()) {
			for (PageComment reply : repliesByParentId.getOrDefault(pendingParentIds.removeFirst(), List.of())) {
				if (!visitedIds.add(reply.getId())) {
					continue;
				}
				replies.add(reply);
				pendingParentIds.addLast(String.valueOf(reply.getId()));
			}
		}
	}

	@Override
	public int countByPageAndIsPublished(Integer page, Long blogId, Boolean isPublished) {
		return commentMapper.countByPageAndIsPublished(page, blogId, isPublished);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveComment(top.naccl.model.dto.Comment comment) {
		if (commentMapper.saveComment(comment) != 1) {
			throw new PersistenceException("评论失败");
		}
	}

}
