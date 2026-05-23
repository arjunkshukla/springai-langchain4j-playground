package com.ai_playground.springai_langchian4j.renderer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.template.TemplateRenderer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class SpelPromptTemplateRenderer implements TemplateRenderer {

	private static final Pattern SPEL_EXPRESSION_PATTERN = Pattern.compile("#\\{([^}]+)}");
	private final ExpressionParser parser = new SpelExpressionParser();

	@Override
	public String apply(String template, Map<String, Object> variables) {
		Assert.hasText(template, "template cannot be null or empty");
		Assert.notNull(variables, "variables cannot be null");
		Assert.noNullElements(variables.keySet(), "variables keys cannot be null");

		StandardEvaluationContext context = new StandardEvaluationContext();
		variables.forEach(context::setVariable);

		Matcher matcher = SPEL_EXPRESSION_PATTERN.matcher(template);
		StringBuffer rendered = new StringBuffer();

		while (matcher.find()) {
			Object value = this.parser.parseExpression(matcher.group(1)).getValue(context);
			matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : value.toString()));
		}
		matcher.appendTail(rendered);
		return rendered.toString();
	}
}
