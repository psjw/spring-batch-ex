package com.psjw.springbatchex.job;

import com.psjw.springbatchex.entity.InvalidPaymentAmountException;
import com.psjw.springbatchex.entity.Payment;
import com.psjw.springbatchex.entity.PaymentRepository;
import com.psjw.springbatchex.entity.PaymentSource;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.Collections;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentReportJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PaymentRepository paymentRepository;

    /**
     * JpaPagingItemReader -> limit, offset기반의 sql 조회만들기
     */
    @Bean
    public Job paymentReportJob(
            Step paymentReportStep
    ) {
        return new JobBuilder("paymentReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(paymentReportStep)
                .build();
    }

    /**
     * PaymentSource -> Payment
     *
     * @return
     */
    @Bean
    public Step paymentReportStep(
            JpaPagingItemReader<PaymentSource> paymentReportReader,
            ItemProcessor<PaymentSource, Payment> paymentReportProcessor,
            JpaItemWriter<Payment> paymentJpaItemWriter
    ) {
        // FaultTolerantStepBuilder를 통해서 기본 정책, 기본 policy, skipLimit 기본값
        // FaultTolerantChunkProcessor 실질적으로 내결합성의 관련된 로직들 수행 -> skiplimit 초과했는지
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(10, transactionManager)
                .reader(paymentReportReader)
                .processor(paymentReportProcessor)
                .writer(paymentJpaItemWriter)
                .faultTolerant()
//                .skipLimit(2) // 최대 2번 까지 skip 허용, skip = 예외 발생
                .skip(InvalidPaymentAmountException.class)
//                .skipPolicy(new LimitCheckingItemSkipPolicy(
//                        1,
//                        throwable -> {
//                            if (throwable instanceof InvalidPaymentAmountException) {
//                                return false;
//                            } else if (throwable instanceof IllegalStateException) {
//                                return false;
//                            } else {
//                                return false;
//                            }
//                        }
//                )) //횟수 기반
//                .skipPolicy(new AlwaysSkipItemSkipPolicy()) // 항상 skip을 하는 정책
                //항상 skip을 허용하지 않음, 다음 Policy 예를 들어 LimitCheckingItemSkipPolicy가 동작
//                .skipPolicy(new NeverSkipItemSkipPolicy()) // 항상 skip을 하지 않는 정책
                .build();
    }

    @Bean
    @StepScope //@Value로 받으려면
    public JpaPagingItemReader<PaymentSource> paymentReportReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ) {
        return new JpaPagingItemReaderBuilder<PaymentSource>()
                .name("paymentReportReader")
                .entityManagerFactory(entityManagerFactory)
                /**
                 * Payment_Source where payment_date = 2025-05-02 -> Job Parameter
                 */
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .pageSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<PaymentSource, Payment> itemProcessor() {
        /**
         * 최종 결제 금액이 0원 -> payment -> X
         */
        return paymentSource -> {
            // 최종 금액이 0원인 경우 제외
//            if(paymentSource.getFinalAmount().compareTo(BigDecimal.ZERO) == 0){
//                return null; //wirter에 전달 X
//            }

            /**
             * 할인 금액이 마이너스가 되는 케이스
             *
             * 1,000 - 100(할인) = 900
             * 음수가 들어오는 경우는 원장에서 어떤 처리가 잘못된
             */
            //할인 금액이 음수인 경우
            if (paymentSource.getDiscountAmount().signum() == -1) {
                final String msg = "할인 금액이 0 아닌 결제는 처리할 수 없습니다. 현재 할인 금액 : " + paymentSource.getDiscountAmount();
                log.error(msg);
                throw new InvalidPaymentAmountException(msg);
            }

            return new Payment(
                    null,
                    paymentSource.getFinalAmount(),
                    paymentSource.getPaymentDate(),
                    "PAYMENT"
            );
        };
    }

    /**
     * 데이터베이스에 반영을 한다
     * payment table -> insert
     * 단건 insert 여러개
     * 1. 배치 어플리케이션 -> mysql서버로 쿼리전달
     * 2. mysql 서버 해당 sql 수행이후 ACK 보냄
     * 3. 배치 어플리케이션에서 ACK 확인
     *
     * @return
     */
    @Bean
    public JpaItemWriter<Payment> paymentJpaItemWriter() {
        JpaItemWriter<Payment> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

//    private ItemWriter<Payment> itemWriter() {
//        return paymentRepository::saveAllAndFlush;
//    }

//    private ItemWriter<Payment> itemWriter() {
//        return items -> items.forEach(paymentSource ->
//                log.info("Payment 로그 출력: 금액={}, 결제일={}, 상태={}",
//                        paymentSource.getAmount(),
//                        paymentSource.getPaymentDate(),
//                        paymentSource.getStatus()));
//    }

//    private ItemWriter<PaymentSource> itemWriter(){
//        return items -> {
//            items.forEach(item -> {
//                log.info("PaymentSource 로그 출력: 원래 금액={}, 할인 금액={}, 최종 금액={}, 결제일={}",
//                        item.getOriginalAmount(),
//                        item.getDiscountAmount(),
//                        item.getFinalAmount(),
//                        item.getPaymentDate()
//                );
//            });
//        };
//
//    }
}
