package com.project.app.identity.service;

public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static boolean accepts(String password) {
        if (password == null) return false;
        int points = password.codePointCount(0, password.length());
        return points >= 12 && points <= 128;
    }
}
