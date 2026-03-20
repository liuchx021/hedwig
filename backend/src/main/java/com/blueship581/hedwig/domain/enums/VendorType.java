package com.blueship581.hedwig.domain.enums;

import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Locale;

public enum VendorType {
    OTTAI("OTTAI"),
    SISENSING("SISENSING");

    @EnumValue
    @JSONField(value = true)
    private final String jsonValue;

    VendorType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JSONCreator
    public static VendorType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("厂商类型不能为空");
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "OTTAI" -> OTTAI;
            case "SISENSING", "SI_SENSING" -> SISENSING;
            default -> throw new IllegalArgumentException("不支持的厂商类型：" + value);
        };
    }

    public String toJsonValue() {
        return jsonValue;
    }
}
