package cat.cicd.global.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class LogTimeAspect {

	@Around("@annotation(cat.cicd.global.annotation.LogExecutionTime)")
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		Object result = joinPoint.proceed();

		stopWatch.stop();
		long totalTimeMillis = stopWatch.getTotalTimeMillis();

		String methodName = joinPoint.getSignature().getName();
		log.info("[ExecutionTime] {}: {} ms", methodName, totalTimeMillis);

		return result;
	}
}