package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OjWarehouseRefreshDispatcher implements OjSubmissionCollectionJobService.RefreshHandler {
    private final Map<String, OjWarehouseRefreshHandler> handlersByOjName;

    public OjWarehouseRefreshDispatcher(List<OjWarehouseRefreshHandler> handlers) {
        this.handlersByOjName = handlersByOjName(handlers);
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(OjSubmissionCollectionResult result) {
        String ojName = OjNames.normalize(result.ojName());
        OjWarehouseRefreshHandler handler = handlersByOjName.get(ojName);
        if (handler == null) {
            return OjSubmissionCollectionJobRefreshResult.failed(ojName + " warehouse refresh is not implemented");
        }
        return handler.refresh(result.batchId());
    }

    private static Map<String, OjWarehouseRefreshHandler> handlersByOjName(List<OjWarehouseRefreshHandler> handlers) {
        Map<String, OjWarehouseRefreshHandler> indexed = new LinkedHashMap<>();
        for (OjWarehouseRefreshHandler handler : handlers == null ? List.<OjWarehouseRefreshHandler>of() : handlers) {
            OjWarehouseRefreshHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
            String ojName = OjNames.normalize(nonNullHandler.ojName());
            if (indexed.putIfAbsent(ojName, nonNullHandler) != null) {
                throw new IllegalArgumentException("duplicate OJ warehouse refresh handler: " + ojName);
            }
        }
        return Map.copyOf(indexed);
    }
}
