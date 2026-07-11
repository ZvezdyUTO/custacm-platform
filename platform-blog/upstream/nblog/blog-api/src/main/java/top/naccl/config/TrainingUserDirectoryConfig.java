package top.naccl.config;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class TrainingUserDirectoryConfig {
    @Bean
    OjHandleAccountService ojHandleAccountService(OjHandleAccountRepository repository) {
        return new OjHandleAccountService(repository, Clock.system(ZoneOffset.ofHours(8)));
    }
}
