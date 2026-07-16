package com.booking.domain.enums;

/**
 * Lý do 1 refresh token (auth.tokens) bị deactivate
 *
 * Dùng làm value cho cột deactivation_reason
 * → Lưu DB dưới dạng String (qua .name())
 */
public enum DeactivationReason {

    /** Login mới đẩy token cũ ra (single session) */
    NEW_LOGIN,

    /** User tự logout */
    LOGOUT,

    /** Admin force revoke (revoke all sessions hoặc revoke 1 jti) */
    ADMIN_REVOKE,

    /** Token hết hạn (cleanup job tự đánh dấu) */
    EXPIRED,

    /** User đổi password → invalidate hết session cũ */
    PASSWORD_CHANGE
}