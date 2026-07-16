package com.booking.domain.enums;

/**
 * Lý do 1 jti bị đưa vào tokens_blacklist
 *
 * Áp dụng cho cả ACCESS jti và REFRESH jti.
 * Lưu DB dưới dạng String (qua .name())
 */
public enum BlacklistReason {

    /** User tự logout */
    LOGOUT,

    /** Login mới → revoke session cũ */
    NEW_LOGIN,

    /** Admin force revoke */
    ADMIN_REVOKE,

    /** User đổi password */
    PASSWORD_CHANGE
}