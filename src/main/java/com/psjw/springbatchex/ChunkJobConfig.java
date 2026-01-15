package com.psjw.springbatchex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ChunkJobConfig {

    @Bean
    public Job chunkJob(
            JobRepository jobRepository,
            Step chunkStep
    ) {
        return new JobBuilder("chunkJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(chunkStep)
                .build();
    }

    @Bean
    public Step chunkStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("chunkStep", jobRepository)
                .<Long, Long>chunk(10, transactionManager)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemReader<Long> itemReader() {
        return new ListItemReader<>(getItems());
    }

    private ItemWriter<Long> itemWriter() {
        return items -> {
            items.forEach(item -> System.out.println("itemWriter" + item));
        };
    }

    public ItemProcessor<Long, Long> itemProcessor() {
        return item -> {
            // filter, item 10이면 null(제외) 리턴
            if (item == 10L) {
                return null;
            }
            return item;
        };
    }

    /**
     * COMMIT_COUNT, FILTER_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT
     * 11,101,101
     *
     * total count = 101
     * chunk size = 10
     *
     * 101 / 10 = 10.1
     * 10 * 10 = 100
     * @return
     */
    private List<Long> getItems(){
        return LongStream.rangeClosed(1, 101)
                .boxed()
                .toList();
    }

}
