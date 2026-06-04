package com.booking.application.port.in;


import com.booking.presentation.request.RegisterRequest;
import com.booking.presentation.response.RegisterResponse;

/**
 * Use Case: Đăng ký tài khoản mới
 * → Định nghĩa hành động, không quan tâm implementation
 * → Controller chỉ biết interface này, không biết AuthServiceImpl
 */
public interface RegisterUseCase {
    RegisterResponse register(RegisterRequest registerRequest);
}
