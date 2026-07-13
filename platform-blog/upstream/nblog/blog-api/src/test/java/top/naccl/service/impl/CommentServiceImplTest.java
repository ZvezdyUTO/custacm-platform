package top.naccl.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.mapper.CommentMapper;
import top.naccl.model.vo.PageComment;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
	@Mock
	private CommentMapper commentMapper;

	@InjectMocks
	private CommentServiceImpl commentService;

	@Test
	void loadsAllReplyLevelsWithOneBatchQueryAndFlattensThemByTime() {
		PageComment root = comment(1L, "-1", 0L);
		PageComment latestChild = comment(2L, "1", 3_000L);
		PageComment nestedChild = comment(3L, "2", 2_000L);
		PageComment earliestChild = comment(4L, "1", 1_000L);
		PageComment unrelated = comment(5L, "99", 500L);
		when(commentMapper.getPageCommentListByPageAndParentCommentId(0, 42L, -1L))
				.thenReturn(List.of(root));
		when(commentMapper.getPublishedReplyList(0, 42L))
				.thenReturn(List.of(latestChild, nestedChild, earliestChild, unrelated));

		List<PageComment> result = commentService.getPageCommentList(0, 42L, -1L);

		assertEquals(List.of(4L, 3L, 2L), result.getFirst().getReplyComments().stream()
				.map(PageComment::getId)
				.toList());
		verify(commentMapper).getPageCommentListByPageAndParentCommentId(0, 42L, -1L);
		verify(commentMapper).getPublishedReplyList(0, 42L);
		verifyNoMoreInteractions(commentMapper);
	}

	private PageComment comment(Long id, String parentId, long createdAt) {
		PageComment comment = new PageComment();
		comment.setId(id);
		comment.setParentCommentId(parentId);
		comment.setCreateTime(new Date(createdAt));
		return comment;
	}
}
