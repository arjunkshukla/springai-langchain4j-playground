package com.ai_playground.springai_langchian4j.rag;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

public class DocumentReader {
	
	@Value("classpath:docs/coredeux-import.txt")
	private Resource textResource;
	
	@Value("classpath:docs/01-overview.pdf")
	private Resource pdfResource;
	
	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	public List<Document> textReaderSample() {
		TextReader reader = new TextReader(textResource);
		reader.getCustomMetadata().put("source", "coredeux-import");
		return reader.read();
	}
	
	public List<Document> pdfReaderSample() {
		TikaDocumentReader reader = new TikaDocumentReader(pdfResource);
		return reader.get();
	}

	public List<Document> directoryReaderSample() {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources("classpath*:docs/*");
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
