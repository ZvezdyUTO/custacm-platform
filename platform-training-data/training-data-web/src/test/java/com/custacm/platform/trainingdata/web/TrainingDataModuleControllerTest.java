package com.custacm.platform.trainingdata.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingDataModuleControllerTest {
    @Test
    void exposesHealthAndModuleInfo() {
        TrainingDataModuleController controller = new TrainingDataModuleController();

        assertThat(controller.health()).containsEntry("service", "training-data-web");
        assertThat(controller.moduleInfo())
                .containsEntry("module", "platform-training-data")
                .containsEntry("service", "training-data-web");
        assertThat(controller.moduleInfo().get("features"))
                .isEqualTo(List.of(
                        "oj-warehouse-modules",
                        "codeforces-ods-submission",
                        "codeforces-handle-account",
                        "codeforces-warehouse-refresh",
                        "codeforces-submission-collector"
                ));
    }
}
