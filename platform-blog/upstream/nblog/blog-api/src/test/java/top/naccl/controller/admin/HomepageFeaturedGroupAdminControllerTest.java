package top.naccl.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.model.dto.HomepageFeaturedGroupUpsertRequest;
import top.naccl.service.HomepageFeaturedGroupService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageFeaturedGroupAdminControllerTest {
	@Mock
	private HomepageFeaturedGroupService service;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new HomepageFeaturedGroupAdminController(service)).build();
	}

	@Test
	void exposesListCandidatesCrudAndWholeOrderContracts() throws Exception {
		HomepageFeaturedGroupUpsertRequest request = new HomepageFeaturedGroupUpsertRequest(
				"夏季训练", List.of(11L, 12L, 13L));
		when(service.listAdmin()).thenReturn(List.of());
		when(service.candidates("训练")).thenReturn(List.of());
		when(service.create(request)).thenReturn(List.of());
		when(service.update(7L, request)).thenReturn(List.of());
		when(service.delete(7L)).thenReturn(List.of());
		when(service.reorder(List.of(2L, 1L))).thenReturn(List.of());

		mockMvc.perform(get("/admin/homepage-featured-groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());
		mockMvc.perform(get("/admin/homepage-featured-groups/candidates").param("query", "训练"))
				.andExpect(status().isOk());
		String body = """
				{"title":"夏季训练","articleIds":[11,12,13]}
				""";
		mockMvc.perform(post("/admin/homepage-featured-groups")
						.contentType("application/json").content(body))
				.andExpect(status().isOk());
		mockMvc.perform(put("/admin/homepage-featured-groups/7")
						.contentType("application/json").content(body))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/admin/homepage-featured-groups/7"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/admin/homepage-featured-groups/order")
						.contentType("application/json").content("{\"ids\":[2,1]}"))
				.andExpect(status().isOk());

		verify(service).listAdmin();
		verify(service).candidates("训练");
		verify(service).create(request);
		verify(service).update(7L, request);
		verify(service).delete(7L);
		verify(service).reorder(List.of(2L, 1L));
	}
}
