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
    void keepsCollectionSchedulesDefinedButDisabledUntilDeploymentOptsIn() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties")
        );

        assertSchedule(
                properties,
                0,
                "codeforces-daily-one-hundred-hours",
                "CODEFORCES",
                "${BLOG_CODEFORCES_DAILY_COLLECTION_ENABLED:false}",
                "0 0 0 * * *",
                "${BLOG_DAILY_COLLECTION_LOOKBACK:100h}"
        );
        assertSchedule(
                properties,
                1,
                "codeforces-half-hour-no-overlap",
                "CODEFORCES",
                "${BLOG_CODEFORCES_INTRADAY_COLLECTION_ENABLED:false}",
                "0 0,30 1-23 * * *",
                "${BLOG_INTRADAY_COLLECTION_LOOKBACK:0h}"
        );
        assertSchedule(
                properties,
                2,
                "atcoder-daily-one-hundred-hours",
                "ATCODER",
                "${BLOG_ATCODER_DAILY_COLLECTION_ENABLED:false}",
                "0 15 0 * * *",
                "${BLOG_DAILY_COLLECTION_LOOKBACK:100h}"
        );
        assertSchedule(
                properties,
                3,
                "atcoder-half-hour-no-overlap",
                "ATCODER",
                "${BLOG_ATCODER_INTRADAY_COLLECTION_ENABLED:false}",
                "0 15,45 1-23 * * *",
                "${BLOG_INTRADAY_COLLECTION_LOOKBACK:0h}"
        );
        assertEquals("4s", properties.getProperty("platform.training-data.collector.job-item-interval"));
        assertEquals("${BLOG_ATCODER_PROBLEM_LIST_SCHEDULE_ENABLED:false}",
                properties.getProperty("platform.training-data.atcoder.problem-list-collector.enabled"));
        assertEquals("${BLOG_ATCODER_PROBLEM_LIST_BOOTSTRAP_ENABLED:false}",
                properties.getProperty("platform.training-data.atcoder.problem-list-collector.bootstrap-on-startup"));
    }

    private void assertSchedule(
            Properties properties,
            int index,
            String name,
            String ojName,
            String enabled,
            String cron,
            String lookback
    ) {
        String prefix = "platform.training-data.collector.schedules[" + index + "].";
        assertEquals(name, properties.getProperty(prefix + "name"));
        assertEquals(ojName, properties.getProperty(prefix + "oj-name"));
        assertEquals(enabled, properties.getProperty(prefix + "enabled"));
        assertEquals(cron, properties.getProperty(prefix + "cron"));
        assertEquals("Asia/Shanghai", properties.getProperty(prefix + "zone"));
        assertEquals(lookback, properties.getProperty(prefix + "lookback"));
    }
}
