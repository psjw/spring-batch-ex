package com.psjw.springbatchex.job;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link NoOffsetItemReader}를 생성하는 빌더 클래스입니다.
 * No-Offset 기반의 페이징 처리를 위한 ItemReader를 손쉽게 설정하고 생성할 수 있도록 돕습니다.
 *
 * @param <T> 읽어올 아이템의 타입
 */
public class NoOffsetItemReaderBuilder<T>  {

    private EntityManagerFactory entityManagerFactory;
    private String queryString;
    private Map<String, Object> parameterValues = new HashMap<>();
    private int chunkSize; //chunk size
    private Function<T, Long> idExtractor; //조회된 Entity에서 ID를 추출하는 함수
    private String name;
    private Class<T> targetType; //조회할 엔티티 클래스 타입

    /**
     * ItemReader의 이름을 설정합니다. Spring Batch 메타데이터에 저장될 이름입니다.
     * @param name ItemReader의 이름
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * JPA EntityManagerFactory를 설정합니다.
     * @param entityManagerFactory EntityManagerFactory 인스턴스
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> entityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        return this;
    }

    /**
     * 페이지 크기를 설정합니다. 한 번에 읽어올 아이템의 수입니다.
     * @param chunkSize 페이지 크기
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> chunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * 데이터를 조회할 JPQL 쿼리 문자열을 설정합니다.
     * @param queryString JPQL 쿼리
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> queryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    /**
     * 쿼리에 사용될 파라미터들을 설정합니다.
     * @param parameterValues 쿼리 파라미터 맵
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
        return this;
    }

    /**
     * 아이템에서 ID를 추출하는 함수를 설정합니다.
     * 이 ID는 다음 페이지 조회를 위한 'seek' 조건으로 사용됩니다.
     * @param idExtractor ID 추출 함수
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> idExtractor(Function<T, Long> idExtractor) {
        this.idExtractor = idExtractor;
        return this;
    }

    /**
     * 조회할 대상 엔티티의 클래스 타입을 설정합니다.
     * @param targetType 대상 엔티티 클래스
     * @return 빌더 인스턴스
     */
    public NoOffsetItemReaderBuilder<T> targetType(Class<T> targetType) {
        this.targetType = targetType;
        return this;
    }

    /**
     * 설정된 값들을 기반으로 {@link NoOffsetItemReader} 인스턴스를 생성합니다.
     * 필수 값이 설정되었는지 검증합니다.
     * @return NoOffsetItemReader 인스턴스
     */
    public NoOffsetItemReader<T> build() {
        Assert.notNull(entityManagerFactory, "EntityManagerFactory is required.");
        Assert.notNull(queryString, "Query string is required.");
        Assert.notNull(idExtractor, "ID extractor function is required.");
        Assert.notNull(targetType, "Target type is required.");
        Assert.notNull(name, "name is required.");
        Assert.state(chunkSize > 0, "chunkSize must be greater than 0.");

        return new NoOffsetItemReader<>(
                this.entityManagerFactory,
                this.queryString,
                this.parameterValues,
                this.chunkSize,
                this.idExtractor,
                this.targetType,
                this.name
        );
    }

}
