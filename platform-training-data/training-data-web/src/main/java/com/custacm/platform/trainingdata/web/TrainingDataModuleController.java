package com.custacm.platform.trainingdata.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TrainingDataModuleController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "training-data-web"
        );
    }

    @GetMapping("/module-info")
    public Map<String, Object> moduleInfo() {
        return Map.of(
                "module", "platform-training-data",
                "service", "training-data-web",
                "features", List.of(
                        "oj-warehouse-modules",
                        "codeforces-ods-submission",
                        "codeforces-handle-account",
                        "codeforces-warehouse-refresh",
                        "codeforces-submission-collector"
                )
        );
    }
}
