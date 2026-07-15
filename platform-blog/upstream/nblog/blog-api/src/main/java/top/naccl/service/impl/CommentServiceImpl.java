package top.naccl.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.Comment;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.mapper.CommentMapper;
import top.naccl.model.vo.PageComment;
import top.naccl.model.vo.PageCommentPage;
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
	static final int MAX_REPLY_COUNT_PER_PAGE = 500;
	private static final int REPLY_QUERY_LIMIT = MAX_REPLY_COUNT_PER_PAGE + 1;
	private static final Comparator<PageComment> REPLY_ORDER = Comparator
			.comparing(PageComment::getCreateTime)
			.thenComparing(PageComment::getId);

	private final CommentMapper commentMapper;

	public CommentServiceImpl(CommentMapper commentMapper) {
		this.commentMapper = commentMapper;
	}

	@Override
	public PageCommentPage getPageComments(Integer page, Long blogId, Long parentCommentId,
	                                      int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<PageComment> comments = commentMapper.getPageCommentListByPageAndParentCommentId(page, blogId, parentCommentId);
		PageInfo<PageComment> pageInfo = new PageInfo<>(comments);
		if (comments.isEmpty()) {
			return new PageCommentPage(pageInfo.getPages(), comments, false);
		}
		List<Long> rootCommentIds = comments.stream().map(PageComment::getId).toList();
		List<PageComment> replyRows = commentMapper.getPublishedReplyListForRootComments(
				page, blogId, rootCommentIds, REPLY_QUERY_LIMIT);
		boolean repliesTruncated = replyRows.size() > MAX_REPLY_COUNT_PER_PAGE;
		if (repliesTruncated) {
			replyRows = replyRows.subList(0, MAX_REPLY_COUNT_PER_PAGE);
		}
		Map<String, List<PageComment>> repliesByParentId = new HashMap<>();
		for (PageComment reply : replyRows) {
			repliesByParentId.computeIfAbsent(reply.getParentCommentId(), ignored -> new ArrayList<>()).add(reply);
		}
		for (PageComment c : comments) {
			List<PageComment> tmpComments = new ArrayList<>();
			collectReplyComments(tmpComments, repliesByParentId, String.valueOf(c.getId()), new HashSet<>());
			//对于两列评论来说，按时间顺序排列应该比树形更合理些
			//排序一下
			tmpComments.sort(REPLY_ORDER);

			c.setReplyComments(tmpComments);
		}
		return new PageCommentPage(pageInfo.getPages(), comments, repliesTruncated);
	}

	@Override
	public Comment getCommentById(Long id) {
		Comment comment = commentMapper.getCommentById(id);
		if (comment == null) {
			throw new NotFoundException("评论不存在");
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
