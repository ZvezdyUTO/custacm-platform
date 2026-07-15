package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Bounded writer for one logical collection batch.
 *
 * <p>Every chunk written through one instance must share the same batch id so
 * downstream warehouse refresh can process the complete collection window.</p>
 *
 * @author huangbingrui.awa
 */
public interface OjSubmissionCollectionBatchWriter {
    void write(String handle, List<JsonNode> submissions) throws JsonProcessingException;

    int writtenRows();

    OjSubmissionCollectionWriteResult result();

    default boolean hasWrites() {
        return writtenRows() > 0;
    }
}
