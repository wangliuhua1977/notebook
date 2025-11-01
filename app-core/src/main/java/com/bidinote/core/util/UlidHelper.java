package com.bidinote.core.util;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * ULID 生成工具。用于块级 ID 与页面 ID。
 */
public final class UlidHelper {
    private UlidHelper() {
    }

    public static String newUlid() {
        return UlidCreator.getUlid().toString();
    }
}
