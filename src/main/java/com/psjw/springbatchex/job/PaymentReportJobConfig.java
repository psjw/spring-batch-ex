package com.psjw.springbatchex.job;

import com.psjw.springbatchex.service.PartnerCorporationService;
import com.psjw.springbatchex.entity.Payment;
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
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@AllArgsConstructor
public class PaymentReportJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PartnerCorporationService partnerCorporationService;
    private final int chunkSize = 10;

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
//            JpaPagingItemReader<PaymentSource> limitOffsetItemReader,
            NoOffsetItemReader<PaymentSource> noOffsetItemReader,
            ItemProcessor<PaymentSource, Payment> paymentReportProcessor,
            ItemWriter<Payment> paymentItemWriter
    ) {
        /**
         * 1. payment_source 테이블 및 인덱스 생성
         * 2. payment_source 데이터 프로시저로 셋업, 2025-05-02 이전 데이터 셋업, 2025-05-02 데이터 셋업
         * 3. Step 소요시간, Chunk 소요시간을 측정하는 리스너 작성
         */
        return new StepBuilder("paymentReportStep", jobRepository)
                .<PaymentSource, Payment>chunk(chunkSize, transactionManager)
//                .reader(limitOffsetItemReader)
                .reader(noOffsetItemReader)
                .processor(paymentReportProcessor)
                .writer(paymentItemWriter)
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

//    @Bean
//    @StepScope //@Value로 받으려면
//    public JpaPagingItemReader<PaymentSource> limitOffsetItemReader(
//            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
//    ) {
//        return new JpaPagingItemReaderBuilder<PaymentSource>()
//                .name("paymentReportReader")
//                .entityManagerFactory(entityManagerFactory)
//                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
//                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
//                .pageSize(chunkSize)
//                .build();
//    }

    @Bean
    @StepScope
    public NoOffsetItemReader<PaymentSource> noOffsetItemReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ) {
        return new NoOffsetItemReaderBuilder<PaymentSource>()
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate ORDER BY ps.id DESC")
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                .name("noOffsetItemReader")
                .idExtractor(PaymentSource::getId)
                .targetType(PaymentSource.class)
                .chunkSize(chunkSize)
                .build();

    }

    @Bean
    public ItemProcessor<PaymentSource, Payment> itemProcessor() {
        return paymentSource -> new Payment(
                null,
                paymentSource.getFinalAmount(),
                paymentSource.getPaymentDate(),
                "partnerCorpName",
                "PAYMENT"
        );
    }

    @Bean
    public ItemWriter<Payment> paymentReportWriter() {
        return chunk -> {
//            for (Payment payment : chunk) {
//                log.info("Writer payment : {}", payment);
//            }
        };
    }
}
