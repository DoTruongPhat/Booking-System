package com.booking.domain.exception;

public final class ErrorCode {

    private ErrorCode() {}

    // ── Authentication ────────────────────────────────────
    public static final String AUTH_001 = "AUTH_001";
    public static final String AUTH_001_MSG = "Invalid username or password";

    public static final String AUTH_002 = "AUTH_002";
    public static final String AUTH_002_MSG = "Account is locked";

    public static final String AUTH_003 = "AUTH_003";
    public static final String AUTH_003_MSG = "Token is invalid or expired";

    public static final String AUTH_004 = "AUTH_004";
    public static final String AUTH_004_MSG = "Current password is incorrect";

    public static final String AUTH_005 = "AUTH_005";
    public static final String AUTH_005_MSG = "Invalid or expired OTP";

    // ── User ──────────────────────────────────────────────
    public static final String USR_001 = "USR_001";
    public static final String USR_001_MSG = "User not found";

    public static final String USR_002 = "USR_002";
    public static final String USR_002_MSG = "Username already exists";

    public static final String USR_003 = "USR_003";
    public static final String USR_003_MSG = "Email already exists";

    public static final String USR_004     = "USR_004";
    public static final String USR_004_MSG = "Username is required";

    public static final String USR_005     = "USR_005";
    public static final String USR_005_MSG = "Username must be 3-100 characters";

    public static final String USR_006     = "USR_006";
    public static final String USR_006_MSG = "Email is required";

    public static final String USR_007     = "USR_007";
    public static final String USR_007_MSG = "Invalid email format";

    public static final String USR_008     = "USR_008";
    public static final String USR_008_MSG = "Password hash is required";

    public static final String USR_009     = "USR_009";
    public static final String USR_009_MSG = "Timezone is required";

    public static final String USR_010     = "USR_010";
    public static final String USR_010_MSG = "Username only allows letters, numbers and underscore";

    public static final String USR_011     = "USR_011";
    public static final String USR_011_MSG = "Password must be at least 8 characters";

    // ── Token ─────────────────────────────────────────────
    public static final String TKN_001 = "TKN_001";
    public static final String TKN_001_MSG = "Token has been revoked";

    public static final String TKN_002 = "TKN_002";
    public static final String TKN_002_MSG = "Token not found";

    public static final String TKN_003 = "TKN_003";
    public static final String TKN_003_MSG = "Session not found";

    // ── Permission ────────────────────────────────────────
    public static final String RBC_001 = "RBC_001";
    public static final String RBC_001_MSG = "Access denied";

    // ── Common ────────────────────────────────────────────
    public static final String CMN_001 = "CMN_001";
    public static final String CMN_001_MSG = "Internal server error";

    public static final String CMN_002 = "CMN_002";
    public static final String CMN_002_MSG = "Invalid request data";

    public static final String CMN_003 = "CMN_003";
    public static final String CMN_003_MSG = "Access denied";

    public static final String CMN_004 = "CMN_004";
    public static final String CMN_004_MSG = "System Param not found: ";

    public static final String CMN_005 = "CMN_005";
    public static final String CMN_005_MSG = "Role not found: ";

    public static final String CMN_006 = "CMN_006";
    public static final String CMN_006_MSG = "Ticket not found: ";

    public static final String CMN_007 = "CMN_007";
    public static final String CMN_007_MSG = "Cannot assign ticket with status: ";

    public static final String CMN_008 = "CMN_008";
    public static final String CMN_008_MSG = "Ticket already closed ";
}
