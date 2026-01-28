package com.school.management.interfaces.rest.access.dto;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
    private Boolean logoutAll;
}
