package top.naccl.service.impl;

import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.CommentMapper;
import top.naccl.model.vo.PageComment;
import top.naccl.model.vo.PageCommentPage;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

	@AfterEach
	void clearPageHelperState() {
		PageHelper.clearPage();
	}

	@Test
	void loadsAllReplyLevelsOnlyForTheCurrentRootPageAndFlattensEachTreeByTime() {
		PageComment root = comment(1L, "-1", 0L);
		PageComment secondRoot = comment(10L, "-1", 0L);
		PageComment latestChild = comment(2L, "1", 3_000L);
		PageComment nestedChild = comment(3L, "2", 2_000L);
		PageComment earliestChild = comment(4L, "1", 1_000L);
		PageComment secondRootChild = comment(11L, "10", 4_000L);
		PageComment unrelated = comment(5L, "99", 500L);
		when(commentMapper.getPageCommentListByPageAndParentCommentId(0, 42L, -1L))
				.thenReturn(List.of(root, secondRoot));
		when(commentMapper.getPublishedReplyListForRootComments(
				0, 42L, List.of(1L, 10L), CommentServiceImpl.MAX_REPLY_COUNT_PER_PAGE + 1))
				.thenReturn(List.of(latestChild, nestedChild, earliestChild, secondRootChild, unrelated));

		PageCommentPage result = commentService.getPageComments(0, 42L, -1L, 1, 10);

		assertEquals(List.of(4L, 3L, 2L), result.comments().getFirst().getReplyComments().stream()
					.map(PageComment::getId)
					.toList());
		assertEquals(List.of(11L), result.comments().get(1).getReplyComments().stream()
					.map(PageComment::getId)
					.toList());
		assertFalse(result.repliesTruncated());
		verify(commentMapper).getPageCommentListByPageAndParentCommentId(0, 42L, -1L);
		verify(commentMapper).getPublishedReplyListForRootComments(
				0, 42L, List.of(1L, 10L), CommentServiceImpl.MAX_REPLY_COUNT_PER_PAGE + 1);
		verifyNoMoreInteractions(commentMapper);
	}

	@Test
	void capsRepliesForOneRootPageAndReportsTruncation() {
		PageComment root = comment(1L, "-1", 0L);
		List<PageComment> replies = new ArrayList<>();
		for (int index = 0; index <= CommentServiceImpl.MAX_REPLY_COUNT_PER_PAGE; index++) {
			replies.add(comment((long) index + 2, "1", index));
		}
		when(commentMapper.getPageCommentListByPageAndParentCommentId(0, 42L, -1L))
				.thenReturn(List.of(root));
		when(commentMapper.getPublishedReplyListForRootComments(
				0, 42L, List.of(1L), CommentServiceImpl.MAX_REPLY_COUNT_PER_PAGE + 1))
				.thenReturn(replies);

		PageCommentPage result = commentService.getPageComments(0, 42L, -1L, 1, 10);

		assertTrue(result.repliesTruncated());
		assertEquals(CommentServiceImpl.MAX_REPLY_COUNT_PER_PAGE,
				result.comments().getFirst().getReplyComments().size());
	}

	@Test
	void skipsTheRecursiveQueryWhenTheRootPageIsEmpty() {
		when(commentMapper.getPageCommentListByPageAndParentCommentId(0, 42L, -1L))
				.thenReturn(List.of());

		assertEquals(List.of(), commentService.getPageComments(0, 42L, -1L, 1, 10).comments());

		verify(commentMapper).getPageCommentListByPageAndParentCommentId(0, 42L, -1L);
		verifyNoMoreInteractions(commentMapper);
	}

	@Test
	void missingReplyParentIsAResourceNotFoundFailure() {
		when(commentMapper.getCommentById(99L)).thenReturn(null);

		assertThrows(NotFoundException.class, () -> commentService.getCommentById(99L));
	}

	private PageComment comment(Long id, String parentId, long createdAt) {
		PageComment comment = new PageComment();
		comment.setId(id);
		comment.setParentCommentId(parentId);
		comment.setCreateTime(new Date(createdAt));
		return comment;
	}
}
