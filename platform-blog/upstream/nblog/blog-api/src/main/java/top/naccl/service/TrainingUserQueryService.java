package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import org.springframework.stereotype.Service;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.model.vo.TrainingUserSummary;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrainingUserQueryService {
    private final UserMapper userMapper;
    private final OjHandleAccountService handleAccountService;

    public TrainingUserQueryService(UserMapper userMapper, OjHandleAccountService handleAccountService) {
        this.userMapper = userMapper;
        this.handleAccountService = handleAccountService;
    }

    public java.util.List<TrainingUserSummary> listCollectableUsers() {
        Map<String, User> users = userMapper.findAll().stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity()));
        return handleAccountService.listAll().stream()
                .filter(account -> account.needCollect() && users.containsKey(account.username()))
                .map(account -> new TrainingUserSummary(
                        account.username(),
                        users.get(account.username()).getNickname(),
                        account.handles().keySet().stream().toList()))
                .sorted(Comparator.comparing(TrainingUserSummary::username))
                .toList();
    }
}
