package com.alihsan.backend.util;

public final class MobileNumberUtil {
    private MobileNumberUtil() {
    }

    public static String normalize(String mobile) {
        if (mobile == null) {
            return null;
        }
        String digitsOnly = mobile.replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) {
            return "";
        }
        if (digitsOnly.startsWith("00")) {
            digitsOnly = digitsOnly.substring(2);
        }
        if (digitsOnly.startsWith("252")) {
            return digitsOnly;
        }
        if (digitsOnly.startsWith("0")) {
            return "252" + digitsOnly.substring(1);
        }
        return digitsOnly;
    }
}
