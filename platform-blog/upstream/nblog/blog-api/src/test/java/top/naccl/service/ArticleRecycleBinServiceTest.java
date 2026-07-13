package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.User;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.UserMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class ArticleRecycleBinServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-13T04:00:00Z");

	@Mock private BlogMapper blogMapper;
	@Mock private CommentMapper commentMapper;
	@Mock private UserMapper userMapper;
	@Mock private ImageAssetService imageAssetService;
	@Mock private RedisService redisService;

	private ArticleRecycleBinService service;

	@BeforeEach
	void setUp() {
		service = new ArticleRecycleBinService(blogMapper, commentMapper, userMapper,
				imageAssetService, redisService, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void movingAnArticleOnlyMarksItAndPreservesAllRelatedContent() {
		when(blogMapper.moveBlogToRecycleBin(anyLong(), any())).thenReturn(1);

		service.moveToRecycleBin(42L);

		ArgumentCaptor<Date> deletedAt = ArgumentCaptor.forClass(Date.class);
		verify(blogMapper).moveBlogToRecycleBin(org.mockito.ArgumentMatchers.eq(42L), deletedAt.capture());
		assertEquals(Date.from(NOW), deletedAt.getValue());
		verify(commentMapper, never()).deleteCommentsByBlogId(anyLong());
		verify(blogMapper, never()).deleteBlogTagByBlogId(anyLong());
		verify(blogMapper, never()).deleteBlogById(anyLong());
		verify(imageAssetService, never()).prepareBlogDeletion(anyLong());
		verify(redisService).deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Test
	void ownerMoveAndRestoreAreBoundToTheAuthenticatedUserAndSevenDayCutoff() {
		User owner = new User();
		owner.setId(7L);
		when(userMapper.findByUsername("player1")).thenReturn(owner);
		when(blogMapper.moveOwnedBlogToRecycleBin(anyLong(), anyLong(), any())).thenReturn(1);
		when(blogMapper.restoreOwnedBlogFromRecycleBin(anyLong(), anyLong(), any())).thenReturn(1);

		service.moveOwnedToRecycleBin("player1", 42L);
		service.restoreOwned("player1", 42L);

		ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
		verify(blogMapper).moveOwnedBlogToRecycleBin(
				org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(7L), any());
		verify(blogMapper).restoreOwnedBlogFromRecycleBin(
				org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(7L), cutoff.capture());
		assertEquals(Date.from(NOW.minus(ArticleRecycleBinService.RETENTION)), cutoff.getValue());
	}

	@Test
	void restoreRejectsMissingOrExpiredArticle() {
		when(blogMapper.restoreBlogFromRecycleBin(anyLong(), any())).thenReturn(0);

		assertThrows(NotFoundException.class, () -> service.restore(42L));

		verify(redisService, never()).deleteCacheByKey(any());
	}

	@Test
	void physicalCleanupRunsOnlyForRowsSelectedAsExpiredAndKeepsDeletionTransactional() {
		when(blogMapper.findExpiredRecycleBinBlogIds(any())).thenReturn(List.of(42L));
		when(blogMapper.deleteBlogById(42L)).thenReturn(1);

		assertEquals(1, service.purgeExpired());

		ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
		verify(blogMapper).findExpiredRecycleBinBlogIds(cutoff.capture());
		assertEquals(Date.from(NOW.minus(ArticleRecycleBinService.RETENTION)), cutoff.getValue());
		InOrder order = inOrder(imageAssetService, commentMapper, blogMapper);
		order.verify(imageAssetService).prepareBlogDeletion(42L);
		order.verify(commentMapper).deleteCommentsByBlogId(42L);
		order.verify(blogMapper).deleteBlogTagByBlogId(42L);
		order.verify(blogMapper).deleteBlogById(42L);
	}
}
