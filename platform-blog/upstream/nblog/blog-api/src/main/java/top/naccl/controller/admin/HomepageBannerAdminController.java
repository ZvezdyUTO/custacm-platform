package top.naccl.controller.admin;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.model.vo.Result;
import top.naccl.service.HomepageBannerService;

import java.util.List;

/**
 * 管理员首页横幅图片管理接口。
 *
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/admin/homepage-banners")
public class HomepageBannerAdminController {
    private final HomepageBannerService service;

    public HomepageBannerAdminController(HomepageBannerService service) {
        this.service = service;
    }

    @GetMapping
    public Result list() {
        return Result.ok("请求成功", service.list());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result upload(@RequestPart("file") MultipartFile file) {
        return Result.ok("上传成功", service.upload(file));
    }

    @PutMapping("/order")
    public Result reorder(@RequestBody OrderRequest request) {
        return Result.ok("排序成功", service.reorder(request.ids()));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable long id) {
        return Result.ok("删除成功", service.delete(id));
    }

    public record OrderRequest(List<Long> ids) {
    }
}
