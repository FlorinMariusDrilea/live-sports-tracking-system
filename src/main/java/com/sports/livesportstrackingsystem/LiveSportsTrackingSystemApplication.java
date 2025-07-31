package com.sports.livesportstrackingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LiveSportsTrackingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveSportsTrackingSystemApplication.class, args);
	}

}
