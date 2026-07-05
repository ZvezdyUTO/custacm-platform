package com.custacm.platform.trainingdata.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrainingDataModuleController.class)
@Import(TrainingDataSecurityConfig.class)
class TrainingDataSecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpointsIgnoreBearerToken() throws Exception {
        mockMvc.perform(get("/module-info")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk());
    }

    @Test
    void odsIngestRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/ods/codeforces/submissions:batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void odsIngestRejectsPlayerRole() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/ods/codeforces/submissions:batch-upsert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void odsIngestAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/ods/codeforces/submissions:batch-upsert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isNotFound());
    }
}
