package top.naccl.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.model.vo.HomepageFeaturedImage;
import top.naccl.service.HomepageFeaturedImageService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageFeaturedImageAdminControllerTest {
    @Mock
    private HomepageFeaturedImageService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new HomepageFeaturedImageAdminController(service)).build();
    }

    @Test
    void exposesListUploadOrderAndDeleteContracts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "featured.jpg", "image/jpeg", new byte[]{1});
        HomepageFeaturedImage image = new HomepageFeaturedImage(
                3L,
                "/api/image/homepage-featured.jpg",
                "/api/image/homepage-featured-thumbnail.jpg",
                0);
        when(service.list()).thenReturn(List.of(image));
        when(service.upload(file)).thenReturn(image);
        when(service.reorder(List.of(3L))).thenReturn(List.of(image));
        when(service.delete(3L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/homepage-featured-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3));
        mockMvc.perform(multipart("/admin/homepage-featured-images").file(file))
                .andExpect(status().isOk());
        mockMvc.perform(put("/admin/homepage-featured-images/order")
                        .contentType("application/json")
                        .content("{\"ids\":[3]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/admin/homepage-featured-images/3"))
                .andExpect(status().isOk());

        verify(service).list();
        verify(service).upload(file);
        verify(service).reorder(List.of(3L));
        verify(service).delete(3L);
    }
}
