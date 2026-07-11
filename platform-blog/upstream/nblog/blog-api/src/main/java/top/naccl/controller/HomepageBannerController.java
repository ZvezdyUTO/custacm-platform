package top.naccl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.HomepageBannerService;

/**
 * 首页横幅图片公开读取接口。
 *
 * @author huangbingrui.awa
 */
@RestController
public class HomepageBannerController {
    private final HomepageBannerService service;

    public HomepageBannerController(HomepageBannerService service) {
        this.service = service;
    }

    @GetMapping("/homepage-banners")
    public Result list() {
        return Result.ok("请求成功", service.list());
    }
}
