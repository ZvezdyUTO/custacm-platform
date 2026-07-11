package top.naccl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Result health() {
        return Result.ok("ok", Map.of("status", "UP"));
    }
}
