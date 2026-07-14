package top.naccl.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import top.naccl.entity.Blog;
import top.naccl.entity.Category;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.ArticleArchiveService;
import top.naccl.service.ArticleRecycleBinService;
import top.naccl.service.CategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Description: 博客文章后台管理
 * @Author: Naccl
 * @Date: 2020-07-29
 */
@RestController
@RequestMapping("/admin")
public class BlogAdminController {
	private static final DateTimeFormatter BACKUP_FILENAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final ZoneId BACKUP_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	@Autowired
	BlogService blogService;
	@Autowired
	CategoryService categoryService;
	@Autowired
	ArticleRecycleBinService recycleBinService;
	@Autowired
	ArticleArchiveService articleArchiveService;

	/**
	 * 获取博客文章列表
	 *
	 * @param title      按标题模糊查询
	 * @param categoryId 按分类id查询
	 * @param pageNum    页码
	 * @param pageSize   每页个数
	 * @return
	 */
	@GetMapping("/blogs")
	public Result blogs(@RequestParam(defaultValue = "") String title,
	                    @RequestParam(defaultValue = "") Integer categoryId,
	                    @RequestParam(defaultValue = "1") Integer pageNum,
	                    @RequestParam(defaultValue = "10") Integer pageSize) {
		String orderBy = "create_time desc";
		PageHelper.startPage(pageNum, pageSize, orderBy);
		PageInfo<Blog> pageInfo = new PageInfo<>(blogService.getListByTitleAndCategoryId(title, categoryId));
		List<Category> categories = categoryService.getCategoryList();
		Map<String, Object> map = new HashMap<>(4);
		map.put("blogs", pageInfo);
		map.put("categories", categories);
		return Result.ok("请求成功", map);
	}

	@GetMapping("/blogs/recycle-bin")
	public Result recycleBin(@RequestParam(defaultValue = "") String title,
			@RequestParam(defaultValue = "") Integer categoryId,
			@RequestParam(defaultValue = "1") Integer pageNum,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		Map<String, Object> map = new HashMap<>(4);
		map.put("blogs", recycleBinService.listForAdmin(title, categoryId, pageNum, pageSize));
		map.put("categories", categoryService.getCategoryList());
		return Result.ok("请求成功", map);
	}

	@GetMapping(value = "/blogs/backup", produces = "application/zip")
	public ResponseEntity<StreamingResponseBody> backup() {
		String filename = "custacm-article-backup-"
				+ ZonedDateTime.now(BACKUP_TIME_ZONE).format(BACKUP_FILENAME_TIME) + ".zip";
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename).build().toString())
				.body(articleArchiveService::writeAllArticlesBackup);
	}

	/**
	 * 将博客文章移入固定保留七天的回收站。
	 *
	 * @param id 文章id
	 * @return
	 */
	@DeleteMapping("/blog")
	public Result delete(@RequestParam Long id) {
		recycleBinService.moveToRecycleBin(id);
		return Result.ok("已移入回收站");
	}

	@PutMapping("/blog/restore")
	public Result restore(@RequestParam Long id) {
		recycleBinService.restore(id);
		return Result.ok("恢复成功");
	}

}
