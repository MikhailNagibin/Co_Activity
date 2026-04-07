package com.coactivity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
			return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	@SuppressWarnings("resource")
	GenericContainer<?> redisContainer() {
		GenericContainer<?> redisContainer =
				new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"));
		return redisContainer.withExposedPorts(6379);
	}

}
