package com.example.grocery.dto.forgot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private boolean success;
    private String message;
    private String resetToken;
}
