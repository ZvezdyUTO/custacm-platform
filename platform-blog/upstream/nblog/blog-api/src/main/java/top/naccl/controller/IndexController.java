package top.naccl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.model.vo.Result;
import top.naccl.service.CategoryService;
import top.naccl.service.HomepageFeaturedGroupService;
import top.naccl.service.SiteSettingService;
import top.naccl.service.TagService;

import java.util.List;
import java.util.Map;

/**
 * @Description: 站点相关
 * @Author: Naccl
 * @Date: 2020-08-09
 */

@RestController
public class IndexController {
	@Autowired
	SiteSettingService siteSettingService;
	@Autowired
	HomepageFeaturedGroupService homepageFeaturedGroupService;
	@Autowired
	CategoryService categoryService;
	@Autowired
	TagService tagService;

	/**
	 * 获取页面仍在使用的站点配置、分类列表、标签云和精选文章组
	 *
	 * @return
	 */
	@GetMapping("/site")
	public Result site() {
		Map<String, Object> map = siteSettingService.getSiteInfo();
		List<Category> categoryList = categoryService.getCategoryNameList();
		List<Tag> tagList = tagService.getTagListNotId();
		map.put("categoryList", categoryList);
		map.put("tagList", tagList);
		map.put("featuredGroups", homepageFeaturedGroupService.listPublic());
		return Result.ok("请求成功", map);
	}
}
