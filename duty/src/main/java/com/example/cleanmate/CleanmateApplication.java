package com.example.cleanmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CleanmateApplication {

	public static void main(String[] args) {
		SpringApplication.run(CleanmateApplication.class, args);
	}

}
