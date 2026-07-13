package top.naccl.controller.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.AdminUserCreateRequest;
import top.naccl.model.dto.AdminUserUpdateRequest;
import top.naccl.model.vo.Result;
import top.naccl.service.impl.AdminUserService;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class UserAdminController {
    private final AdminUserService userService;

    public UserAdminController(AdminUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users:batch-create")
    public Result batchCreate(@RequestBody List<AdminUserCreateRequest> requests) {
        return Result.ok("批量创建成功", userService.batchCreate(requests));
    }

    @GetMapping("/users")
    public Result list() {
        return Result.ok("获取成功", userService.list());
    }

    @PutMapping("/users/{username}")
    public Result update(@PathVariable String username, @RequestBody AdminUserUpdateRequest request) {
        return Result.ok("修改成功", userService.update(username, request));
    }

    @DeleteMapping("/users/{username}")
    public Result delete(@PathVariable String username) {
        userService.delete(username);
        return Result.ok("删除成功");
    }
}
