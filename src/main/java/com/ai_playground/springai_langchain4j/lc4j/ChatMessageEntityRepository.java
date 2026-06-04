package com.ai_playground.springai_langchain4j.lc4j;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageEntityRepository extends JpaRepository<ChatMessageEntity, Long> {

	List<ChatMessageEntity> findByMemoryIdOrderByPkAsc(String memoryId);

	void deleteByMemoryId(String memoryId);
}
