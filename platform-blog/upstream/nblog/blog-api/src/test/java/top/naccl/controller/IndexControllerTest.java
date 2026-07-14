package top.naccl.controller;

import org.junit.jupiter.api.Test;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;
import top.naccl.service.SiteSettingService;
import top.naccl.service.TagService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class IndexControllerTest {
	@Test
	void siteResponseOmitsUnusedLegacyPayloads() {
		IndexController controller = new IndexController();
		controller.siteSettingService = mock(SiteSettingService.class);
		controller.blogService = mock(BlogService.class);
		controller.categoryService = mock(CategoryService.class);
		controller.tagService = mock(TagService.class);
		when(controller.siteSettingService.getSiteInfo()).thenReturn(new HashMap<>(Map.of(
				"siteInfo", Map.of(), "introduction", Map.of()
		)));
		when(controller.categoryService.getCategoryNameList()).thenReturn(java.util.List.of());
		when(controller.tagService.getTagListNotId()).thenReturn(java.util.List.of());
		when(controller.blogService.getFeaturedBlogList(false))
				.thenReturn(java.util.List.of());

		Result result = controller.site(null);
		Map<?, ?> data = (Map<?, ?>) result.getData();

		assertEquals(200, result.getCode());
		assertEquals(5, data.size());
		assertFalse(data.containsKey("badges"));
		assertFalse(data.containsKey("newBlogList"));
	}
}
