package com.example.grocery.dto.forgot;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private Long otpRefId;
    private String otp;
}
