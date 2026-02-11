package com.psjw.springbatchex.service;

import com.psjw.springbatchex.entity.Grade;
import org.springframework.stereotype.Service;

@Service
public class OrderClient {
    public Grade getGrade(Long userId) {
        // 150ms 대기, 외부 API 호출 하는 것처럼 응답 지연
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (userId % 3 == 0) {
            return Grade.VIP;
        } else if (userId % 2 == 0) {
            return Grade.PREMIUM;
        } else {
            return Grade.BASIC;
        }
    }
}
