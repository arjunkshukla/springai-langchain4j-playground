package com.ai_playground.springai_langchian4j.lc4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * A project-local summarizing ChatMemory implementation.
 * <p>
 * LangChain4j in this branch gives us the window-based memory strategies out of the box,
 * but not a ready-made summary memory. This class fills that gap by:
 * <ul>
 *   <li>loading the current conversation from the persistent store,</li>
 *   <li>summarizing the oldest part of the conversation once the message budget is exceeded,</li>
 *   <li>persisting the compacted result back into the same store.</li>
 * </ul>
 * The goal is to preserve important context while keeping the working set small enough
 * for long conversations.
 */
public class MessageSummaryChatMemory implements ChatMemory {

	private static final String SUMMARY_PROMPT = """
			Summarize the following conversation for future chat memory.
			Keep names, decisions, preferences, open questions, and action items.
			Be concise, factual, and do not invent information that is not present.
			
			Conversation:
			%s
			""";

	private final Object id;
	private final int maxMessages;
	private final ChatModel summarizerModel;
	private final ChatMemoryStore chatMemoryStore;

	public MessageSummaryChatMemory(Object id, int maxMessages, ChatModel summarizerModel, ChatMemoryStore chatMemoryStore) {
		this.id = Objects.requireNonNull(id, "id");
		this.maxMessages = maxMessages;
		this.summarizerModel = Objects.requireNonNull(summarizerModel, "summarizerModel");
		this.chatMemoryStore = Objects.requireNonNull(chatMemoryStore, "chatMemoryStore");
	}

	@Override
	public synchronized Object id() {
		return id;
	}

	@Override
	public synchronized void add(ChatMessage message) {
		// Rebuild from the persisted messages, append the new turn, then compact and persist.
		List<ChatMessage> messages = new ArrayList<>(messages());
		messages.add(message);
		chatMemoryStore.updateMessages(id, compact(messages));
	}

	@Override
	public synchronized List<ChatMessage> messages() {
		List<ChatMessage> storedMessages = chatMemoryStore.getMessages(id);
		return storedMessages == null ? List.of() : new ArrayList<>(storedMessages);
	}

	@Override
	public synchronized void clear() {
		chatMemoryStore.deleteMessages(id);
	}

	@Override
	public synchronized void set(Iterable<ChatMessage> messages) {
		List<ChatMessage> messageList = new ArrayList<>();
		if (messages != null) {
			for (ChatMessage message : messages) {
				messageList.add(message);
			}
		}
		chatMemoryStore.updateMessages(id, compact(messageList));
	}

	private List<ChatMessage> compact(List<ChatMessage> messages) {
		if (messages.size() <= maxMessages) {
			return messages;
		}

		// Keep the system message intact if one is present, then summarize the oldest
		// non-system portion of the conversation and keep the newest turns verbatim.
		int prefixSize = messages.isEmpty() || !(messages.get(0) instanceof SystemMessage) ? 0 : 1;
		int messagesToSummarize = messages.size() - maxMessages + 1;
		int summaryStartIndex = prefixSize;
		int summaryEndIndex = Math.min(messages.size(), summaryStartIndex + messagesToSummarize);

		List<ChatMessage> toSummarize = messages.subList(summaryStartIndex, summaryEndIndex);
		String summaryText = summarize(toSummarize);

		List<ChatMessage> compacted = new ArrayList<>();
		if (prefixSize == 1) {
			compacted.add(messages.get(0));
		}
		compacted.add(AiMessage.from("Conversation summary: " + summaryText));
		compacted.addAll(messages.subList(summaryEndIndex, messages.size()));
		return compacted;
	}

	private String summarize(List<ChatMessage> messagesToSummarize) {
		if (messagesToSummarize.isEmpty()) {
			return "";
		}

		// The summarizer is the same Ollama-backed ChatModel used elsewhere in this branch,
		// but here we use it purely as a compaction step for memory.
		String conversation = messagesToSummarize.stream()
				.map(this::renderMessage)
				.collect(Collectors.joining("\n"));

		ChatResponse summaryResponse = summarizerModel.chat(
				SystemMessage.from("You summarize conversations for long-term memory."),
				UserMessage.from(String.format(SUMMARY_PROMPT, conversation)));

		String summaryText = summaryResponse.aiMessage().text();
		return summaryText == null || summaryText.isBlank() ? conversation : summaryText.trim();
	}

	private String renderMessage(ChatMessage message) {
		if (message instanceof SystemMessage systemMessage) {
			return "SYSTEM: " + systemMessage.text();
		}
		if (message instanceof UserMessage userMessage) {
			return "USER: " + (userMessage.hasSingleText() ? userMessage.singleText() : userMessage.toString());
		}
		if (message instanceof AiMessage aiMessage) {
			return "ASSISTANT: " + Objects.toString(aiMessage.text(), aiMessage.toString());
		}
		if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
			return "TOOL: " + toolExecutionResultMessage.text();
		}
		return message.type().name() + ": " + message;
	}
}
