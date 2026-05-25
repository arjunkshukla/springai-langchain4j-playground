package com.ai_playground.springai_langchian4j.lc4j;

import org.springframework.lang.NonNull;

import dev.langchain4j.data.message.ChatMessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long pk;

	@Column(nullable = false)
	private String memoryId;

	@NonNull
	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private ChatMessageType chatMessageType;

	protected ChatMessageEntity() {
	}

	public ChatMessageEntity(String memoryId, String message, ChatMessageType chatMessageType) {
		this.memoryId = memoryId;
		this.message = message;
		this.chatMessageType = chatMessageType;
	}

	public Long getPk() {
		return pk;
	}

	public String getMemoryId() {
		return memoryId;
	}

	public String getMessage() {
		return message;
	}

	public ChatMessageType getChatMessageType() {
		return chatMessageType;
	}

	public void setPk(Long pk) {
		this.pk = pk;
	}

	public void setMemoryId(String memoryId) {
		this.memoryId = memoryId;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setChatMessageType(ChatMessageType chatMessageType) {
		this.chatMessageType = chatMessageType;
	}
}
