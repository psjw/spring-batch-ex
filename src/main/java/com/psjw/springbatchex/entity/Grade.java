package com.psjw.springbatchex.entity;

import lombok.Getter;

@Getter
public enum Grade {
    INIT("초기 등급"),
    BASIC("기본 등급"),
    PREMIUM("프리미엄 등급"),
    VIP("VIP 등급");

    private final String description;

    Grade(String description) {
        this.description = description;
    }
}