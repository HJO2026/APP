package com.example.HJO;

import org.springframework.boot.SpringApplication;

public class TestHjoApplication {

	public static void main(String[] args) {
		SpringApplication.from(HjoApplication::main).with(TestcontainersConfiguration.class).withAdditionalProfiles("test").run(args);
	}

}
