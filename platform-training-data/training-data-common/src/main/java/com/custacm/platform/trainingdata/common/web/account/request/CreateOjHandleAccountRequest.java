package com.custacm.platform.trainingdata.common.web.account.request;

import java.util.Map;

public record CreateOjHandleAccountRequest(String studentIdentity, Map<String, String> handles) {
}
