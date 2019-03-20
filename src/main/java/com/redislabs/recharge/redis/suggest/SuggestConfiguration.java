package com.redislabs.recharge.redis.suggest;

import com.redislabs.recharge.redisearch.RediSearchCommandConfiguration;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SuggestConfiguration extends RediSearchCommandConfiguration {
	private String field;
	private String score;
	private double defaultScore = 1d;
	private boolean increment;
}