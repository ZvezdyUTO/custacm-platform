package top.naccl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.context.annotation.Import;
import top.naccl.config.TrainingDataModuleConfiguration;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void enablesDefaultCodeforcesAndAtcoderCollectionSchedules() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties")
        );

        assertSchedule(
                properties,
                0,
                "codeforces-daily-five-days",
                "CODEFORCES",
                "0 0 0 * * *",
                "120h"
        );
        assertSchedule(
                properties,
                1,
                "codeforces-half-hour-one-hour",
                "CODEFORCES",
                "0 0,30 1-23 * * *",
                "1h"
        );
        assertSchedule(
                properties,
                2,
                "atcoder-daily-five-days",
                "ATCODER",
                "0 15 0 * * *",
                "120h"
        );
        assertSchedule(
                properties,
                3,
                "atcoder-half-hour-one-hour",
                "ATCODER",
                "0 15,45 1-23 * * *",
                "1h"
        );
        assertEquals("4s", properties.getProperty("platform.training-data.collector.job-item-interval"));
    }

    private void assertSchedule(
            Properties properties,
            int index,
            String name,
            String ojName,
            String cron,
            String lookback
    ) {
        String prefix = "platform.training-data.collector.schedules[" + index + "].";
        assertEquals(name, properties.getProperty(prefix + "name"));
        assertEquals(ojName, properties.getProperty(prefix + "oj-name"));
        assertEquals("true", properties.getProperty(prefix + "enabled"));
        assertEquals(cron, properties.getProperty(prefix + "cron"));
        assertEquals("Asia/Shanghai", properties.getProperty(prefix + "zone"));
        assertEquals(lookback, properties.getProperty(prefix + "lookback"));
    }
}
