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
import top.naccl.entity.User;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.UserMapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
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
	}

	@Test
	void createBindsCurrentUserAndForcesAdminFieldsOff() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		top.naccl.model.dto.Blog input = validBlog();
		input.setTop(true);
		input.setRecommend(true);
		input.setPassword("forged");

		playerBlogService.create("player1", input);

		assertEquals(player, input.getUser());
		assertFalse(input.getTop());
		assertFalse(input.getRecommend());
		assertFalse(input.getAppreciation());
		assertEquals("", input.getPassword());
		verify(blogService).saveBlog(input);
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
		stored.setPassword("admin-secret");
		stored.setViews(88);
		when(blogMapper.getBlogByIdAndUserId(10L, 7L)).thenReturn(stored);
		top.naccl.model.dto.Blog input = validBlog();
		input.setId(10L);

		playerBlogService.update("player1", input);

		assertEquals(true, input.getTop());
		assertEquals(true, input.getRecommend());
		assertEquals(true, input.getAppreciation());
		assertEquals("admin-secret", input.getPassword());
		assertEquals(88, input.getViews());
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
