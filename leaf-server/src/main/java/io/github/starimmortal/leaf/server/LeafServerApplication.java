package io.github.starimmortal.leaf.server;

import io.github.starimmortal.leaf.autoconfigure.annotation.EnableLeafServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableLeafServer
@SpringBootApplication
public class LeafServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeafServerApplication.class, args);
    }
}
