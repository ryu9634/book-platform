package com.ryu.ebook.user.auth.dto;

public record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken) {

    public static TokenResponse bearer(String accessToken, long expiresIn, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, expiresIn, refreshToken);
    }
}
