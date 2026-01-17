package com.psjw.springbatchex;

import com.psjw.springbatchex.entity.Payment;
import com.psjw.springbatchex.entity.PaymentRepository;
import com.psjw.springbatchex.entity.PaymentSource;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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
     * @return
     */
    @Bean
    public Step paymentReportStep(
            JpaPagingItemReader<PaymentSource> paymentReportReader,
            ItemProcessor<PaymentSource, Payment> paymentReportProcessor,
            JpaItemWriter<Payment> paymentJpaItemWriter
    ){
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(10, transactionManager)
                .reader(paymentReportReader)
                .processor(paymentReportProcessor)
                .writer(paymentJpaItemWriter)
                .build();
    }

    @Bean
    @StepScope //@Value로 받으려면
    public JpaPagingItemReader<PaymentSource> paymentReportReader(
            @Value("#{jobParameters['paymentDate']}")LocalDate paymentDate
    ){
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
    public ItemProcessor<PaymentSource, Payment> itemProcessor(){
        /**
         * 최종 결제 금액이 0원 -> payment -> X
         */
        return paymentSource -> {
            if(paymentSource.getFinalAmount().compareTo(BigDecimal.ZERO) == 0){
                return null; //wirter에 전달 X
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
     * @return
     */
    @Bean
    public JpaItemWriter<Payment> paymentJpaItemWriter(){
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
