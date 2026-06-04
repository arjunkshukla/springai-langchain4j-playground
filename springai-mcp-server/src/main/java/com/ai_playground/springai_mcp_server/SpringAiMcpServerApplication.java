package com.ai_playground.springai_mcp_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the standalone Spring AI MCP resource server.
 */
@SpringBootApplication
public class SpringAiMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiMcpServerApplication.class, args);
	}
}
