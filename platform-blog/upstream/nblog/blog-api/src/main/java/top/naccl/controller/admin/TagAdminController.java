package top.naccl.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.controller.support.PageRequestValidator;
import top.naccl.entity.Tag;
import top.naccl.exception.BadRequestException;
import top.naccl.model.vo.Result;
import top.naccl.service.TagService;
import top.naccl.util.StringUtils;

/**
 * @Description: 博客标签后台管理
 * @Author: Naccl
 * @Date: 2020-08-02
 */
@RestController
@RequestMapping("/admin")
public class TagAdminController {
	private final TagService tagService;

	public TagAdminController(TagService tagService) {
		this.tagService = tagService;
	}

	/**
	 * 获取博客标签列表
	 *
	 * @param pageNum  页码
	 * @param pageSize 每页个数
	 * @return
	 */
	@GetMapping("/tags")
	public Result tags(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
		PageRequestValidator.validate(pageNum, pageSize);
		String orderBy = "id desc";
		PageHelper.startPage(pageNum, pageSize, orderBy);
		PageInfo<Tag> pageInfo = new PageInfo<>(tagService.getTagList());
		return Result.ok("请求成功", pageInfo);
	}

	/**
	 * 添加新标签
	 *
	 * @param tag 标签实体
	 * @return
	 */
	@PostMapping("/tag")
	public Result saveTag(@RequestBody Tag tag) {
		if (tag == null || StringUtils.isEmpty(tag.getName())) {
			throw new BadRequestException("参数不能为空");
		}
		tagService.saveTag(tag);
		return Result.ok("添加成功");
	}

	/**
	 * 按id删除标签
	 *
	 * @param id 标签id
	 * @return
	 */
	@DeleteMapping("/tag")
	public Result delete(@RequestParam Long id) {
		tagService.deleteTagById(id);
		return Result.ok("删除成功");
	}
}
