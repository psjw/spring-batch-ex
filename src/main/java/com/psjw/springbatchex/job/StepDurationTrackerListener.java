package com.psjw.springbatchex.job;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class StepDurationTrackerListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(">>> Step 시작: {} (job={}, 시작 시각={})", stepExecution.getStepName(),
                stepExecution.getJobExecution().getJobInstance().getJobName(),
                stepExecution.getStartTime());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        final LocalDateTime startTime = stepExecution.getStartTime();
        final LocalDateTime endTime = stepExecution.getEndTime();

        final long durationMillis = Duration.between(startTime, endTime).toMillis();

        // 80분 -> 1시간 20분 , 45 -> 45분
        final long hours = durationMillis / (1_000 * 60 * 60);
        final long minutes = (durationMillis % (1_000 * 60 * 60)) / (1_000 * 60);
        final long seconds = (durationMillis % (1_000 * 60)) / 1_000;
        final long millis = durationMillis % 1_000;

        String duration;
        if (hours > 0) {
            duration = String.format("%d시간 %d분", hours, minutes);
        } else if (minutes > 0) {
            duration = String.format("%d분", minutes);
        } else if (seconds > 0) {
            duration = String.format("%d초", seconds);
        } else {
            duration = String.format("%dms", millis);
        }

        log.info(
                ">>> Step 종료: {}, 상태={}, 읽음={}건, 처리={}건, 기록={}건, 스킵={}건, 소요시간={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getProcessSkipCount() + stepExecution.getWriteCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                duration
        );
        return stepExecution.getExitStatus();
    }
}
