package top.naccl.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.entity.Category;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

// Author: huangbingrui.awa
class CategoryControllerTest {
    @Test
    void returnsOnlyTheLightweightCategoryNavigationData() throws Exception {
        CategoryService categoryService = mock(CategoryService.class);
        Category category = new Category();
        category.setName("算法");
        when(categoryService.getCategoryNameList()).thenReturn(List.of(category));
        CategoryController controller = new CategoryController();
        controller.blogService = mock(BlogService.class);
        controller.categoryService = categoryService;
        MockMvc mockMvc = standaloneSetup(controller).build();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("算法"));

        verify(categoryService).getCategoryNameList();
    }
}
