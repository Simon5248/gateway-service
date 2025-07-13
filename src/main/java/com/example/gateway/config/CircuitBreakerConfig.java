package com.example.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * 斷路器配置類
 * 用於處理服務間調用的故障轉移和服務降級
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 重試配置
     * 定義了網關重試請求的基本配置
     */
    @Bean
    public RetryGatewayFilterFactory.RetryConfig retryConfig() {
        return new RetryGatewayFilterFactory.RetryConfig();
    }

    /**
     * 斷路器默認配置
     * 使用 Resilience4J 作為斷路器實現
     * @return 斷路器自定義配置
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                // 配置斷路器參數
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        // 設置滑動窗口類型為基於時間，而不是基於計數
                        .slidingWindowType(SlidingWindowType.TIME_BASED)
                        // 滑動窗口大小為10秒
                        .slidingWindowSize(10)
                        // 失敗率達到50%時打開斷路器
                        .failureRateThreshold(50)
                        // 斷路器打開後等待10秒後進入半開狀態
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        // 半開狀態允許3次調用，根據這些調用的結果決定是否關閉斷路器
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                // 配置調用超時
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        // 設置超時時間為5秒
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .build());
    }
}
