package top.naccl.controller;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.controller.admin.CategoryAdminController;
import top.naccl.controller.admin.TagAdminController;
import top.naccl.controller.player.PlayerAccountController;
import top.naccl.exception.ConflictException;
import top.naccl.exception.NotFoundException;
import top.naccl.handler.ControllerExceptionHandler;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;
import top.naccl.service.PlayerAvatarService;
import top.naccl.service.PlayerProfileService;
import top.naccl.service.TagService;
import top.naccl.service.UserService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
class ControllerHttpStatusContractTest {
	private final BlogService blogService = mock(BlogService.class);
	private final CategoryService categoryService = mock(CategoryService.class);
	private final TagService tagService = mock(TagService.class);
	private final UserService userService = mock(UserService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		BlogController blogController = new BlogController();
		blogController.blogService = blogService;

		CategoryAdminController categoryController = new CategoryAdminController(categoryService);

		TagAdminController tagController = new TagAdminController(tagService);

		PlayerAccountController accountController = new PlayerAccountController();
		ReflectionTestUtils.setField(accountController, "userService", userService);
		ReflectionTestUtils.setField(accountController, "playerAvatarService", mock(PlayerAvatarService.class));
		ReflectionTestUtils.setField(accountController, "playerProfileService", mock(PlayerProfileService.class));
		ReflectionTestUtils.setField(accountController, "ojHandleAccountService", mock(OjHandleAccountService.class));

		mockMvc = standaloneSetup(blogController, categoryController, tagController, accountController)
				.setControllerAdvice(new ControllerExceptionHandler())
				.build();
	}

	@Test
	void invalidSearchUsesHttpBadRequestInsteadOfSuccessEnvelope() throws Exception {
		mockMvc.perform(get("/searchBlog").param("query", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
	}

	@Test
	void duplicateCategoryUsesHttpConflict() throws Exception {
		doThrow(new ConflictException("该分类已存在"))
				.when(categoryService).saveCategory(org.mockito.ArgumentMatchers.any());

		mockMvc.perform(post("/admin/category")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"算法\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
	}

	@Test
	void categoryPageSizeIsBoundedBeforeServiceAccess() throws Exception {
		mockMvc.perform(get("/admin/categories")
						.param("pageNum", "1")
						.param("pageSize", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
		verifyNoInteractions(categoryService);
	}

	@Test
	void categoryUpdateWithoutIdUsesHttpBadRequest() throws Exception {
		mockMvc.perform(put("/admin/category")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"算法\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
	}

	@Test
	void missingCategoryUpdateUsesHttpNotFound() throws Exception {
		doThrow(new NotFoundException("分类不存在"))
				.when(categoryService).updateCategory(org.mockito.ArgumentMatchers.any());

		mockMvc.perform(put("/admin/category")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":99,\"name\":\"算法\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void duplicateTagUsesHttpConflict() throws Exception {
		doThrow(new ConflictException("该标签已存在"))
				.when(tagService).saveTag(org.mockito.ArgumentMatchers.any());

		mockMvc.perform(post("/admin/tag")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Java\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
	}

	@Test
	void tagPageNumberIsValidatedBeforeServiceAccess() throws Exception {
		mockMvc.perform(get("/admin/tags")
						.param("pageNum", "0")
						.param("pageSize", "10"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
		verifyNoInteractions(tagService);
	}

	@Test
	void referencedCategoryUsesHttpConflict() throws Exception {
		doThrow(new ConflictException("已有博客与此分类关联，不可删除"))
				.when(categoryService).deleteCategoryById(3L);

		mockMvc.perform(delete("/admin/category").param("id", "3"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
	}

	@Test
	void referencedTagUsesHttpConflict() throws Exception {
		doThrow(new ConflictException("已有博客与此标签关联，不可删除"))
				.when(tagService).deleteTagById(3L);

		mockMvc.perform(delete("/admin/tag").param("id", "3"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
	}

	@Test
	void missingTagDeleteUsesHttpNotFound() throws Exception {
		doThrow(new NotFoundException("标签不存在"))
				.when(tagService).deleteTagById(99L);

		mockMvc.perform(delete("/admin/tag").param("id", "99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void invalidNewPasswordUsesHttpBadRequest() throws Exception {
		mockMvc.perform(patch("/player/me/password")
						.principal(new TestingAuthenticationToken("player1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"oldPassword\":\"old\",\"newPassword\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
	}

	@Test
	void wrongOldPasswordUsesHttpBadRequest() throws Exception {
		when(userService.changePassword("player1", "wrong", "new-password"))
				.thenThrow(new BadCredentialsException("旧密码错误"));

		mockMvc.perform(patch("/player/me/password")
						.principal(new TestingAuthenticationToken("player1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"oldPassword\":\"wrong\",\"newPassword\":\"new-password\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
	}

	@Test
	void unsuccessfulPasswordWriteUsesHttpInternalServerError() throws Exception {
		when(userService.changePassword("player1", "old-password", "new-password")).thenReturn(false);

		mockMvc.perform(patch("/player/me/password")
						.principal(new TestingAuthenticationToken("player1", null))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"oldPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.errorCode").value("PERSISTENCE_ERROR"));
	}
}
