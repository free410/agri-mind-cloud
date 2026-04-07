package com.smartagri.security;

public final class LoginAdminContext {

    private static final ThreadLocal<Long> ADMIN_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    private LoginAdminContext() {
    }

    public static void setAdmin(Long adminId, String username) {
        ADMIN_ID_HOLDER.set(adminId);
        USERNAME_HOLDER.set(username);
    }

    public static Long getAdminId() {
        return ADMIN_ID_HOLDER.get();
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void clear() {
        ADMIN_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
