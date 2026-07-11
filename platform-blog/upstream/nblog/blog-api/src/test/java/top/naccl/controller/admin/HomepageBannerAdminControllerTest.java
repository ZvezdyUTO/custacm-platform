package top.naccl.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.model.vo.HomepageBannerImage;
import top.naccl.service.HomepageBannerService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageBannerAdminControllerTest {
    @Mock
    private HomepageBannerService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new HomepageBannerAdminController(service)).build();
    }

    @Test
    void exposesUploadOrderAndDeleteContracts() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[]{1});
        HomepageBannerImage image = new HomepageBannerImage(3L, "/api/image/banner.jpg", 0);
        when(service.upload(file)).thenReturn(image);
        when(service.reorder(List.of(3L))).thenReturn(List.of(image));
        when(service.delete(3L)).thenReturn(List.of(image));

        mockMvc.perform(multipart("/admin/homepage-banners").file(file)).andExpect(status().isOk());
        mockMvc.perform(put("/admin/homepage-banners/order")
                        .contentType("application/json")
                        .content("{\"ids\":[3]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/admin/homepage-banners/3")).andExpect(status().isOk());

        verify(service).upload(file);
        verify(service).reorder(List.of(3L));
        verify(service).delete(3L);
    }
}
