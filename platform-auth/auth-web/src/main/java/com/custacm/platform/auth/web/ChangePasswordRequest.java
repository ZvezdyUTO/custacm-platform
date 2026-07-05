package com.custacm.platform.auth.web;

public record ChangePasswordRequest(String oldPassword, String newPassword, String confirmNewPassword) {
}
