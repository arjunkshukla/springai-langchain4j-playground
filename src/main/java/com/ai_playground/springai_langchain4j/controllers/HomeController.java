package com.ai_playground.springai_langchain4j.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards the root URL to the static chat UI.
 */
@Controller
public class HomeController {

	/**
	 * Serves the single-page UI from the static resources folder.
	 */
	@GetMapping("/")
	public String index() {
		return "forward:/index.html";
	}
}
