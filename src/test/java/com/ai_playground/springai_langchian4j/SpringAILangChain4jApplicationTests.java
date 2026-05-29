package com.ai_playground.springai_langchian4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ai_playground.springai_langchian4j.config.PgVectorTestConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(PgVectorTestConfiguration.class)
class SpringAILangChain4jApplicationTests {

	@Test
	void contextLoads() {
	}

}
