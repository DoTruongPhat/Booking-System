package com.booking.application.validator;

import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.UserException;
import com.booking.domain.exception.ValidationException;
import org.springframework.stereotype.Component;

/**
 * UserValidator = validation business rules cho User
 *
 * Tại sao tách ra class riêng?
 * → Service không nên chứa validation logic
 * → Dễ test riêng lẻ
 * → Tái sử dụng ở nhiều chỗ
 * → Single Responsibility Principle
 *
 * Tại sao không validate trong setter?
 * → Setter gọi khi load từ DB → không cần validate
 * → Setter gọi từ mapper → không nên throw exception
 * → Validation chỉ cần khi nhận input từ client
 */

@Component
public class UserValidator {
    /**
     * Validate username
     * → Gọi khi register hoặc update username
     */

    public void validateUsername(String username) {
        if(username == null || username.isBlank()){
            throw new UserException(
                    ErrorCode.USR_004,
                    ErrorCode.USR_004_MSG
            );
        }

        if(username.length() < 3 || username.length() > 100){
            throw new UserException(
                    ErrorCode.USR_005,
                    ErrorCode.USR_005_MSG
            );
        }

        if(!username.matches("^[a-zA-Z0-9_]+$")){
            throw new UserException(
                    ErrorCode.USR_010,
                    ErrorCode.USR_010_MSG
            );
        }

    }

    /**
     * Validate email
     */
    public void validateEmail(String email) {
        if(email == null || email.isBlank()){
            throw new UserException(
                    ErrorCode.USR_006,
                    ErrorCode.USR_006_MSG
            );
        }

        if(!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")){
            throw new UserException(
                    ErrorCode.USR_007,
                    ErrorCode.USR_007_MSG
            );
        }
    }

        /**
        * Validate password
        */
    public void validatePassword(String password) {
        if (password == null || password.isBlank())
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must not be blank");

        if (password.length() < 8)
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must be at least 8 characters");

        if (!password.matches(".*[A-Z].*"))
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must contain at least 1 uppercase letter");

        if (!password.matches(".*[a-z].*"))
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must contain at least 1 lowercase letter");

        if (!password.matches(".*[0-9].*"))
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must contain at least 1 number");

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            throw new ValidationException(
                    ErrorCode.CMN_002,
                    "Password must contain at least 1 special character");
    }

    /**
     * Validate timezone
     */
    public void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new UserException(
                    ErrorCode.USR_009,
                    ErrorCode.USR_009_MSG
            );
        }
    }



}
