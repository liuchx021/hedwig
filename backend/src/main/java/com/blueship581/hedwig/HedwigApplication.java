package com.blueship581.hedwig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class HedwigApplication {

    public static void main(String[] args) {
        String timezone = System.getenv().getOrDefault("TZ", "Asia/Shanghai");
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        System.setProperty("user.timezone", timezone);
        SpringApplication.run(HedwigApplication.class, args);
    }
}
