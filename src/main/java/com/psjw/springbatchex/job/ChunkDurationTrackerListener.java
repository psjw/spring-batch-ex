package com.psjw.springbatchex.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

@Slf4j
public class ChunkDurationTrackerListener implements ChunkListener {

    @Override
    public void beforeChunk(ChunkContext context) {
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long startTime = (long) context.getAttribute("startTime");
        long endTime = System.currentTimeMillis();

        long durationMillis = endTime - startTime;

        //처리가 완료된 청크 번호를 가져옴
        long commitNumber = context.getStepContext().getStepExecution().getCommitCount();

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

        log.info("Chunck #{}, Duration: {}", commitNumber, duration);
    }

}
