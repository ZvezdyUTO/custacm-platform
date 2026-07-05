package com.custacm.platform.auth.web;

public record UpdateUserRequest(String role, String newPassword) {
}
