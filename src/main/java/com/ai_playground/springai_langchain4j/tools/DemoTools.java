package com.ai_playground.springai_langchain4j.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Local demo tool implementation used by Spring AI static and dynamic tool examples.
 */
@Component
public class DemoTools {

	/**
	 * Returns a hardcoded weather response for a requested city.
	 */
	@Tool(description = "Get the current weather for a specific city or location.")
	public String getWeather(String location) {
		if (location == null || location.isEmpty()) {
			return "Please provide a valid location.";
		} else if (location.equalsIgnoreCase("New York")) {
			return "The current weather in New York is cloudy with a temperature of 20°C.";
		} else if (location.equalsIgnoreCase("Los Angeles")) {
			return "The current weather in Los Angeles is sunny with a temperature of 30°C.";
		} else if (location.equalsIgnoreCase("Sydney")) {
			return "The current weather in Sydney is purple snow with 123°C.";
		} else {
			return "The current weather in " + location + " is sunny with a temperature of 25°C.";
		}
	}
	
	/**
	 * Returns a hardcoded exchange-rate response for two currency codes.
	 */
	@Tool(description = "Get the current exchange rate between two currencies.")
	public String getExchangeRate(String fromCurrency, String toCurrency) {
		if (fromCurrency == null || toCurrency == null || fromCurrency.isEmpty() || toCurrency.isEmpty()) {
			return "Please provide valid currency codes.";
		} else if (fromCurrency.equalsIgnoreCase("USD") && toCurrency.equalsIgnoreCase("EUR")) {
			return "The current exchange rate from USD to EUR is 0.85.";
		} else if (fromCurrency.equalsIgnoreCase("EUR") && toCurrency.equalsIgnoreCase("USD")) {
			return "The current exchange rate from EUR to USD is 1.18.";
		} else if (fromCurrency.equalsIgnoreCase("USD") && toCurrency.equalsIgnoreCase("JPY")) {
			return "The current exchange rate from USD to JPY is 110.00.";
		} else if (fromCurrency.equalsIgnoreCase("JPY") && toCurrency.equalsIgnoreCase("USD")) {
			return "The current exchange rate from JPY to USD is 0.0091.";
		} else {
			return "The current exchange rate from " + fromCurrency.toUpperCase() + " to " + toCurrency.toUpperCase() + " is 1.00.";
		}
	}
	
	/**
	 * Simulates adding a product and quantity to a shopping cart.
	 */
	@Tool(description = "Add an item to the shopping cart.")
	public String addToCart(String cartId, String productCode, int quantity) {
		if (cartId == null || productCode == null || cartId.isEmpty() || productCode.isEmpty() || quantity <= 0) {
			return "Please provide valid cart ID, product code, and quantity.";
		} else {
			System.out.println("Adding " + quantity + " of product " + productCode.toUpperCase() + " to cart " + cartId.toUpperCase() + ".");
			return "Added " + quantity + " of product " + productCode.toUpperCase() + " to cart " + cartId.toUpperCase() + ".";
		}
	}
	
	/**
	 * Simulates booking a flight between two locations on a requested date.
	 */
	@Tool(description = "Book a flight for a specific route and date.")
	public String bookFlight(String from, String to, String date) {
		if (from == null || to == null || date == null || from.isEmpty() || to.isEmpty() || date.isEmpty()) {
			return "Please provide valid departure location, destination, and date.";
		} else {
			System.out.println("Booked a flight from " + from.toUpperCase() + " to " + to.toUpperCase() + " on " + date + ".");
			return "Booked a flight from " + from.toUpperCase() + " to " + to.toUpperCase() + " on " + date + ".";
		}
	}
}
