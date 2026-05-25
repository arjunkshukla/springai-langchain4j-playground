package com.ai_playground.springai_langchian4j.lc4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class PersistentChatMemoryStore implements ChatMemoryStore {

	private final ChatMessageEntityRepository chatMessageEntityRepository;

	public PersistentChatMemoryStore(ChatMessageEntityRepository chatMessageEntityRepository) {
		this.chatMessageEntityRepository = chatMessageEntityRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessage> getMessages(Object memoryId) {
		String sessionId = Objects.toString(memoryId, null);
		if (sessionId == null) {
			return List.of();
		}

		return chatMessageEntityRepository.findByMemoryIdOrderByPkAsc(sessionId).stream()
				.map(entity -> ChatMessageDeserializer.messageFromJson(entity.getMessage()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void updateMessages(Object memoryId, List<ChatMessage> messages) {
		String sessionId = Objects.toString(memoryId, null);
		if (sessionId == null) {
			return;
		}

		chatMessageEntityRepository.deleteByMemoryId(sessionId);
		chatMessageEntityRepository.saveAll(messages.stream()
				.map(message -> new ChatMessageEntity(
						sessionId,
						ChatMessageSerializer.messageToJson(message),
						message.type()))
				.collect(Collectors.toList()));
	}

	@Override
	@Transactional
	public void deleteMessages(Object memoryId) {
		String sessionId = Objects.toString(memoryId, null);
		if (sessionId == null) {
			return;
		}

		chatMessageEntityRepository.deleteByMemoryId(sessionId);
	}
}
