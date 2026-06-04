package com.booking.shared.util;

/**
 * Utility class để che thông tin nhạy cảm trong log
 * Không bao giờ log plaintext: password, token, email, phone
 */
public class MaskUtil {

    private MaskUtil() {}

    /**
     * Che username: "alice" → "al***"
     */

    public static String maskUsername(String username) {
        if(username == null || username.length() < 2)
            return "***";

        return username.substring(0, 2) + "***";

    }

    /**
     * Che email: "alice@gmail.com" → "al***@gmail.com"
     */
    public static String maskEmail(String email) {
        if(email == null || email.length() < 2)
            return "***";

        String [] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String masked = local.length() > 2
                ? local.substring(0, 2) + "***"
                : "***";

        return masked + "@" + domain;
    }

    /**
     * Che phone: "0912345678" → "091***678"
     */

    public static String maskPhone(String phone){
        if(phone == null || phone.length() < 2)
            return "***";

        return phone.substring(0, 3)
                + "***"
                +phone.substring(phone.length() - 3);
    }

    /**
     * Che password: bất kỳ → "***"
     */
    public static String maskPassword(String password){
        return "***";
    }

    /**
     * Che token: chỉ hiện 10 ký tự đầu
     * "eyJhbGci..." → "eyJhbGci.."
     */
    public static String maskToken(String token){
        if (token == null || token.length() < 10)
            return "***";

        return token.substring(0, 10) + "***";
    }

}
