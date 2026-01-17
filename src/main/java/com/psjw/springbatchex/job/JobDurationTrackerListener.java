package com.psjw.springbatchex.job;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

@Slf4j
public class JobDurationTrackerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        final JobParameters jobParameters = jobExecution.getJobParameters();
        log.info(">>> Job 시작 시간: {} (시작 시각: {})",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStartTime()
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        final LocalDateTime startTime = jobExecution.getStartTime();
        final LocalDateTime endTime = jobExecution.getEndTime();

        final long durationMillis = Duration.between(startTime, endTime).toMillis();

        // 80분 -> 1시간 20분 , 45 -> 45분
        final long hours = durationMillis / (1_000 * 60 * 60);
        final long minutes = (durationMillis % (1_000 * 60 * 60)) / (1_000 * 60);
        final long seconds = (durationMillis % (1_000 * 60)) / 1_000;

        String duration;
        if (hours > 0) {
            duration = String.format("%d시간 %d분", hours, minutes);
        } else if (minutes > 0) {
            duration = String.format("%d분", minutes);
        } else {
            duration = String.format("%d초", seconds);
        }

        log.info(">>> Job 종료: 상태={} 소요시간={} 종료시각={}",
                jobExecution.getStatus(),
                duration,
                jobExecution.getEndTime()
        );

        if(jobExecution.getStatus().isUnsuccessful()){
            log.error(">>> Job 실패 원인: {}", jobExecution.getAllFailureExceptions());
        }
    }
}
