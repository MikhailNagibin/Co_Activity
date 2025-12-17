package com.coactivity;

import org.springframework.boot.SpringApplication;

public class TestCoActivityApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoActivityApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
