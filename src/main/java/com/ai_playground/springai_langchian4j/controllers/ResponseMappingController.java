package com.ai_playground.springai_langchian4j.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.dto.MeetingSummary;

@RestController
@RequestMapping("/response-mapping")
public class ResponseMappingController {

	private final ChatClient chatClient;

	public ResponseMappingController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@Value("classpath:meeting-transcript.txt")
	private Resource transcriptResource;

	/**
	 * BeanOutputConverter is a prompt-and-parse workflow, not a hard validation
	 * boundary. Spring AI documents that it injects the JSON schema as instructions
	 * into the prompt and then parses the model output afterward, so the model can
	 * still drift and produce unsupported values. We initially saw that with
	 * {@code URGENT} for {@code Priority} even though the schema only allowed
	 * {@code HIGH}, {@code MEDIUM}, and {@code LOW}.
	 * See: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
	 * Observed output:
	 * {
	 *   "mainTopic": "Improving the onboarding experience for new customers before the next quarterly release",
	 *   "actionItems": [
	 *     {"assignee":"Daniel","taskDescription":"Prepare a short report identifying the three most common pain points in account setup","dueDate":"Next Wednesday","priority":"HIGH"},
	 *     {"assignee":"Meera","taskDescription":"Draft a simpler welcome email sequence and coordinate with marketing","dueDate":"End of next week","priority":"MEDIUM"},
	 *     {"assignee":"Karthik","taskDescription":"Investigate the dashboard issue and suggest quick optimizations","dueDate":"18th","priority":"LOW"}
	 *   ]
	 * }
	 */
	@GetMapping("/bean-output-converter")
	public MeetingSummary beanOutputConverter() throws IOException {
		var converter = new BeanOutputConverter<MeetingSummary>(MeetingSummary.class);
		String format = converter.getFormat();

		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		String response = chatClient.prompt().user(u -> u.text("""
				Analyse the meeting transcript below.
				Extract the main topic and all action items.
				All priority values must be one of HIGH, MEDIUM, or LOW.
				{format}
				
				
				Transcript:
				{transcript}
				""").param("format", format).param("transcript", transcript)).call().content();

		return converter.convert(response);
	}
	
	/**
	 * beanOutputConverterV2 still uses Spring AI's structured-output plumbing under the
	 * hood. When {@code entity(MeetingSummary.class)} is invoked, Spring AI creates a
	 * {@link org.springframework.ai.converter.BeanOutputConverter} internally and puts
	 * its generated output format into the request context. So the schema guidance is
	 * still present, even though we are no longer injecting {@code {format}} manually
	 * into the user prompt.
	 *
	 * The important limitation is that this remains a prompt-and-parse workflow, not a
	 * hard validation boundary. The model is asked to produce JSON that matches the
	 * schema, but it can still drift, invent extra items, or stop before the root object
	 * is fully closed. That is exactly what happened in the failure we saw: the response
	 * contained a partially completed JSON document that ended after the
	 * {@code actionItems} array, and Jackson threw {@code JsonEOFException} because the
	 * closing brace for the root object never arrived.
	 *
	 * This is why the console log from {@code BeanOutputConverter} should be read as the
	 * raw model response that failed parsing, not as evidence that the schema was not
	 * sent. For a more reliable path, use native structured output.
	 *
	 * See: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
	 */
	@GetMapping("/bean-output-converter-v2") 
	public MeetingSummary beanOutputConverterV2() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);
		// NOT RELIABLE: Spring AI injects the format internally, but the model still has
		// to return one complete JSON object on its own and Jackson only parses after.
		// Approach 1 of passing the format directly into the prompt is more reliable.
		MeetingSummary response = chatClient.prompt().user(u -> u.text("""
				Analyse the meeting transcript below.
				Extract the main topic and all action items.
				All priority values must be one of HIGH, MEDIUM, or LOW.
				
				Transcript:
				{transcript}
				""").param("transcript", transcript)).call().entity(MeetingSummary.class);

		return response;
	}
	
	/**
	 * Native structured output is the better long-term path here because the schema is
	 * sent through the model's structured-output capability rather than only being
	 * described in plain text. Spring AI's docs call out this approach as more reliable
	 * and better aligned with the model. Keep this as the preferred example for future
	 * reference.
	 * See: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
	 * Observed output:
	 * {
	 *   "mainTopic": "Improving onboarding experience for new customers",
	 *   "actionItems": [
	 *     {"assignee":"Daniel","taskDescription":"Identify three most common pain points and prepare a report for product decisions","dueDate":"Next Wednesday (urgent)","priority":"MEDIUM"},
	 *     {"assignee":"Meera","taskDescription":"Draft a simplified version of the welcome email sequence and coordinate with marketing","dueDate":"End of next week","priority":"MEDIUM"},
	 *     {"assignee":"Karthik","taskDescription":"Investigate dashboard issue, suggest quick optimizations, and provide findings before the 18th","dueDate":"Before 18th","priority":"LOW"}
	 *   ]
	 * }
	 */
	@GetMapping("/native-structured-output")
	public MeetingSummary nativeStructuredOutput() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		return chatClient.prompt()
				.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
				.user(u -> u.text("""
						Analyse the meeting transcript below.
						Extract the main topic and all action items.

						Transcript:
						{transcript}
						""").param("transcript", transcript))
				.call()
				.entity(MeetingSummary.class);
	}
	
	@GetMapping("/list-output-converter")
	public List<String> listOutputConverter() throws IOException {
		var converter = new ListOutputConverter(new DefaultConversionService());
		String format = converter.getFormat();

		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		String response = chatClient.prompt().user(u -> u.text("""
				List 5 funny names for the team on the basic of following meeting transcript.
				{format}
				
				
				Transcript:
				{transcript}
				""").param("format", format).param("transcript", transcript)).call().content();

		return converter.convert(response);
	}
	
	/**
	 * This is the most explicit prompt-based list example: we inject the list output
	 * format into the prompt, let the model answer in plain text, and then parse that
	 * text back into a {@link List}. It is simple and easy to inspect, but it is still
	 * a prompt-and-parse flow, so the model can return extra prose, malformed items, or
	 * a list that needs cleanup before conversion.
	 */
	@GetMapping("/list-output-converter-v2")
	public List<String> listOutputConverterV2() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		List<String> response = chatClient.prompt().user(u -> u.text("""
				List 5 funny names for the team on the basic of following meeting transcript.				
				
				Transcript:
				{transcript}
				""").param("transcript", transcript)).call().entity(new ParameterizedTypeReference<List<String>>() {});

		return response;
	}
	
	/**
	 * Native structured output is the strongest contract Spring AI offers here, but
	 * this particular {@code List<String>} shape has been brittle in practice with the
	 * current model. The schema only says "array of strings", so the model does not get
	 * much guidance about what each item should contain, and in our runs it often came
	 * back as an empty list instead of useful names. This is why the native approach is
	 * more reliable for object-shaped DTOs than for a very open-ended free-form list.
	 * See: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
	 */
	@GetMapping("/native-structured-output-list")
	public List<String> nativeStructuredOutputList() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		// In this setup the native list path has been the least helpful one: the schema
		// is valid, but the model often chooses to return an empty array for this very
		// open-ended List<String> request.
		List<String> response = chatClient.prompt()
				.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)// DOES NOT WORK FOR LIST AND MAPS ONLY WORKS FOR OBJECTS.
				.user(u -> u.text("""
						List 5 funny names for the team on the basic of following meeting transcript.

						Transcript:
						{transcript}
						""").param("transcript", transcript))
				.call()
				.entity(new ParameterizedTypeReference<List<String>>() {});
		
		return response;// returns empty list. not reliable. 
	}
	
	/**
	 * Prompt-and-parse version: the JSON shape is injected into the prompt through
	 * {@code {format}}, then the response is parsed afterward. This is easy to read,
	 * but it is still soft guidance, so the model can omit an assignee, invent a key,
	 * or return extra prose that needs cleanup.
	 */
	@GetMapping("/map-output-converter")
	public Map<String, Object> mapOutputConverter() throws IOException {
		var converter = new MapOutputConverter();
		String format = converter.getFormat();

		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		String response = chatClient.prompt().user(u -> u.text("""
				Extract and map every assignee against their task decription from the below meeting transcript. Do not omit any assignee.
				{format}
				
				Transcript:
				{transcript}
				""").param("format", format).param("transcript", transcript)).call().content();

		return converter.convert(response);
	}
	
	/**
	 * Slightly cleaner prompt flow: Spring AI injects the map format internally when
	 * {@code entity(Map<String, Object>)} is used, so the prompt text stays shorter.
	 * The tradeoff is the same though: this is still prompt-and-parse, not hard
	 * validation, so the model can still return an incomplete map or miss one assignee
	 * even when the transcript clearly contains it.
	 */
	@GetMapping("/map-output-converter-v2")
	public Map<String, Object> mapOutputConverterV2() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		Map<String, Object> response = chatClient.prompt().user(u -> u.text("""
				Extract and map every assignee against their task decription from the below meeting transcript. Do not omit any assignee.

				Transcript:
				{transcript}
				""").param("transcript", transcript)).call().entity(new ParameterizedTypeReference<Map<String, Object>>() {});

		return response;
	}
	
	/**
	 * Native structured output is the strongest contract Spring AI offers here, but
	 * this particular {@code Map<String, Object>} shape has been brittle in practice with the
	 * current model. The schema only says "map of strings and object", so the model does not get
	 * much guidance about what each item should contain, and in our runs it often came
	 * back as an empty map instead of useful key value map. This is why the native approach is
	 * more reliable for object-shaped DTOs than for a very open-ended free-form list.
	 * See: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
	 */
	@GetMapping("/native-structured-output-map")
	public Map<String, Object> nativeStructuredOutputMap() throws IOException {
		String transcript = Files.readString(transcriptResource.getFile().toPath(), StandardCharsets.UTF_8);

		// In this setup the native map path has been the least helpful one: the schema
		// is valid, but the model often chooses to return an empty map for this very
		// open-ended Map<String, Object> request.
		Map<String, Object> response = chatClient.prompt().advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
				.user(u -> u
						.text("""
								Extract and map every assignee against their task decription from the below meeting transcript. Do not omit any assignee.

								Transcript:
								{transcript}
								""")
						.param("transcript", transcript))
				.call().entity(new ParameterizedTypeReference<Map<String, Object>>() {
				});

		return response;// returns empty map. not reliable. 
	}
}
