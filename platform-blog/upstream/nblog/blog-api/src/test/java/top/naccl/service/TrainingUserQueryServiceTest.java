package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.vo.TrainingUserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingUserQueryServiceTest {
    @Mock UserMapper userMapper;
    @Mock OjHandleAccountService handleAccountService;

    @Test
    void returnsOnlyCollectableBoundUsersWithoutHandlesOrPrivateFields() {
        User active = user("player-a", "队员 A");
        User retired = user("player-b", "队员 B");
        when(userMapper.findAll()).thenReturn(List.of(retired, active));
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        when(handleAccountService.listAll()).thenReturn(List.of(
                new OjHandleAccount("player-a", Map.of("CODEFORCES", "secret-cf", "ATCODER", "secret-at"), true, now, now),
                new OjHandleAccount("player-b", Map.of("CODEFORCES", "retired-handle"), false, now, now)
        ));

        var result = new TrainingUserQueryService(userMapper, handleAccountService).listCollectableUsers();

        assertEquals(List.of(new TrainingUserSummary(
                "player-a", "队员 A", List.of("CODEFORCES", "ATCODER"))), result);
    }

    private static User user(String username, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }
}
