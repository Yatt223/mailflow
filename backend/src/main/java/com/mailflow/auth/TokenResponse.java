package com.mailflow.auth;

public record TokenResponse(String token, String type, long expireDansSecondes) {}