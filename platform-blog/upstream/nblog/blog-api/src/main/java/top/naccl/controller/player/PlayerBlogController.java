package top.naccl.controller.player;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
import top.naccl.service.ArticleArchiveService;
import top.naccl.service.ArticleDownloadService;
import top.naccl.service.BlogService;
import top.naccl.service.CategoryService;
import top.naccl.service.PlayerBlogService;
import top.naccl.service.TagService;

import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player")
public class PlayerBlogController {
	private static final int MAX_DOWNLOAD_FILENAME_CODE_POINTS = 80;

	@Autowired private PlayerBlogService playerBlogService;
	@Autowired private CategoryService categoryService;
	@Autowired private TagService tagService;
	@Autowired private BlogService blogService;
	@Autowired private ArticleDownloadService articleDownloadService;
	@Autowired private ArticleArchiveService articleArchiveService;

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

	@GetMapping("/blogs/recycle-bin")
	public Result recycleBin(Authentication authentication,
			@RequestParam(defaultValue = "") String title,
			@RequestParam(defaultValue = "") Integer categoryId,
			@RequestParam(defaultValue = "1") Integer pageNum,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		PageInfo<Blog> blogs = playerBlogService.listRecycleBin(
				authentication.getName(), title, categoryId, pageNum, pageSize);
		Map<String, Object> data = new HashMap<>(4);
		data.put("blogs", blogs);
		data.put("categories", categoryService.getCategoryList());
		return Result.ok("请求成功", data);
	}

	@GetMapping("/blog")
	public Result blog(Authentication authentication, @RequestParam Long id) {
		return Result.ok("获取成功", playerBlogService.get(authentication.getName(), id));
	}

	@GetMapping("/internal-blog")
	public Result internalBlog(@RequestParam Long id) {
		return Result.ok("获取成功", blogService.getInternalBlogById(id));
	}

	@GetMapping(value = "/blog/download", produces = "application/zip")
	public ResponseEntity<StreamingResponseBody> download(Authentication authentication, @RequestParam Long id) {
		Blog blog = articleDownloadService.download(authentication.getName(), isAdmin(authentication), id);
		String filename = safeFilename(blog.getTitle(), blog.getId());
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
				.body(output -> articleArchiveService.writeSingleArticle(blog, output));
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
		return Result.ok("添加成功", playerBlogService.create(authentication.getName(), blog));
	}

	@PutMapping("/blog")
	public Result update(Authentication authentication, @RequestBody top.naccl.model.dto.Blog blog) {
		playerBlogService.update(authentication.getName(), blog);
		return Result.ok("更新成功");
	}

	@DeleteMapping("/blog")
	public Result delete(Authentication authentication, @RequestParam Long id) {
		playerBlogService.delete(authentication.getName(), id);
		return Result.ok("已移入回收站");
	}

	@PutMapping("/blog/restore")
	public Result restore(Authentication authentication, @RequestParam Long id) {
		playerBlogService.restore(authentication.getName(), id);
		return Result.ok("恢复成功");
	}

	private static boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
	}

	private static String safeFilename(String title, Long id) {
		String safeTitle = title == null ? "" : title
				.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
				.trim();
		if (safeTitle.isEmpty()) {
			return "article-" + id + ".zip";
		}
		if (safeTitle.codePointCount(0, safeTitle.length()) > MAX_DOWNLOAD_FILENAME_CODE_POINTS) {
			int end = safeTitle.offsetByCodePoints(0, MAX_DOWNLOAD_FILENAME_CODE_POINTS);
			safeTitle = safeTitle.substring(0, end) + "-" + id;
		}
		return safeTitle + ".zip";
	}
}
