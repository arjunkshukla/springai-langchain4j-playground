package com.ai_playground.springai_langchain4j.rag;

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

/**
 * Loads raw Spring AI {@link Document documents} from resources.
 *
 * <p>This component is the first step in the ETL pipeline: read the file, let
 * Tika or Spring's text reader extract content, then hand the documents to the
 * splitter.</p>
 */
@Component
public class DocumentReader {
	
	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	/**
	 * Reads plain-text resources with Spring AI's {@link TextReader}.
	 *
	 * <p>The source filename is stored in metadata so downstream retrieval and
	 * previews can show where a chunk came from.</p>
	 */
	public List<Document> textReader(Resource resource) {
		TextReader reader = new TextReader(resource);
		reader.getCustomMetadata().put("source", resource.getFilename());
		return reader.read();
	}
	
	/**
	 * Reads any Tika-supported resource into one or more documents.
	 *
	 * <p>This is the general-purpose reader used for PDFs and other extracted
	 * document formats.</p>
	 */
	public List<Document> tikaReader(Resource resource) {
		TikaDocumentReader reader = new TikaDocumentReader(resource);
		return reader.get();
	}

	/**
	 * Scans a classpath pattern and reads every readable document in filename
	 * order.
	 *
	 * <p>The ordering keeps previews and test fixtures deterministic.</p>
	 */
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

	/**
	 * Guards the split/read pipeline from trying to process missing or unreadable
	 * files.
	 */
	public static boolean isReadableDocument(Resource resource) {
		return resource != null && resource.exists() && resource.isReadable();
	}

	/**
	 * Returns a stable display name for a resource, falling back to the URI when
	 * a filename is not available.
	 */
	public static String resourceName(Resource resource) {
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
