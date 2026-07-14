package top.naccl.controller.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.HomepageFeaturedGroupOrderRequest;
import top.naccl.model.dto.HomepageFeaturedGroupUpsertRequest;
import top.naccl.model.vo.Result;
import top.naccl.service.HomepageFeaturedGroupService;

/**
 * 管理员首页精选文章组管理接口。
 *
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/admin/homepage-featured-groups")
public class HomepageFeaturedGroupAdminController {
	private final HomepageFeaturedGroupService service;

	public HomepageFeaturedGroupAdminController(HomepageFeaturedGroupService service) {
		this.service = service;
	}

	@GetMapping
	public Result list() {
		return Result.ok("请求成功", service.listAdmin());
	}

	@GetMapping("/candidates")
	public Result candidates(@RequestParam(defaultValue = "") String query) {
		return Result.ok("请求成功", service.candidates(query));
	}

	@PostMapping
	public Result create(@RequestBody HomepageFeaturedGroupUpsertRequest request) {
		return Result.ok("创建成功", service.create(request));
	}

	@PutMapping("/{id}")
	public Result update(@PathVariable long id, @RequestBody HomepageFeaturedGroupUpsertRequest request) {
		return Result.ok("更新成功", service.update(id, request));
	}

	@DeleteMapping("/{id}")
	public Result delete(@PathVariable long id) {
		return Result.ok("删除成功", service.delete(id));
	}

	@PutMapping("/order")
	public Result reorder(@RequestBody HomepageFeaturedGroupOrderRequest request) {
		return Result.ok("排序成功", service.reorder(request == null ? null : request.ids()));
	}
}
