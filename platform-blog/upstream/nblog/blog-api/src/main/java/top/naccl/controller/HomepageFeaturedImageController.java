package top.naccl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.HomepageFeaturedImageService;

/**
 * 首页精选图片公开全量读取接口。
 *
 * @author huangbingrui.awa
 */
@RestController
public class HomepageFeaturedImageController {
    private final HomepageFeaturedImageService service;

    public HomepageFeaturedImageController(HomepageFeaturedImageService service) {
        this.service = service;
    }

    @GetMapping("/homepage-featured-images")
    public Result list() {
        return Result.ok("请求成功", service.list());
    }
}
