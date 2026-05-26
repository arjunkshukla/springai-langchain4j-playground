package com.ai_playground.springai_langchian4j.lc4j;

import java.util.ArrayList;
import java.util.Iterator;
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

	private static final String SUMMARY_PREFIX = "[MEMORY_SUMMARY]\n";
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
		if (messages.isEmpty()) {
			return List.of();
		}

		// Keep the original system message, preserve one explicit summary block if it
		// already exists, and only compact the older raw conversation turns.
		List<ChatMessage> working = new ArrayList<>(messages);
		SystemMessage systemMessage = extractSystemMessage(working);
		ChatMessage summaryMessage = extractSummaryMessage(working);
		int rawWindowSize = Math.max(1, maxMessages - (systemMessage == null ? 0 : 1) - 1);

		if (summaryMessage != null) {
			List<ChatMessage> recentRawMessages = tail(working, rawWindowSize);
			return assemble(systemMessage, summaryMessage, recentRawMessages);
		}

		int maxRawMessagesWithoutSummary = Math.max(1, maxMessages - (systemMessage == null ? 0 : 1));
		if (working.size() <= maxRawMessagesWithoutSummary) {
			return assemble(systemMessage, null, working);
		}

		int messagesToSummarize = Math.max(1, working.size() - rawWindowSize);
		List<ChatMessage> toSummarize = working.subList(0, messagesToSummarize);
		String summaryText = summarize(toSummarize);
		ChatMessage newSummaryMessage = summaryMessage(summaryText);
		List<ChatMessage> recentRawMessages = tail(working.subList(messagesToSummarize, working.size()), rawWindowSize);
		return assemble(systemMessage, newSummaryMessage, recentRawMessages);
	}

	private List<ChatMessage> assemble(SystemMessage systemMessage, ChatMessage summaryMessage,
			List<ChatMessage> rawMessages) {
		List<ChatMessage> compacted = new ArrayList<>();
		if (systemMessage != null) {
			compacted.add(systemMessage);
		}
		if (summaryMessage != null) {
			compacted.add(summaryMessage);
		}
		compacted.addAll(rawMessages);
		return compacted;
	}

	private List<ChatMessage> tail(List<ChatMessage> messages, int maxCount) {
		if (messages.size() <= maxCount) {
			return new ArrayList<>(messages);
		}
		return new ArrayList<>(messages.subList(messages.size() - maxCount, messages.size()));
	}

	private SystemMessage extractSystemMessage(List<ChatMessage> messages) {
		if (!messages.isEmpty() && messages.get(0) instanceof SystemMessage systemMessage) {
			messages.remove(0);
			return systemMessage;
		}
		return null;
	}

	private ChatMessage extractSummaryMessage(List<ChatMessage> messages) {
		for (Iterator<ChatMessage> iterator = messages.iterator(); iterator.hasNext();) {
			ChatMessage message = iterator.next();
			if (isSummaryMessage(message)) {
				iterator.remove();
				return message;
			}
		}
		return null;
	}

	private boolean isSummaryMessage(ChatMessage message) {
		// Keep the summary as an explicit memory note, not as a normal assistant reply,
		// so compaction can preserve it without the model treating it like a fresh turn.
		return message instanceof SystemMessage systemMessage
				&& systemMessage.text() != null
				&& systemMessage.text().startsWith(SUMMARY_PREFIX);
	}

	private ChatMessage summaryMessage(String summaryText) {
		return SystemMessage.from(SUMMARY_PREFIX + summaryText);
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
