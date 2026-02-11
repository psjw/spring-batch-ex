package com.psjw.springbatchex.job;

import com.psjw.springbatchex.entity.Grade;
import com.psjw.springbatchex.entity.User;
import com.psjw.springbatchex.service.OrderClient;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@AllArgsConstructor
public class UserGradeApplyJobConfiguration {


    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderClient orderClient;
    private final int chunkSize = 1_000;

    @Bean
    public Job userGradleApplyJob(
            Step userGradleApplyStep
    ) {
        return new JobBuilder("userGradleApplyJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(userGradleApplyStep)
                .build();
    }

    @Bean
    public Step userGradleApplyStep(
            JpaCursorItemReader<User> cursorItemReader,
            ItemWriter<User> writer
    ) {
        return new StepBuilder("userGradleApplyStep", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                // Step 소요 시간 측정
                .listener(new StepDurationTrackerListener())
                .reader(cursorItemReader)
                .writer(writer)
                .listener(new ChunkDurationTrackerListener())
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<User> cursorItemReader() {
        return new JpaCursorItemReaderBuilder<User>()
                .name("cursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT u FROM User u WHERE u.grade = 'INIT'")
                .build();
    }

    /**
     * Chunk #1 Duration :2m24s
     * Chunk #1, Duration: 19초
     * 이론적으로 처리되는 속도는 Chunk #1 15,000ms
     *
     * 기존 방식 : 155,880ms
     * 병렬처리 방식 : 18,556 ms
     *
     * 88% 성능향상
     *
     */

    /**
     * rows = 1,000
     * Step 처리 시간: 2m34s424ms -> 152,000ms
     * user grade api 응답 : 150ms -> 1,000 * 150 = 150,000
     */

//    @Bean
//    public JpaItemWriter<User> writer() {
//        // JpaItemWriter 으로 update 반영
//        return new JpaItemWriterBuilder<User>()
//                .entityManagerFactory(entityManagerFactory)
//                .build();
//    }
    @Bean
    public ItemWriter<User> writer() {
        return chunk -> {
            var appliedUserGrades = Flowable.fromIterable(chunk.getItems())
                    /**
                     * [1, 2, 3, 4, 5, 6, 7, 8, 9,  ... 1,000]
                     * 동시에 진행 -> 레일 1번 -> 1, 2, 3 ... 100 -> 15,000ms
                     * 동시에 진행 -> 레일 2번 -> 1, 2, 3 ... 100 -> 15,000ms
                     * 동시에 진행 -> 레일 3번 -> 1, 2, 3 ... 100- > 15,000ms
                     * ...
                     * 동시에 진행 -> 레일 10번 -> 1, 2, 3 ... 100- > 15,000ms
                     */
                    .parallel()
                    .runOn(Schedulers.io())
                    .map(user -> {
                        Grade grade = orderClient.getGrade(user.getId());
                        user.setGrade(grade);
                        return user;
                    })
                    /**
                     * 1, 2, 3, 4, 5, 6, 7, 8, 9
                     * 레일 1번 -> 1, 2, 3 -> 100ms
                     * 레일 2번 -> 4, 5, 6 -> 100ms
                     * 레일 3번 -> 7, 8, 9 -> 150ms
                     *
                     * 레일 병합
                     * [1, 2, 3], [4, 5, 6], [7, 8, 9]
                     */
                    .sequential()
                    .toList()
                    .blockingGet();

            new JpaItemWriterBuilder<User>()
                    .entityManagerFactory(entityManagerFactory)
                    .build()
                    .write(new Chunk<>());

        };
    }
}
