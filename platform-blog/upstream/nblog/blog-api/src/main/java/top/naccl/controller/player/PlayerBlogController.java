package top.naccl.controller.player;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.entity.Blog;
import top.naccl.model.vo.Result;
import top.naccl.service.CategoryService;
import top.naccl.service.PlayerBlogService;
import top.naccl.service.TagService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player")
public class PlayerBlogController {
	@Autowired private PlayerBlogService playerBlogService;
	@Autowired private CategoryService categoryService;
	@Autowired private TagService tagService;

	@GetMapping("/blogs")
	public Result blogs(Authentication authentication,
	                    @RequestParam(defaultValue = "") String title,
	                    @RequestParam(defaultValue = "") Integer categoryId,
	                    @RequestParam(defaultValue = "1") Integer pageNum,
	                    @RequestParam(defaultValue = "10") Integer pageSize) {
		PageInfo<Blog> blogs = playerBlogService.list(authentication.getName(), title, categoryId, pageNum, pageSize);
		Map<String, Object> data = new HashMap<>(4);
		data.put("blogs", blogs);
		data.put("categories", categoryService.getCategoryList());
		return Result.ok("请求成功", data);
	}

	@GetMapping("/blog")
	public Result blog(Authentication authentication, @RequestParam Long id) {
		return Result.ok("获取成功", playerBlogService.get(authentication.getName(), id));
	}

	@GetMapping("/categoryAndTag")
	public Result categoryAndTag() {
		Map<String, Object> data = new HashMap<>(4);
		data.put("categories", categoryService.getCategoryList());
		data.put("tags", tagService.getTagList());
		return Result.ok("请求成功", data);
	}

	@PostMapping("/blog")
	public Result create(Authentication authentication, @RequestBody top.naccl.model.dto.Blog blog) {
		playerBlogService.create(authentication.getName(), blog);
		return Result.ok("添加成功");
	}

	@PutMapping("/blog")
	public Result update(Authentication authentication, @RequestBody top.naccl.model.dto.Blog blog) {
		playerBlogService.update(authentication.getName(), blog);
		return Result.ok("更新成功");
	}

	@DeleteMapping("/blog")
	public Result delete(Authentication authentication, @RequestParam Long id) {
		playerBlogService.delete(authentication.getName(), id);
		return Result.ok("删除成功");
	}
}
