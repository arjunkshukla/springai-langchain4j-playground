package com.ai_playground.springai_langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point for the Spring AI and LangChain4j playground.
 */
@SpringBootApplication
public class SpringAILangChain4jApplication {

	/**
	 * Starts the embedded Spring Boot application and initializes all configured beans.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringAILangChain4jApplication.class, args);
	}

}
