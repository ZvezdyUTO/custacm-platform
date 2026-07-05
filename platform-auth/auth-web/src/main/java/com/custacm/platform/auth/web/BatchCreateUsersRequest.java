package com.custacm.platform.auth.web;

import java.util.List;

public record BatchCreateUsersRequest(List<CreateUserRequest> users) {
}
