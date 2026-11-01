package com.pmtool.backend.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CognitoUserInfo(
        String sub,
        String name,
        String email,

        @JsonProperty("email_verified")
        Boolean emailVerified
) {
}
