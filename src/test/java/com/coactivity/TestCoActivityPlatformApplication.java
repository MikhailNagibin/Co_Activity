package com.coactivity;

import org.springframework.boot.SpringApplication;

public class TestCoActivityPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoActivityPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
