package com.custacm.platform.auth.web;

public record CreateUserRequest(String studentIdentity, String password, String role) {
}
