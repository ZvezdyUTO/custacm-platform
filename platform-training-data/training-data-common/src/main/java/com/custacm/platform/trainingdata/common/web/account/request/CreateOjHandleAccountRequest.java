package com.custacm.platform.trainingdata.common.web.account.request;

import java.util.Map;

public record CreateOjHandleAccountRequest(String username, Map<String, String> handles) {
}
