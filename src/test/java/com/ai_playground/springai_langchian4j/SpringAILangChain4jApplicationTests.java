package com.ai_playground.springai_langchian4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ai_playground.springai_langchian4j.config.PgVectorTestConfiguration;

/**
 * Verifies that the Spring context starts with the Testcontainers-backed pgvector
 * configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PgVectorTestConfiguration.class)
class SpringAILangChain4jApplicationTests {

	/**
	 * Smoke test that the application context can start successfully.
	 */
	@Test
	void contextLoads() {
	}

}
