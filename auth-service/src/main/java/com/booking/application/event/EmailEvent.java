package com.booking.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event gửi email qua Kafka
 * Producer: AuthService
 * Consumer: EmailConsumer
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    /**
     * Event gửi email qua Kafka
     * Producer: AuthService
     * Consumer: EmailConsumer
     */

    private String type;

    //email người nhận
    private String to;

    //tên người nhận
    private String username;

    //dữ liệu kèm theo (token, OTP,....)
    private String payload;

}
