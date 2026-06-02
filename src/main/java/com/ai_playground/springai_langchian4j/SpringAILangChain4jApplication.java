package com.ai_playground.springai_langchian4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point for the demo application.
 *
 * <p>Spring Boot scans this package and its children for controllers, services,
 * configuration, and other beans that make up the Spring AI playground.</p>
 */
@SpringBootApplication
public class SpringAILangChain4jApplication {

	/**
	 * Starts the application using Spring Boot's auto-configuration and component
	 * scanning.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringAILangChain4jApplication.class, args);
	}

}
