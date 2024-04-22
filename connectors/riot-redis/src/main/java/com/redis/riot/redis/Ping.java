package com.redis.riot.redis;

import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.util.RedisModulesUtils;
import com.redis.riot.core.AbstractRedisRunnable;

import io.lettuce.core.metrics.CommandMetrics.CommandLatency;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;

public class Ping extends AbstractRedisRunnable {

	public static final int DEFAULT_ITERATIONS = 1;
	public static final int DEFAULT_COUNT = 10;
	public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
	private static final double[] DEFAULT_PERCENTILES = DefaultCommandLatencyCollectorOptions.DEFAULT_TARGET_PERCENTILES;

	private PrintWriter out;
	private int iterations = DEFAULT_ITERATIONS;
	private int count = DEFAULT_COUNT;
	private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
	private boolean latencyDistribution;
	private double[] percentiles = DEFAULT_PERCENTILES;

	public void setOut(PrintWriter out) {
		this.out = out;
	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit unit) {
		this.timeUnit = unit;
	}

	public boolean isLatencyDistribution() {
		return latencyDistribution;
	}

	public void setLatencyDistribution(boolean latencyDistribution) {
		this.latencyDistribution = latencyDistribution;
	}

	public double[] getPercentiles() {
		return percentiles;
	}

	public void setPercentiles(double[] percentiles) {
		this.percentiles = percentiles;
	}

	@Override
	protected Job job() {
		TaskletStep step = new TaskletStep();
		CallableTaskletAdapter tasklet = new CallableTaskletAdapter();
		tasklet.setCallable(this::call);
		step.setName(getName());
		step.setTransactionManager(getJobFactory().getPlatformTransactionManager());
		step.setJobRepository(getJobFactory().getJobRepository());
		step.setTasklet(tasklet);
		return jobBuilder().start(step).build();
	}

	private RepeatStatus call() {
		try (StatefulRedisModulesConnection<String, String> connection = RedisModulesUtils
				.connection(getRedisClient())) {
			RedisModulesCommands<String, String> commands = connection.sync();
			for (int iteration = 0; iteration < iterations; iteration++) {
				LatencyStats stats = new LatencyStats();
				for (int index = 0; index < count; index++) {
					long startTime = System.nanoTime();
					String reply = commands.ping();
					Assert.isTrue("pong".equalsIgnoreCase(reply), "Invalid PING reply received: " + reply);
					stats.recordLatency(System.nanoTime() - startTime);
				}
				Histogram histogram = stats.getIntervalHistogram();
				if (latencyDistribution) {
					histogram.outputPercentileDistribution(System.out, (double) timeUnit.toNanos(1));
				}
				Map<Double, Long> percentileMap = new TreeMap<>();
				for (double targetPercentile : percentiles) {
					long percentile = toTimeUnit(histogram.getValueAtPercentile(targetPercentile));
					percentileMap.put(targetPercentile, percentile);
				}
				long min = toTimeUnit(histogram.getMinValue());
				long max = toTimeUnit(histogram.getMaxValue());
				CommandLatency latency = new CommandLatency(min, max, percentileMap);
				out.println(latency.toString());
				if (getSleep() != null) {
					try {
						Thread.sleep(getSleep().toMillis());
					} catch (InterruptedException e) {
						// Restore interrupted state...
						Thread.currentThread().interrupt();
					}
				}
			}
		}
		return RepeatStatus.FINISHED;
	}

	private long toTimeUnit(long value) {
		return timeUnit.convert(value, TimeUnit.NANOSECONDS);
	}

	public static double[] defaultPercentiles() {
		return DEFAULT_PERCENTILES;
	}

}