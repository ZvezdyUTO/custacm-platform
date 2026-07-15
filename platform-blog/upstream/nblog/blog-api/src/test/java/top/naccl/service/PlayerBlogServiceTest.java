package top.naccl.service;

import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.UserMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerBlogServiceTest {
	@Mock private BlogMapper blogMapper;
	@Mock private UserMapper userMapper;
	@Mock private CategoryService categoryService;
	@Mock private TagService tagService;
	@Mock private BlogService blogService;
	@Mock private CommentService commentService;
	@Mock private ImageAssetService imageAssetService;
	@Mock private ArticleRecycleBinService recycleBinService;
	@InjectMocks private PlayerBlogService playerBlogService;

	private User player;
	private Category category;

	@BeforeEach
	void setUp() {
		player = new User();
		player.setId(7L);
		player.setUsername("player1");
		category = new Category();
		category.setId(3L);
		org.mockito.Mockito.lenient().when(imageAssetService.prepareBlogAssets(
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.nullable(Long.class),
				org.mockito.ArgumentMatchers.nullable(Long.class), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(new ImageAssetService.PreparedBlogAssets(null, java.util.List.of(), java.util.List.of()));
	}

	@Test
	void createRejectsTextFieldsOverTheirLimits() {
		top.naccl.model.dto.Blog input = validBlog();
		input.setTitle("题".repeat(101));

		BadRequestException error = assertThrows(BadRequestException.class,
				() -> playerBlogService.create("player1", input));

		assertEquals("文章标题不能超过 100 字", error.getMessage());
		verify(blogService, never()).saveBlog(any());
	}

	@Test
	void createBindsCurrentUserAndForcesAdminFieldsOff() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		top.naccl.model.dto.Blog input = validBlog();
		input.setTop(true);
		input.setRecommend(true);
		input.setInternal(true);

		playerBlogService.create("player1", input);

		assertEquals(player, input.getUser());
		assertFalse(input.getTop());
		assertFalse(input.getRecommend());
		assertFalse(input.getAppreciation());
		assertEquals(true, input.getInternal());
		assertEquals(true, input.getCommentEnabled());
		verify(blogService).saveBlog(input);
	}

	@Test
	void createAllowsMissingFirstPictureAndNormalizesItToEmptyString() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		top.naccl.model.dto.Blog input = validBlog();
		input.setFirstPicture(null);

		playerBlogService.create("player1", input);

		assertEquals("", input.getFirstPicture());
		verify(blogService).saveBlog(input);
	}

	@Test
	void createDeduplicatesRepeatedTagIdsWhilePreservingFirstSeenOrder() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		Tag first = new Tag();
		first.setId(7L);
		Tag second = new Tag();
		second.setId(8L);
		when(tagService.getTagById(7L)).thenReturn(first);
		when(tagService.getTagById(8L)).thenReturn(second);
		doAnswer(invocation -> {
			((top.naccl.model.dto.Blog) invocation.getArgument(0)).setId(12L);
			return null;
		}).when(blogService).saveBlog(any());
		top.naccl.model.dto.Blog input = validBlog();
		input.setTagList(List.of(7, 7L, 8));

		playerBlogService.create("player1", input);

		verify(tagService, times(1)).getTagById(7L);
		verify(tagService, times(1)).getTagById(8L);
		InOrder tagWriteOrder = inOrder(blogService);
		tagWriteOrder.verify(blogService).saveBlogTag(12L, 7L);
		tagWriteOrder.verify(blogService).saveBlogTag(12L, 8L);
	}

	@Test
	void updatePreservesAdminControlledFields() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		top.naccl.entity.Blog stored = new top.naccl.entity.Blog();
		stored.setId(10L);
		stored.setUser(player);
		stored.setTop(true);
		stored.setRecommend(true);
		stored.setAppreciation(true);
		when(blogMapper.getBlogByIdAndUserId(10L, 7L)).thenReturn(stored);
		top.naccl.model.dto.Blog input = validBlog();
		input.setId(10L);

		playerBlogService.update("player1", input);

		assertEquals(true, input.getTop());
		assertEquals(true, input.getRecommend());
		assertEquals(true, input.getAppreciation());
		verify(blogService).updateBlog(input);
	}

	@Test
	void updateRejectsAnotherUsersBlog() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(blogMapper.getBlogByIdAndUserId(10L, 7L)).thenReturn(null);
		top.naccl.model.dto.Blog input = validBlog();
		input.setId(10L);

		assertThrows(NotFoundException.class, () -> playerBlogService.update("player1", input));
		verify(blogService, never()).updateBlog(any());
	}

	@Test
	void listResolvesCurrentUserBeforeRunningPagedBlogQuery() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(blogMapper.getListByTitleAndCategoryIdAndUserId("", null, 7L))
				.thenReturn(Collections.emptyList());

		try {
			assertEquals(0, playerBlogService.list("player1", "", null, 1, 10).getTotal());
		} finally {
			PageHelper.clearPage();
		}

		InOrder order = inOrder(userMapper, blogMapper);
		order.verify(userMapper).findByUsername("player1");
		order.verify(blogMapper).getListByTitleAndCategoryIdAndUserId("", null, 7L);
	}

	@Test
	void deleteAndRestoreStayScopedToTheAuthenticatedOwner() {
		playerBlogService.delete("player1", 10L);
		playerBlogService.restore("player1", 10L);

		verify(recycleBinService).moveOwnedToRecycleBin("player1", 10L);
		verify(recycleBinService).restoreOwned("player1", 10L);
	}

	private top.naccl.model.dto.Blog validBlog() {
		top.naccl.model.dto.Blog blog = new top.naccl.model.dto.Blog();
		blog.setTitle("题解");
		blog.setFirstPicture("/cover.png");
		blog.setContent("content");
		blog.setDescription("description");
		blog.setWords(10);
		blog.setPublished(true);
		blog.setCommentEnabled(true);
		blog.setCate(3);
		blog.setTagList(Collections.emptyList());
		return blog;
	}
}
