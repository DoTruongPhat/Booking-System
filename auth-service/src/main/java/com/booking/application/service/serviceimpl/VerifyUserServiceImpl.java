package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.VerifyUserUseCase;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.PasswordService;
import com.booking.domain.model.User;
import com.booking.presentation.request.VerifyUserRequest;
import com.booking.presentation.response.VerifyUserResponse;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * VerifyUserServiceImpl
 *
 * LUỒNG:
 *   1. Tìm user theo username
 *   2. Verify password (BCrypt)
 *   3. Trả về valid + user info nếu đúng
 *
 * DÙNG CHO:
 *   - Microservice khác verify user (booking, payment, ...)
 *   - Kiểm tra credentials trước khi cho action nhạy cảm
 *   - KHÔNG cấp token, chỉ verify
 *
 * TÁCH RIÊNG vì:
 *   - Use case khác login (login → cấp token, verify → chỉ check)
 *   - Không tạo session, không tạo JWT
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class VerifyUserServiceImpl implements VerifyUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordService passwordService;

    @Override
    public VerifyUserResponse verifyUser(VerifyUserRequest request) {
        String username = request.getUsername();
        log.info("[VerifyUser] Request: {}", MaskUtil.maskUsername(username));

        // Bước 1: Tìm user
        Optional<User> opt = userRepositoryPort.findByUsername(username);
        if (opt.isEmpty()) {
            log.warn("[VerifyUser] User not found: {}", MaskUtil.maskUsername(username));
            return buildInvalidResponse();
        }

        User user = opt.get();

        // Bước 2: Verify password
        boolean valid = passwordService.verify(
            request.getPassword(),
            user.getPasswordHash(),
            user.getPasswordSalt(),
            user.getUsername()
        );

        if (!valid) {
            log.warn("[VerifyUser] Wrong password: {}", MaskUtil.maskUsername(username));
            return buildInvalidResponse();
        }

        // Bước 3: Trả về thông tin user
        VerifyUserResponse response = new VerifyUserResponse();
        response.setValid(true);
        response.setUserId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRoles(
            user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(r -> r.getCode()).toList()
        );

        log.info("[VerifyUser] Success: {}", MaskUtil.maskUsername(username));
        return response;
    }

    private VerifyUserResponse buildInvalidResponse() {
        VerifyUserResponse response = new VerifyUserResponse();
        response.setValid(false);
        return response;
    }
}
