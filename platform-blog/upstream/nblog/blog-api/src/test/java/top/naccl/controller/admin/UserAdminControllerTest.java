package top.naccl.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.service.impl.AdminUserService;
import top.naccl.model.dto.AdminUserUpdateRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

// Author: huangbingrui.awa
@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {
    @Mock
    private AdminUserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserAdminController(userService)).build();
    }

    @Test
    void batchCreateUsesDocumentedColonRouteWithoutExtraSlash() throws Exception {
        when(userService.batchCreate(anyList())).thenReturn(List.of());

        mockMvc.perform(post("/admin/users:batch-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());

        verify(userService).batchCreate(List.of());
    }

    @Test
    void updateCombinesAccountAndHandleChangesIntoOneRoute() throws Exception {
        mockMvc.perform(put("/admin/users/player")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newUsername\":\"player\",\"nickname\":\"Player\",\"email\":\"\","
                                + "\"role\":\"ROLE_player\",\"handles\":{\"CODEFORCES\":\"Benq\"},"
                                + "\"needCollect\":true}"))
                .andExpect(status().isOk());

        verify(userService).update(
                eq("player"),
                eq(new AdminUserUpdateRequest(
                        "player", "Player", "", "ROLE_player", null,
                        java.util.Map.of("CODEFORCES", "Benq"), true))
        );
    }
}
