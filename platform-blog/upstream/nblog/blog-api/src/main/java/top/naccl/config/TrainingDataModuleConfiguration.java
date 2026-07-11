package top.naccl.config;

import com.custacm.platform.trainingdata.atcoder.config.AtcoderProblemListSchedulingConfig;
import com.custacm.platform.trainingdata.atcoder.config.AtcoderTrainingDataConfig;
import com.custacm.platform.trainingdata.codeforces.config.CodeforcesTrainingDataConfig;
import com.custacm.platform.trainingdata.common.config.CommonTrainingDataConfig;
import com.custacm.platform.trainingdata.common.scheduler.OjCollectorSchedulingConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        CommonTrainingDataConfig.class,
        CodeforcesTrainingDataConfig.class,
        AtcoderTrainingDataConfig.class,
        OjCollectorSchedulingConfig.class,
        AtcoderProblemListSchedulingConfig.class
})
public class TrainingDataModuleConfiguration {
}
