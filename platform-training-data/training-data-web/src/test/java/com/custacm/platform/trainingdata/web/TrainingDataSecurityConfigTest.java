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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    void codeforcesHandleCreateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesWarehouseRefreshRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesSubmissionCollectionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesSubmissionCollectionJobStartRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesSubmissionCollectionJobReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs/job-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesSubmissionCollectionJobListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void codeforcesStudentDataPurgeRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/training-data/admin/codeforces/users/112487张三/data"))
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
    void codeforcesHandleCreateRejectsPlayerRole() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesWarehouseRefreshRejectsPlayerRole() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesSubmissionCollectionRejectsPlayerRole() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesSubmissionCollectionJobStartRejectsPlayerRole() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesSubmissionCollectionJobReadRejectsPlayerRole() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs/job-1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesSubmissionCollectionJobListRejectsPlayerRole() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeforcesStudentDataPurgeRejectsPlayerRole() throws Exception {
        mockMvc.perform(delete("/api/training-data/admin/codeforces/users/112487张三/data")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player"))))
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

    @Test
    void codeforcesHandleCreateAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesWarehouseRefreshAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesSubmissionCollectionAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesSubmissionCollectionJobStartAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesSubmissionCollectionJobReadAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs/job-1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesSubmissionCollectionJobListAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesStudentDataPurgeAllowsAdminRolePastSecurity() throws Exception {
        mockMvc.perform(delete("/api/training-data/admin/codeforces/users/112487张三/data")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesHandleLookupIsPublicAndIgnoresBearerToken() throws Exception {
        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codeforcesWarehouseQueryEndpointsArePublicAndIgnoreBearerToken() throws Exception {
        mockMvc.perform(get("/api/training-data/codeforces/accepted-summary")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/training-data/codeforces/submissions/by-student")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/training-data/codeforces/submissions/by-problem")
                        .param("problemKey", "1000:A")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/training-data/codeforces/first-accepted/by-student")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/training-data/codeforces/first-accepted/by-problem")
                        .param("problemKey", "1000:A")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isNotFound());
    }
}
