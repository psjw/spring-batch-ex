package com.psjw.springbatchex.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

@Slf4j
public class SampleChunkListener implements ChunkListener {

    @Override
    public void beforeChunk(ChunkContext context) {
        log.info("sample - 1 SampleChunkListener beforeChunk");
    }

    @Override
    public void afterChunk(ChunkContext context) {
        log.info("sample - 1 SampleChunkListener afterChunk");
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        log.info("sample - 1 SampleChunkListener afterChunkError");
    }
}
