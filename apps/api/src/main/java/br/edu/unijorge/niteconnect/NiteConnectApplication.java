package br.edu.unijorge.niteconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NiteConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(NiteConnectApplication.class, args);
    }
}
