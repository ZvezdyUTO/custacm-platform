package top.naccl.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.controller.support.PageRequestValidator;
import top.naccl.entity.Category;
import top.naccl.exception.BadRequestException;
import top.naccl.model.vo.Result;
import top.naccl.service.CategoryService;
import top.naccl.util.StringUtils;

/**
 * @Description: 博客分类后台管理
 * @Author: Naccl
 * @Date: 2020-08-02
 */
@RestController
@RequestMapping("/admin")
public class CategoryAdminController {
	private final CategoryService categoryService;

	public CategoryAdminController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	/**
	 * 获取博客分类列表
	 *
	 * @param pageNum  页码
	 * @param pageSize 每页个数
	 * @return
	 */
	@GetMapping("/categories")
	public Result categories(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
		PageRequestValidator.validate(pageNum, pageSize);
		String orderBy = "id desc";
		PageHelper.startPage(pageNum, pageSize, orderBy);
		PageInfo<Category> pageInfo = new PageInfo<>(categoryService.getCategoryList());
		return Result.ok("请求成功", pageInfo);
	}

	/**
	 * 添加新分类
	 *
	 * @param category 分类实体
	 * @return
	 */
	@PostMapping("/category")
	public Result saveCategory(@RequestBody Category category) {
		validateCategory(category);
		categoryService.saveCategory(category);
		return Result.ok("分类添加成功");
	}

	/**
	 * 修改分类名称
	 *
	 * @param category 分类实体
	 * @return
	 */
	@PutMapping("/category")
	public Result updateCategory(@RequestBody Category category) {
		validateCategory(category);
		if (category.getId() == null || category.getId() <= 0) {
			throw new BadRequestException("分类 ID 必须为正整数");
		}
		categoryService.updateCategory(category);
		return Result.ok("分类更新成功");
	}

	private static void validateCategory(Category category) {
		if (category == null || StringUtils.isEmpty(category.getName())) {
			throw new BadRequestException("分类名称不能为空");
		}
	}

	/**
	 * 按id删除分类
	 *
	 * @param id 分类id
	 * @return
	 */
	@DeleteMapping("/category")
	public Result delete(@RequestParam Long id) {
		categoryService.deleteCategoryById(id);
		return Result.ok("删除成功");
	}
}
