package top.naccl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import top.naccl.config.TrainingDataModuleConfiguration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BlogApiModuleCompositionTest {
    @Test
    void scansBlogAndTrainingModulesFromTheSingleApplication() {
        SpringBootApplication annotation = BlogApiApplication.class.getAnnotation(SpringBootApplication.class);

        assertArrayEquals(
                new String[]{"top.naccl"},
                annotation.scanBasePackages()
        );
		org.junit.jupiter.api.Assertions.assertNotNull(
				TrainingDataModuleConfiguration.class.getAnnotation(Import.class));
    }
}
