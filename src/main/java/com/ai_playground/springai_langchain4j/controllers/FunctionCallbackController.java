package com.ai_playground.springai_langchain4j.controllers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchain4j.tools.DemoTools;

/**
 * Demonstrates dynamic Spring AI tools by creating {@link FunctionToolCallback} instances per request.
 */
@RestController
@RequestMapping("/function-callback")
public class FunctionCallbackController {

	private final ChatClient chatClient;

	private final DemoTools demoTools;

	/**
	 * Creates the controller with a plain chat client and reusable demo business logic.
	 */
	public FunctionCallbackController(@Qualifier("persistedChatClient") ChatClient chatClient, DemoTools demoTools) {
		this.chatClient = chatClient;
		this.demoTools = demoTools;
	}

	/**
	 * Builds the dynamic tool callback list and lets the model decide which callbacks to call.
	 */
	@GetMapping("/ask")
	public String ask(@RequestParam String question, @RequestParam String sessionId) {
		List<ToolCallback> toolCallbacks = new ArrayList<>();
		
		toolCallbacks.add(FunctionToolCallback
				.builder("getOTP", (str) -> {
					System.out.println("Generating OTP...");
					return "Your OTP is: " + (int)(Math.random() * 900000 + 100000);
				})
				.description("Get OTP.").inputType(Void.class)
				.build());
		toolCallbacks.add(FunctionToolCallback
				.builder("transferAmount", (BankTransferRequest entry) -> {
					if(entry.amount() == null) {
						return "Please ask user to enter a valid amount and retry again.";
					} else if(entry.payee() == null || entry.payee().isEmpty()) {
						return "Please ask user to enter a valid payee and retry again.";
					} else if(entry.confirmationId() == null || entry.confirmationId().isEmpty()) {
						return "Please ask user to enter a valid confirmation ID and retry again.";
					}
					return "Transferred " + entry.amount().stripTrailingZeros().toPlainString() + " to " + entry.payee() + " successful.";
				})
				.description("Initiate transfer of an amount to a payee.").inputType(BankTransferRequest.class)
				.build());
		toolCallbacks.add(weatherToolCallback());
		toolCallbacks.add(exchangeRateToolCallback());
		toolCallbacks.add(addToCartToolCallback());
		toolCallbacks.add(bookFlightToolCallback());
		toolCallbacks.add(calculatorToolCallback());
		

		return chatClient.prompt().system("""
				You are a helpful assistant.
				Use the provided tools when they are relevant to the user's request.
				Treat tool results as authoritative and preserve their factual values.
				If the user asks multiple questions, answer every part of the request.
				Do not mention tool names unless the user asks.
				""").user(question).toolCallbacks(toolCallbacks).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId)).call().content();
	}

	/**
	 * Creates a request-time weather callback backed by {@link DemoTools#getWeather(String)}.
	 */
	private ToolCallback weatherToolCallback() {
		System.out.println("Creating weather tool callback...");
		return FunctionToolCallback
				.builder("getCurrentWeather", (WeatherRequest request) -> demoTools.getWeather(request.location()))
				.description("Get the current weather for a specific city or location.").inputType(WeatherRequest.class)
				.build();
	}

	/**
	 * Creates a request-time currency exchange callback backed by {@link DemoTools#getExchangeRate(String, String)}.
	 */
	private ToolCallback exchangeRateToolCallback() {
		System.out.println("Creating exchange rate tool callback...");
		return FunctionToolCallback
				.builder("getExchangeRate",
						(ExchangeRateRequest request) -> demoTools.getExchangeRate(request.fromCurrency(),
								request.toCurrency()))
				.description("Get the current exchange rate between two currencies.")
				.inputType(ExchangeRateRequest.class).build();
	}

	/**
	 * Creates a request-time shopping cart callback backed by {@link DemoTools#addToCart(String, String, int)}.
	 */
	private ToolCallback addToCartToolCallback() {
		System.out.println("Creating add to cart tool callback...");
		return FunctionToolCallback
				.builder("addToCart",
						(AddToCartRequest request) -> demoTools.addToCart(request.cartId(), request.productCode(),
								request.quantity()))
				.description("Add an item to the shopping cart.").inputType(AddToCartRequest.class).build();
	}

	/**
	 * Creates a request-time flight booking callback backed by {@link DemoTools#bookFlight(String, String, String)}.
	 */
	private ToolCallback bookFlightToolCallback() {
		System.out.println("Creating book flight tool callback...");
		return FunctionToolCallback
				.builder("bookFlight",
						(BookFlightRequest request) -> demoTools.bookFlight(request.from(), request.to(),
								request.date()))
				.description("Book a flight for a specific route and date.").inputType(BookFlightRequest.class).build();
	}

	/**
	 * Creates a request-time calculator callback for simple arithmetic requests.
	 */
	private ToolCallback calculatorToolCallback() {
		System.out.println("Creating calculator tool callback...");
		return FunctionToolCallback
				.builder("calculate",
						(ArithmeticRequest request) -> calculate(request.left(), request.operator(), request.right()))
				.description(
						"Calculate simple arithmetic. The operator must be one of +, -, *, /, add, subtract, multiply, or divide.")
				.inputType(ArithmeticRequest.class).build();
	}

	/**
	 * Performs basic arithmetic for the dynamic calculator tool.
	 */
	private String calculate(BigDecimal left, String operator, BigDecimal right) {
		if (left == null || operator == null || right == null) {
			return "Please provide valid left, operator, and right values.";
		}

		String normalizedOperator = operator.trim().toLowerCase(Locale.ROOT);
		BigDecimal result = switch (normalizedOperator) {
		case "+", "add", "plus" -> left.add(right);
		case "-", "subtract", "minus" -> left.subtract(right);
		case "*", "multiply", "times" -> left.multiply(right);
		case "/", "divide" -> right.compareTo(BigDecimal.ZERO) == 0 ? null : left.divide(right, MathContext.DECIMAL64);
		default -> null;
		};

		if (result == null) {
			return "Unable to calculate " + left + " " + operator + " " + right + ".";
		}

		return left.stripTrailingZeros().toPlainString() + " " + operator + " "
				+ right.stripTrailingZeros().toPlainString() + " = " + result.stripTrailingZeros().toPlainString()
				+ ".";
	}

	/**
	 * Input schema for the dynamic weather callback.
	 */
	private record WeatherRequest(String location) {
	}

	/**
	 * Input schema for the dynamic exchange-rate callback.
	 */
	private record ExchangeRateRequest(String fromCurrency, String toCurrency) {
	}

	/**
	 * Input schema for the dynamic add-to-cart callback.
	 */
	private record AddToCartRequest(String cartId, String productCode, int quantity) {
	}

	/**
	 * Input schema for the dynamic book-flight callback.
	 */
	private record BookFlightRequest(String from, String to, String date) {
	}

	/**
	 * Input schema for the dynamic calculator callback.
	 */
	private record ArithmeticRequest(BigDecimal left, String operator, BigDecimal right) {
	}
	
	/**
	 * Input schema for the dynamic calculator callback.
	 */
	private record BankTransferRequest(BigDecimal amount, String payee, String confirmationId) {
	}
}
