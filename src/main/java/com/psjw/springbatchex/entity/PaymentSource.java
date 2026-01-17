package com.psjw.springbatchex.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_source")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 파트너 회사명
     */
    @Column(nullable = false, length = 100)
    private String partnerCorpName;

    /**
     * 원래금액
     */
    @Column(nullable = false)
    private BigDecimal originalAmount;

    /**
     * 할인금액
     */
    @Column(nullable = false)
    private BigDecimal discountAmount;

    /**
     * 최종금액
     */
    @Column(nullable = false)
    private BigDecimal finalAmount;


    /**
     * 결제일자
     */
    @Column(nullable = false)
    private LocalDate paymentDate;

}
