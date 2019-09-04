package com.redislabs.riot.redis.writer;

import java.util.Map;

public class SetFieldItemWriter extends SetItemWriter {
	private String field;

	public void setField(String field) {
		this.field = field;
	}

	@Override
	protected String value(Map<String, Object> item) {
		return convert(item.get(field), String.class);
	}

}