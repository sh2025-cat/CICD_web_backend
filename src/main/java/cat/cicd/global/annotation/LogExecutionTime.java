package cat.cicd.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메소드에 추가하여 메소드의 실행 시간을 로그에 남기는 어노테이션 메트릭(Metric)으로 수집하여 대시보드에 분석하기 위해서는 Micrometer에서 제공하는 @Timed 을 사용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
}