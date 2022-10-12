package io.github.starimmortal.leaf.autoconfigure.annotation;

import io.github.starimmortal.leaf.autoconfigure.configuration.LeafAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LeafAutoConfiguration.class)
@Inherited
public @interface EnableLeafServer {
}
