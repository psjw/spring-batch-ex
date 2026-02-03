package com.psjw.springbatchex.job;

import com.psjw.springbatchex.service.PartnerCorporationService;
import com.psjw.springbatchex.entity.Payment;
import com.psjw.springbatchex.entity.PaymentSource;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.Collections;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.HibernateHints;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
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
//            NoOffsetItemReader<PaymentSource> noOffsetItemReader,
            JpaCursorItemReader<PaymentSource> jpaCursorItemReader,
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
                .reader(jpaCursorItemReader)
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

//    @Bean
//    @StepScope
//    public NoOffsetItemReader<PaymentSource> noOffsetItemReader(
//            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
//    ) {
//        return new NoOffsetItemReaderBuilder<PaymentSource>()
//                .entityManagerFactory(entityManagerFactory)
//                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate ORDER BY ps.id DESC")
//                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
//                .name("noOffsetItemReader")
//                .idExtractor(PaymentSource::getId)
//                .targetType(PaymentSource.class)
//                .chunkSize(chunkSize)
//                .build();
//
//    }

    /**
     * JpaCursorItemReader를 사용하여 특정 결제일(`paymentDate`)의 결제 원천 데이터를 조회합니다.
     * <p>
     * - `paymentDate`는 JobParameter로 전달받습니다.
     * - Cursor 기반으로 동작하여 대용량 데이터를 처리할 때 메모리 사용량을 최소화할 수 있습니다.
     * - `HINT_FETCH_SIZE`를 `Integer.MIN_VALUE`로 설정하여 ResultSet에서 한 번에 한 행씩 가져오도록 DB에 힌트를 줍니다. (스트리밍)
     *
     * @param paymentDate JobParameter로 전달되는 결제일
     * @return JpaCursorItemReader<PaymentSource>
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<PaymentSource> cursorItemReader(
            @Value("#{jobParameters['paymentDate']}") LocalDate paymentDate
    ){
        return new JpaCursorItemReaderBuilder<PaymentSource>()
                .name("cursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                //client -> 전체 데이터 ->
                //server -> 하나씩 -> mysql
                // JPQL 쿼리 작성: SQL이 아닌 JPQL(Java Persistence Query Language)을 사용
                .queryString("SELECT ps FROM PaymentSource ps WHERE ps.paymentDate = :paymentDate")
                // 쿼리 파라미터 설정
                .parameterValues(Collections.singletonMap("paymentDate", paymentDate))
                // Hibernate Hint 설정: HINT_FETCH_SIZE
                // 이 옵션은 데이터베이스에서 데이터를 가져올 때
                // 서버 측 커서(Server-Side Cursor)를 사용할지 클라이언트 측 커서(Client-Side Cursor)를 사용할지 결정하는 중요한 역할을 합니다.
                // Integer.MIN_VALUE: 이 값을 설정하면 JDBC 드라이버에게 결과를 스트리밍 방식으로 받아오도록 요청합니다.
                // 즉, 데이터베이스 서버에 커서를 유지한 채로, 애플리케이션에서는 next()를 호출할 때마다 한 건씩 데이터를 가져옵니다.
                // 이를 통해 대용량의 데이터셋을 처리하더라도 애플리케이션의 메모리 사용량을 최소화할 수 있어 OOM(OutOfMemory) 에러를 방지합니다.
                // 만약 이 옵션이 없다면, JPA 구현체(Hibernate)는 기본적으로 조회된 모든 결과를 애플리케이션 메모리로 가져와서(Client-Side Cursor) 처리하려고 시도할 수 있습니다.
                .hintValues(Collections.singletonMap(HibernateHints.HINT_FETCH_SIZE, Integer.MIN_VALUE))
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
