package com.custacm.platform.trainingdata.codeforces.app.purge;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesStudentDataPurgeRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesStudentDataPurgeServiceTest {
    private final CodeforcesStudentDataPurgeRepository repository = mock(CodeforcesStudentDataPurgeRepository.class);
    private final CodeforcesStudentDataPurgeService service = new CodeforcesStudentDataPurgeService(repository);

    @Test
    void trimsIdentityAndDelegatesToRepository() {
        CodeforcesStudentDataPurgeResult result = new CodeforcesStudentDataPurgeResult(
                "112487张三",
                "tourist",
                1,
                2,
                3,
                4,
                5
        );
        when(repository.purgeByStudentIdentity("112487张三")).thenReturn(result);

        assertThat(service.purgeStudentData(" 112487张三 ")).isEqualTo(result);
        verify(repository).purgeByStudentIdentity("112487张三");
    }

    @Test
    void rejectsBlankIdentity() {
        assertThatThrownBy(() -> service.purgeStudentData(" "))
                .isInstanceOfSatisfying(CodeforcesStudentDataPurgeException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesStudentDataPurgeException.ErrorCode
                                        .CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST
                        ));
    }
}
