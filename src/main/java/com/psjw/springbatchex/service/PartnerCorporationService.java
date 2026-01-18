package com.psjw.springbatchex.service;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 실제 HTTP 통신은 X, 딜레이를 주고 HTTP 통신하는 것처럼 동작 랜덤으로 예외가 발생
 */
@Service
@Slf4j
public class PartnerCorporationService {

    // 호출 횟루를 추적하는 원자적 카운터(스레드 안전)
    private int failureCount = 0;
    private static final String TIMEOUT_ERROR_MESSAGE = "파트너 API 서버연결 실패: 타임아웃 발생";
    private static final int HTTP_REQUEST_DELAY_MS = 200; //200ms

    // 실제 HTTP X, mock 객체 처럼 사업자 번호 <-> 상호명을
    public static final Map<String, String> PARTNER_CORP = Map.ofEntries(
            Map.entry("000-01-00001", "삼성전자"),
            Map.entry("000-01-00002", "LG전자"),
            Map.entry("000-01-00003", "현대자동차"),
            Map.entry("000-01-00004", "SK텔레콤"),
            Map.entry("000-01-00005", "네이버"),
            Map.entry("000-01-00006", "카카오"),
            Map.entry("000-01-00007", "쿠팡"),
            Map.entry("000-01-00008", "배달의민족"),
            Map.entry("000-01-00009", "토스"),
            Map.entry("000-01-00010", "당근마켓"),
            Map.entry("000-01-00011", "KT"),
            Map.entry("000-01-00012", "롯데그룹"),
            Map.entry("000-01-00013", "포스코"),
            Map.entry("000-01-00014", "신한금융그룹"),
            Map.entry("000-01-00015", "KB금융그룹"),
            Map.entry("000-01-00016", "농협"),
            Map.entry("000-01-00017", "하나금융그룹"),
            Map.entry("000-01-00018", "대한항공"),
            Map.entry("000-01-00019", "아시아나항공"),
            Map.entry("000-01-00020", "CJ그룹")
    );

    /**
     * 파트너 회사명을 HTTP API 호출을 통해 가져오는 메서드(가상) 200ms 지연이 있으며, 10번 중 1번은 HTTP 통신 실패 예외가 발생함
     */
    public String getPartnerCorpName(String businessRegistrationNumber) {
        checkFailureByCallCount();
        // HTTP 통신 -> MAP으로 대체
        String partnerCorpName = PARTNER_CORP.getOrDefault(businessRegistrationNumber, "NONE");
        log.info("파트너 사업자번호 {}의 회사명 조회 성공: {}", businessRegistrationNumber, partnerCorpName);
        return partnerCorpName;
    }

    /**
     * 호출 횟수에 따라 예외 발생 여부를 결정하는 메서드 failureCount 횟수마다 한번씩 예외를 발생시킨다.
     */
    private void checkFailureByCallCount() {
        try {
            TimeUnit.MICROSECONDS.sleep(HTTP_REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //랜덤하게 10% 확률로 실패하도록 변경
        if (failureCount < 1 && Math.random() < 0.1) {
            failureCount++;
//            log.warn("{}번째 호출에서 랜덤하게 예외 발생", failureCount);
            final String msg = String.format("%s %d 번째 호출에서 랜덤하게 예외 발생", TIMEOUT_ERROR_MESSAGE, failureCount);
            log.error(msg);
            throw new PartnerHttpException(msg);
        }

    }


}
