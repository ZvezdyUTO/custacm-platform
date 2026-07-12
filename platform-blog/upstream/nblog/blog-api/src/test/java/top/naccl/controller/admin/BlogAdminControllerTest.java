package top.naccl.controller.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import top.naccl.entity.Category;
import top.naccl.entity.User;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;
import top.naccl.service.CommentService;
import top.naccl.service.TagService;
import top.naccl.service.UserService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class BlogAdminControllerTest {
	@Mock private BlogService blogService;
	@Mock private CategoryService categoryService;
	@Mock private TagService tagService;
	@Mock private CommentService commentService;
	@Mock private UserService userService;
	@Mock private Authentication authentication;
	@InjectMocks private BlogAdminController controller;

	@Test
	void createBindsCurrentAdministratorAsAuthor() {
		User administrator = new User();
		administrator.setId(42L);
		administrator.setUsername("admin-two");
		Category category = new Category();
		category.setId(3L);
		when(authentication.getName()).thenReturn("admin-two");
		when(userService.findUserByUsername("admin-two")).thenReturn(administrator);
		when(categoryService.getCategoryById(3L)).thenReturn(category);
		top.naccl.model.dto.Blog blog = validBlog();

		controller.saveBlog(authentication, blog);

		assertSame(administrator, blog.getUser());
		verify(userService).findUserByUsername("admin-two");
		verify(blogService).saveBlog(blog);
	}

	private top.naccl.model.dto.Blog validBlog() {
		top.naccl.model.dto.Blog blog = new top.naccl.model.dto.Blog();
		blog.setTitle("管理员文章");
		blog.setFirstPicture("/cover.png");
		blog.setContent("content");
		blog.setDescription("description");
		blog.setWords(10);
		blog.setCate(3);
		blog.setTagList(Collections.emptyList());
		return blog;
	}
}
