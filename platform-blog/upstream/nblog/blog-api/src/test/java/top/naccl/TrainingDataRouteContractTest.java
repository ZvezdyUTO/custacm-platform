package top.naccl;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.naccl.controller.admin.TrainingDataAdminController;
import top.naccl.controller.player.TrainingDataQueryController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainingDataRouteContractTest {
    @Test
    void playerControllerExposesOnlyApprovedQueryRoutes() {
        assertEquals("/player/training-data",
                TrainingDataQueryController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals(Set.of(
                        "/users",
                        "/accepted-summary",
                        "/submissions/by-user",
                        "/submissions/by-problem",
                        "/first-accepted/by-user",
                        "/first-accepted/by-problem"),
                mappedPaths(TrainingDataQueryController.class, GetMapping.class));
    }

    @Test
    void adminControllerExposesApprovedOperationRoutes() {
        assertEquals("/admin/training-data",
                TrainingDataAdminController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals(Set.of(
                        "/submissions:collect",
                        "/submission-collection-jobs",
                        "/ods/codeforces/submissions:batch-upsert",
                        "/{ojName}/warehouse:refresh"),
                mappedPaths(TrainingDataAdminController.class, PostMapping.class));
        assertEquals(Set.of(
                        "/submission-collection-jobs",
                        "/submission-collection-jobs/{jobId}"),
                mappedPaths(TrainingDataAdminController.class, GetMapping.class));
    }

    private static Set<String> mappedPaths(Class<?> type, Class<? extends java.lang.annotation.Annotation> annotation) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(method -> path(method, annotation))
                .filter(path -> path != null)
                .collect(Collectors.toSet());
    }

    private static String path(Method method, Class<? extends java.lang.annotation.Annotation> annotation) {
        if (annotation == GetMapping.class) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return mapping == null ? null : mapping.value()[0];
        }
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        return mapping == null ? null : mapping.value()[0];
    }
}
