package com.psjw.springbatchex.job;

import com.psjw.springbatchex.service.PartnerCorporationService;
import com.psjw.springbatchex.service.PartnerHttpException;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentReportJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PartnerCorporationService partnerCorporationService;

    /**
     * JpaPagingItemReader -> limit, offset기반의 sql 조회만들기
     */
    @Bean
    public Job paymentReportJob(
            Step paymentReportStep
    ) {
        return new JobBuilder("paymentReportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobDurationTrackerListener())
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
            ItemWriter<Payment> paymentItemWriter
    ) {
        /**
         * 1. 상호명을 더 이상 PaymentSource에서 관리하지 않음
         * 2. PaymentSource에서는 사업자 번호를 추가하고 이 사업자 번호 기반으로
         * 3. Payment를 저장할때 사업자 번호를 기준으로 HTTP 통신 하여 상호명 질의
         */
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(20, transactionManager)
                .reader(paymentReportReader)
                .processor(paymentReportProcessor)
                .writer(paymentItemWriter)
                .listener(new SampleChunkListener())
                .listener(new SampleItemReadListener())
                .listener(new SampleItemProcessListener())
                .listener(new SampleItemWriteListener())
                /**
                 * 1. chunk SampleChunkListener
                 * 2. reader SampleItemReadListener
                 * 3. processor SampleItemProcessListener
                 * 4. writer SampleItemWriteListener
                 */
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
        return paymentSource -> {
//            final String partnerCorpName = partnerCorporationService.getPartnerCorpName(
//                    paymentSource.getPartnerBusinessRegistrationNumber());
            return new Payment(
                    null,
                    paymentSource.getFinalAmount(),
                    paymentSource.getPaymentDate(),
                    "partnerCorpName",
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
    public ItemWriter<Payment> paymentReportWriter() {
        return chunk -> {
            for (Payment payment : chunk) {
                log.info("Writer payment : {}", payment);
            }
        };
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
