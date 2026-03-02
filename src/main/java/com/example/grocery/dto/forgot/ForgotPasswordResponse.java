package com.example.grocery.dto.forgot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordResponse {
    private boolean success;
    private String message;
    private Long otpRefId;
}
