package top.naccl.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.model.vo.NewBlog;
import top.naccl.model.vo.RandomBlog;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;
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
	BlogService blogService;
	@Autowired
	CategoryService categoryService;
	@Autowired
	TagService tagService;

	/**
	 * 获取站点配置信息、最新推荐博客、分类列表、标签云、精选文章
	 *
	 * @return
	 */
	@GetMapping("/site")
	public Result site(Authentication authentication) {
		Map<String, Object> map = siteSettingService.getSiteInfo();
		boolean includeInternal = authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
		List<NewBlog> newBlogList = blogService.getNewBlogListByIsPublished(includeInternal);
		List<Category> categoryList = categoryService.getCategoryNameList();
		List<Tag> tagList = tagService.getTagListNotId();
		List<RandomBlog> featuredBlogList = blogService.getRandomBlogListByLimitNumAndIsPublishedAndIsRecommend(includeInternal);
		map.put("newBlogList", newBlogList);
		map.put("categoryList", categoryList);
		map.put("tagList", tagList);
		map.put("featuredBlogList", featuredBlogList);
		return Result.ok("请求成功", map);
	}
}
