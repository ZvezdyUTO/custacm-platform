package com.custacm.platform.trainingdata.codeforces.app.purge;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesStudentDataPurgeRepository;

public class CodeforcesStudentDataPurgeService {
    private final CodeforcesStudentDataPurgeRepository repository;

    public CodeforcesStudentDataPurgeService(CodeforcesStudentDataPurgeRepository repository) {
        this.repository = repository;
    }

    public CodeforcesStudentDataPurgeResult purgeStudentData(String studentIdentity) {
        return repository.purgeByStudentIdentity(requireText(studentIdentity, "studentIdentity"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CodeforcesStudentDataPurgeException(
                    CodeforcesStudentDataPurgeException.ErrorCode.CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST,
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }
}
