package com.booking.presentation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorResponse {

    // QR code URL để user quét bằng Google Authenticator
    private String qrCodeUrl;

    // Secret key (backup nếu không quét được QR)
    private String secret;

    private String message;

}
