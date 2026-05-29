package com.ai_playground.springai_langchian4j.rag;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DocumentReader {
	
	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	public List<Document> textReader(Resource resource) {
		TextReader reader = new TextReader(resource);
		reader.getCustomMetadata().put("source", resource.getFilename());
		return reader.read();
	}
	
	public List<Document> tikaReader(Resource resource) {
		TikaDocumentReader reader = new TikaDocumentReader(resource);
		return reader.get();
	}

	public List<Document> directoryReader(String pathPattern) {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources(pathPattern);
			return Arrays.stream(resources)
				.filter(DocumentReader::isReadableDocument)
				.sorted(Comparator.comparing(DocumentReader::resourceName))
				.flatMap(resource -> new TikaDocumentReader(resource).get().stream())
				.toList();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load documents from classpath:/docs", ex);
		}
	}

	private static boolean isReadableDocument(Resource resource) {
		return resource != null && resource.exists() && resource.isReadable();
	}

	private static String resourceName(Resource resource) {
		try {
			String filename = resource.getFilename();
			if (StringUtils.hasText(filename)) {
				return filename;
			}
			return Objects.requireNonNullElse(resource.getURI().toString(), "");
		}
		catch (IOException ex) {
			return "";
		}
	}
}
