package top.naccl.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.annotation.VisitLogger;
import top.naccl.enums.VisitBehavior;
import top.naccl.model.vo.BlogDetail;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.PageResult;
import top.naccl.model.vo.Result;
import top.naccl.model.vo.SearchBlog;
import top.naccl.service.BlogService;
import top.naccl.util.StringUtils;

import java.util.List;

/**
 * @Description: 博客相关
 * @Author: Naccl
 * @Date: 2020-08-12
 */
@RestController
public class BlogController {
	@Autowired
	BlogService blogService;

	/**
	 * 按置顶、创建时间排序 分页查询博客简要信息列表
	 *
	 * @param pageNum 页码
	 * @return
	 */
	@VisitLogger(VisitBehavior.INDEX)
	@GetMapping("/blogs")
	public Result blogs(@RequestParam(defaultValue = "1") Integer pageNum, Authentication authentication) {
		PageResult<BlogInfo> pageResult = blogService.getBlogInfoListByIsPublished(pageNum, isAuthenticated(authentication));
		return Result.ok("请求成功", pageResult);
	}

	/**
	 * 按id获取公开博客详情
	 *
	 * @param id 博客id
	 * @return
	 */
	@VisitLogger(VisitBehavior.BLOG)
	@GetMapping("/blog")
	public Result getBlog(@RequestParam Long id) {
		BlogDetail blog = blogService.getBlogByIdAndIsPublished(id);
		blogService.updateViewsToRedis(id);
		return Result.ok("获取成功", blog);
	}

	/**
	 * 按关键字根据文章标题搜索公开博客文章
	 *
	 * @param query 关键字字符串
	 * @return
	 */
	@VisitLogger(VisitBehavior.SEARCH)
	@GetMapping("/searchBlog")
	public Result searchBlog(@RequestParam String query, Authentication authentication) {
		//校验关键字字符串合法性
		if (StringUtils.isEmpty(query) || StringUtils.hasSpecialChar(query) || query.trim().length() > 20) {
			return Result.error("参数错误");
		}
		List<SearchBlog> searchBlogs = blogService.getSearchBlogListByQueryAndIsPublished(query.trim(), isAuthenticated(authentication));
		return Result.ok("获取成功", searchBlogs);
	}

	private static boolean isAuthenticated(Authentication authentication) {
		return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
	}
}
