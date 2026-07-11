package top.naccl.controller.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.AdminUserCreateRequest;
import top.naccl.model.dto.AdminUserPatchRequest;
import top.naccl.model.dto.OjHandlesUpdateRequest;
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

    @PostMapping("/users")
    public Result create(@RequestBody AdminUserCreateRequest request) {
        return Result.ok("创建成功", userService.create(request));
    }

    @PostMapping("/users:batch-create")
    public Result batchCreate(@RequestBody List<AdminUserCreateRequest> requests) {
        return Result.ok("批量创建成功", userService.batchCreate(requests));
    }

    @GetMapping("/users")
    public Result list() {
        return Result.ok("获取成功", userService.list());
    }

    @GetMapping("/users/{username}")
    public Result get(@PathVariable String username) {
        return Result.ok("获取成功", userService.get(username));
    }

    @PatchMapping("/users/{username}")
    public Result patch(@PathVariable String username, @RequestBody AdminUserPatchRequest request) {
        return Result.ok("修改成功", userService.patch(username, request));
    }

    @PutMapping("/users/{username}/oj-handles")
    public Result updateHandles(@PathVariable String username, @RequestBody OjHandlesUpdateRequest request) {
        return Result.ok("修改成功", userService.updateHandles(username, request));
    }

    @DeleteMapping("/users/{username}")
    public Result delete(@PathVariable String username) {
        userService.delete(username);
        return Result.ok("删除成功");
    }
}
