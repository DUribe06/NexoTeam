package com.grupo3.nexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NexoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexoBackendApplication.class, args);
	}

}
