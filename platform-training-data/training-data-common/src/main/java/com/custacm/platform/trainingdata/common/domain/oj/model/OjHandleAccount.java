package com.custacm.platform.trainingdata.common.domain.oj.model;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjHandleAccount(
        String studentIdentity,
        Map<String, String> handles,
        boolean needCollect,
        Map<String, OjHandleCollectionState> collectionStates,
        Instant createdAt,
        Instant updatedAt
) {
    public OjHandleAccount(
            String studentIdentity,
            Map<String, String> handles,
            boolean needCollect,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(studentIdentity, handles, needCollect, Map.of(), createdAt, updatedAt);
    }

    public OjHandleAccount {
        requireText(studentIdentity, "studentIdentity");
        handles = normalizeHandles(handles);
        collectionStates = normalizeCollectionStates(handles, collectionStates);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Map<String, String> normalizeHandles(Map<String, String> handles) {
        if (handles == null || handles.isEmpty()) {
            throw new IllegalArgumentException("handles must not be empty");
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        handles.forEach((ojName, handle) -> {
            String normalizedOjName = OjNames.normalize(ojName);
            requireText(handle, "handles." + normalizedOjName);
            normalized.put(normalizedOjName, handle.trim());
        });
        Map<String, String> ordered = new LinkedHashMap<>();
        putIfPresent(ordered, normalized, OjNames.CODEFORCES);
        putIfPresent(ordered, normalized, OjNames.ATCODER);
        return Collections.unmodifiableMap(ordered);
    }

    public static Map<String, OjHandleCollectionState> normalizeCollectionStates(
            Map<String, String> handles,
            Map<String, OjHandleCollectionState> collectionStates
    ) {
        Map<String, String> normalizedHandles = normalizeHandles(handles);
        Map<String, OjHandleCollectionState> normalizedStates = new LinkedHashMap<>();
        if (collectionStates != null) {
            collectionStates.forEach((ojName, state) -> {
                String normalizedOjName = OjNames.normalize(ojName);
                if (normalizedHandles.containsKey(normalizedOjName)) {
                    normalizedStates.put(
                            normalizedOjName,
                            state == null ? OjHandleCollectionState.empty() : state
                    );
                }
            });
        }
        Map<String, OjHandleCollectionState> ordered = new LinkedHashMap<>();
        for (String ojName : normalizedHandles.keySet()) {
            ordered.put(ojName, normalizedStates.getOrDefault(ojName, OjHandleCollectionState.empty()));
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static void putIfPresent(Map<String, String> target, Map<String, String> source, String ojName) {
        String handle = source.get(ojName);
        if (handle != null) {
            target.put(ojName, handle);
        }
    }
}
