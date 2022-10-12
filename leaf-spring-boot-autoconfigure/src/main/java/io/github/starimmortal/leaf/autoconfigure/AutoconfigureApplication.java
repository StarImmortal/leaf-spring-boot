package io.github.starimmortal.leaf.autoconfigure;

import io.github.starimmortal.leaf.autoconfigure.annotation.EnableLeafServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableLeafServer
public class AutoconfigureApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoconfigureApplication.class, args);
    }
}
