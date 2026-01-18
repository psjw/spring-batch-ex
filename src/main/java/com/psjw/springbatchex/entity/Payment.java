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
@Table(name = "payment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 결제금액
     */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * 결제일
     */
    @Column(nullable = false)
    private LocalDate paymentDate;

    /**
     * 결제 상태, 취소, 부분 취소
     */
    @Column(nullable = false)
    private String status;

}
