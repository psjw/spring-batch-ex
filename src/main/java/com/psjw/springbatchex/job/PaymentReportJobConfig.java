package com.psjw.springbatchex;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PaymentReportJobConfig {

    @Bean
    public Job paymentReportJob(
            JobRepository jobRepository,
            Step paymentReportStep
    ) {
        /**
         * 즉, 실행할 때마다 run.id 값이 1,2,3...이렇게 올라가면서
         * 동일한 파라미터에도 항상 새롭게 실행이 가능한 거죠.
         */
        return new JobBuilder("paymentReportJob", jobRepository)
                .incrementer(new RunIdIncrementer()) //동일 파라미터로 실행시키기위한 방법
                .start(paymentReportStep)
                .build();
    }

    @Bean
    public Step paymentReportStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<BigDecimal> paymentItemReader,
            ItemWriter<BigDecimal> paymentItemWriter) {
        // Job name + Job Parameter = 유니크를 확인
        // paymentReportJob = 첫 번째 Job 수행 paymentReportJob + X
        // paymentReportJob = 두 번째 Job 수행 paymentReportJob + X
        // 수행시키려면
        // paymentReportJob = 첫 번째 Job 수행 paymentReportJob + X
        // paymentReportJob = 두 번째 Job 수행 paymentReportJob + JobParameter = targetDate = 2025-05-01
        // paymentReportJob = 두 번째 Job 수행 paymentReportJob + JobParameter = targetDate = 2025-05-02
        return new StepBuilder("paymentReportStep", jobRepository)
                .<BigDecimal, BigDecimal>chunk(5,
                        transactionManager) // 1,000 -> chunk size =100 -> 10번 수행
                .reader(paymentItemReader)
                //.processor() //필수는 아님
                .writer(paymentItemWriter)
                .build();
    }


    @Bean
    @StepScope //있어야 파라미터 바인딩됨
    public ItemReader<BigDecimal> paymentItemReader(
            @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        System.out.println("Reader targetDate: " + targetDate);
        return new ListItemReader<>(getPayments());
    }

    @Bean
    @StepScope
    public ItemWriter<BigDecimal> paymentItemWriter(
            @Value("#{jobParameters['targetDate']}") String targetDate
    ) {
        System.out.println("Writer targetDate: " + targetDate);
        return items -> items.forEach(item -> System.out.println("Payment: "+ item));
    }

    private List<BigDecimal> getPayments(){
        return List.of(
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(700),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(900),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(1300),
                BigDecimal.valueOf(1400),
                BigDecimal.valueOf(1500)
        );
    }

}
