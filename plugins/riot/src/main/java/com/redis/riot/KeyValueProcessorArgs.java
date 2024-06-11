package com.redis.riot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.function.FunctionItemProcessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import com.redis.riot.core.TemplateExpression;
import com.redis.riot.function.ConsumerUnaryOperator;
import com.redis.riot.function.DropStreamMessageId;
import com.redis.spring.batch.item.redis.common.KeyValue;

import picocli.CommandLine.Option;

public class KeyValueProcessorArgs {

	@Option(names = "--key-proc", description = "SpEL template expression to transform key names, e.g. \"#{#source.database}:#{key}\" for 'abc' returns '0:abc'", paramLabel = "<exp>")
	private TemplateExpression keyExpression;

	@Option(names = "--type-proc", description = "SpEL expression to transform key types.", paramLabel = "<exp>")
	private Expression typeExpression;

	@Option(names = "--ttl-proc", description = "SpEL expression to transform key expiration times.", paramLabel = "<exp>")
	private Expression ttlExpression;

	@Option(names = "--ttls", description = "Propagate key expiration times. True by default.", negatable = true, defaultValue = "true", fallbackValue = "true")
	private boolean propagateTtl = true;

	@Option(names = "--stream-ids", description = "Propagate stream message IDs. True by default.", negatable = true, defaultValue = "true", fallbackValue = "true")
	private boolean propagateStreamMessageId = true;

	public ItemProcessor<KeyValue<String, Object>, KeyValue<String, Object>> processor(EvaluationContext context) {
		List<Consumer<KeyValue<String, Object>>> consumers = new ArrayList<>();
		if (keyExpression != null) {
			consumers.add(t -> t.setKey(keyExpression.getExpression().getValue(context, t, String.class)));
		}
		if (!propagateTtl) {
			consumers.add(t -> t.setTtl(0));
		}
		if (ttlExpression != null) {
			consumers.add(t -> t.setTtl(ttlExpression.getValue(context, t, Long.class)));
		}
		if (!propagateStreamMessageId) {
			consumers.add(new DropStreamMessageId());
		}
		if (typeExpression != null) {
			consumers.add(t -> t.setType(typeExpression.getValue(context, t, String.class)));
		}
		if (consumers.isEmpty()) {
			return null;
		}
		return new FunctionItemProcessor<>(new ConsumerUnaryOperator<>(consumers));
	}

	public TemplateExpression getKeyExpression() {
		return keyExpression;
	}

	public void setKeyExpression(TemplateExpression keyExpression) {
		this.keyExpression = keyExpression;
	}

	public Expression getTypeExpression() {
		return typeExpression;
	}

	public void setTypeExpression(Expression typeExpression) {
		this.typeExpression = typeExpression;
	}

	public Expression getTtlExpression() {
		return ttlExpression;
	}

	public void setTtlExpression(Expression ttlExpression) {
		this.ttlExpression = ttlExpression;
	}

	public boolean isPropagateTtl() {
		return propagateTtl;
	}

	public void setPropagateTtl(boolean propagateTtls) {
		this.propagateTtl = propagateTtls;
	}

	public boolean isPropagateStreamMessageId() {
		return propagateStreamMessageId;
	}

	public void setPropagateStreamMessageId(boolean propagateStreamMessageIds) {
		this.propagateStreamMessageId = propagateStreamMessageIds;
	}

	@Override
	public String toString() {
		return "KeyValueProcessorArgs [keyExpression=" + keyExpression + ", typeExpression=" + typeExpression
				+ ", ttlExpression=" + ttlExpression + ", propagateTtl=" + propagateTtl + ", propagateStreamMessageId="
				+ propagateStreamMessageId + "]";
	}

}
