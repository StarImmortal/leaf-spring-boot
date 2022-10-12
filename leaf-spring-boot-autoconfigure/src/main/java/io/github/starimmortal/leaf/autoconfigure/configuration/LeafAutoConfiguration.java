package io.github.starimmortal.leaf.autoconfigure.configuration;

import io.github.starimmortal.leaf.core.exception.InitException;
import io.github.starimmortal.leaf.core.service.SegmentService;
import io.github.starimmortal.leaf.core.service.SnowflakeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 */
@Configuration
@ConditionalOnClass(LeafProperties.class)
@EnableConfigurationProperties(LeafProperties.class)
@Slf4j
public class LeafAutoConfiguration {

    @Autowired
    private LeafProperties properties;

    @Bean
    public SegmentService initLeafSegmentStarter() throws Exception {
        if (properties != null && properties.getSegment() != null && properties.getSegment().isEnable()) {
            return new SegmentService(properties.getSegment().getDriverClassName(), properties.getSegment().getUrl(), properties.getSegment().getUsername(), properties.getSegment().getPassword());
        }
        log.warn("init leaf segment ignore properties is {}", properties);
        return null;
    }

    @Bean
    public SnowflakeService initLeafSnowflakeStarter() throws InitException {
        if (properties != null && properties.getSnowflake() != null && properties.getSnowflake().isEnable()) {
            return new SnowflakeService(properties.getSnowflake().getAddress(), properties.getSnowflake().getPort());
        }
        log.warn("init leaf snowflake ignore properties is {}", properties);
        return null;
    }
}
