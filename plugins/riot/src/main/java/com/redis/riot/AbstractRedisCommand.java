package com.redis.riot;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.util.RedisModulesUtils;
import com.redis.riot.RedisClientBuilder.RedisURIClient;
import com.redis.riot.core.AbstractJobCommand;
import com.redis.riot.core.Step;
import com.redis.spring.batch.item.redis.RedisItemReader;
import com.redis.spring.batch.item.redis.RedisItemReader.ReaderMode;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisException;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

abstract class AbstractRedisCommand extends AbstractJobCommand {

	private static final String NOTIFY_CONFIG = "notify-keyspace-events";
	private static final String NOTIFY_CONFIG_VALUE = "KEA";

	@ArgGroup(exclusive = false, heading = "TLS options%n")
	private SslArgs sslArgs = new SslArgs();

	@Option(names = "--client", description = "Client name used to connect to Redis (default: ${DEFAULT-VALUE}).", paramLabel = "<name>")
	private String clientName = RedisClientBuilder.DEFAULT_CLIENT_NAME;

	protected RedisURIClient client;
	protected StatefulRedisModulesConnection<String, String> connection;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (client == null) {
			client = redisURIClient();
		}
		if (connection == null) {
			connection = RedisModulesUtils.connection(client.getClient());
		}
	}

	protected <K, V, T> RedisItemReader<K, V, T> configure(RedisItemReader<K, V, T> reader) {
		super.configure(reader);
		reader.setClient(client.getClient());
		reader.setDatabase(client.getUri().getDatabase());
		return reader;
	}

	protected abstract RedisURIClient redisURIClient();

	@Override
	protected void shutdown() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
		if (client != null) {
			client.close();
			client = null;
		}
	}

	protected RedisClientBuilder redisClientBuilder() {
		RedisClientBuilder builder = new RedisClientBuilder();
		builder.clientName(clientName);
		builder.sslOptions(sslArgs.sslOptions());
		return builder;
	}

	protected void configureExportStep(Step<?, ?> step) {
		RedisItemReader<?, ?, ?> reader = (RedisItemReader<?, ?, ?>) step.getReader();
		if (reader.getMode() != ReaderMode.LIVEONLY) {
			log.info("Creating scan size estimator for step {} with pattern {} and type {}", step.getName(),
					reader.getKeyPattern(), reader.getKeyType());
			RedisScanSizeEstimator estimator = new RedisScanSizeEstimator(reader.getClient());
			estimator.setKeyPattern(reader.getKeyPattern());
			estimator.setKeyType(reader.getKeyType());
			step.maxItemCountSupplier(estimator);
		}
		if (reader.getMode() != ReaderMode.SCAN) {
			checkNotifyConfig(reader.getClient());
			log.info("Configuring step {} as live with {} and {}", step.getName(), reader.getFlushInterval(),
					reader.getIdleTimeout());
			step.live(true);
			step.flushInterval(reader.getFlushInterval());
			step.idleTimeout(reader.getIdleTimeout());
		}
	}

	private void checkNotifyConfig(AbstractRedisClient client) {
		Map<String, String> valueMap;
		try (StatefulRedisModulesConnection<String, String> conn = RedisModulesUtils.connection(client)) {
			try {
				valueMap = conn.sync().configGet(NOTIFY_CONFIG);
			} catch (RedisException e) {
				return;
			}
		}
		String actual = valueMap.getOrDefault(NOTIFY_CONFIG, "");
		Set<Character> expected = characterSet(NOTIFY_CONFIG_VALUE);
		Assert.isTrue(characterSet(actual).containsAll(expected),
				String.format("Keyspace notifications not property configured: expected '%s' but was '%s'.",
						NOTIFY_CONFIG_VALUE, actual));
	}

	private static Set<Character> characterSet(String string) {
		return string.codePoints().mapToObj(c -> (char) c).collect(Collectors.toSet());
	}

}
