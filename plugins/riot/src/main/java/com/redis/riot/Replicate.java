package com.redis.riot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;

import com.redis.riot.core.Step;
import com.redis.spring.batch.item.redis.RedisItemReader;
import com.redis.spring.batch.item.redis.RedisItemReader.ReaderMode;
import com.redis.spring.batch.item.redis.RedisItemWriter;
import com.redis.spring.batch.item.redis.common.KeyValue;
import com.redis.spring.batch.item.redis.reader.KeyComparisonItemReader;
import com.redis.spring.batch.item.redis.reader.KeyNotificationItemReader;

import io.lettuce.core.codec.ByteArrayCodec;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "replicate", description = "Replicate a Redis database into another Redis database.")
public class Replicate extends AbstractCompareCommand {

	public enum CompareMode {
		FULL, QUICK, NONE
	}

	public static final String STEP_NAME = "replicate";
	public static final CompareMode DEFAULT_COMPARE_MODE = CompareMode.QUICK;

	private static final String QUEUE_MESSAGE = " | capacity: %,d";
	private static final String SCAN_TASK_NAME = "Scanning";
	private static final String LIVEONLY_TASK_NAME = "Listening";
	private static final String LIVE_TASK_NAME = "Scanning/Listening";

	@Option(names = "--struct", description = "Enable data structure-specific replication")
	private boolean struct;

	@ArgGroup(exclusive = false)
	private RedisWriterArgs targetRedisWriterArgs = new RedisWriterArgs();

	@Option(names = "--log-keys", description = "Log keys being read and written.")
	private boolean logKeys;

	@Option(names = "--compare", description = "Compare mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", paramLabel = "<mode>")
	private CompareMode compareMode = DEFAULT_COMPARE_MODE;

	@Override
	protected boolean isQuickCompare() {
		return compareMode == CompareMode.QUICK;
	}

	@Override
	protected Job job() {
		List<Step<?, ?>> steps = new ArrayList<>();
		Step<KeyValue<byte[], Object>, KeyValue<byte[], Object>> replicateStep = step();
		steps.add(replicateStep);
		if (shouldCompare()) {
			steps.add(compareStep());
		}
		return job(steps);
	}

	@Override
	protected void configureTargetRedisWriter(RedisItemWriter<?, ?, ?> writer) {
		super.configureTargetRedisWriter(writer);
		log.info("Configuring target Redis writer with {}", targetRedisWriterArgs);
		targetRedisWriterArgs.configure(writer);
	}

	private Step<KeyValue<byte[], Object>, KeyValue<byte[], Object>> step() {
		RedisItemReader<byte[], byte[], Object> reader = reader();
		configureSourceRedisReader(reader);
		RedisItemWriter<byte[], byte[], KeyValue<byte[], Object>> writer = writer();
		configureTargetRedisWriter(writer);
		Step<KeyValue<byte[], Object>, KeyValue<byte[], Object>> step = step(STEP_NAME, reader, writer);
		step.processor(processor());
		step.taskName(taskName(reader));
		if (reader.getMode() != ReaderMode.SCAN) {
			step.statusMessageSupplier(() -> liveExtraMessage(reader));
		}
		if (logKeys) {
			log.info("Adding key logger");
			ReplicateWriteLogger<byte[], Object> writeLogger = new ReplicateWriteLogger<>(log, reader.getCodec());
			step.writeListener(writeLogger);
			ReplicateReadLogger<byte[]> readLogger = new ReplicateReadLogger<>(log, reader.getCodec());
			reader.addItemReadListener(readLogger);
			reader.addItemWriteListener(readLogger);
		}
		return step;
	}

	private boolean shouldCompare() {
		return compareMode != CompareMode.NONE && !getJobArgs().isDryRun();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private RedisItemReader<byte[], byte[], Object> reader() {
		if (struct) {
			log.info("Creating Redis data-structure reader");
			return RedisItemReader.struct(ByteArrayCodec.INSTANCE);
		}
		log.info("Creating Redis dump reader");
		return (RedisItemReader) RedisItemReader.dump();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private RedisItemWriter<byte[], byte[], KeyValue<byte[], Object>> writer() {
		if (struct) {
			log.info("Creating Redis data-structure writer");
			return RedisItemWriter.struct(ByteArrayCodec.INSTANCE);
		}
		log.info("Creating Redis dump writer");
		return (RedisItemWriter) RedisItemWriter.dump();
	}

	private String taskName(RedisItemReader<?, ?, ?> reader) {
		switch (reader.getMode()) {
		case SCAN:
			return SCAN_TASK_NAME;
		case LIVEONLY:
			return LIVEONLY_TASK_NAME;
		default:
			return LIVE_TASK_NAME;
		}
	}

	@SuppressWarnings("rawtypes")
	private String liveExtraMessage(RedisItemReader<?, ?, ?> reader) {
		KeyNotificationItemReader keyReader = (KeyNotificationItemReader) reader.getReader();
		if (keyReader == null || keyReader.getQueue() == null) {
			return "";
		}
		return String.format(QUEUE_MESSAGE, keyReader.getQueue().remainingCapacity());
	}

	@Override
	protected KeyComparisonItemReader<byte[], byte[]> compareReader() {
		KeyComparisonItemReader<byte[], byte[]> reader = super.compareReader();
		reader.setProcessor(processor());
		return reader;
	}

	public RedisWriterArgs getTargetRedisWriterArgs() {
		return targetRedisWriterArgs;
	}

	public void setTargetRedisWriterArgs(RedisWriterArgs redisWriterArgs) {
		this.targetRedisWriterArgs = redisWriterArgs;
	}

	@Override
	public boolean isStruct() {
		return struct;
	}

	public void setStruct(boolean type) {
		this.struct = type;
	}

	public boolean isLogKeys() {
		return logKeys;
	}

	public void setLogKeys(boolean enable) {
		this.logKeys = enable;
	}

	public CompareMode getCompareMode() {
		return compareMode;
	}

	public void setCompareMode(CompareMode compareMode) {
		this.compareMode = compareMode;
	}

}
