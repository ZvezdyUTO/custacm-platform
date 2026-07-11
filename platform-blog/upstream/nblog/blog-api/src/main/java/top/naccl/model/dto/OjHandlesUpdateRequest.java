package top.naccl.model.dto;

import java.util.Map;

public record OjHandlesUpdateRequest(Map<String, String> handles, Boolean needCollect) {
}
