package com.redis.riot.core;

import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class StepArgs {

	public static final int DEFAULT_CHUNK_SIZE = 50;
	public static final int DEFAULT_THREADS = 1;
	public static final RetryPolicy DEFAULT_RETRY_POLICY = RetryPolicy.LIMIT;
	public static final SkipPolicy DEFAULT_SKIP_POLICY = SkipPolicy.NEVER;
	public static final int DEFAULT_RETRY_LIMIT = MaxAttemptsRetryPolicy.DEFAULT_MAX_ATTEMPTS;

	@Option(names = "--sleep", description = "Duration in millis to wait after each batch write (default: no sleep).", paramLabel = "<ms>")
	private long sleep;

	@Option(names = "--threads", description = "Number of concurrent threads to use for batch processing (default: ${DEFAULT-VALUE}).", paramLabel = "<int>")
	private int threads = DEFAULT_THREADS;

	@Option(names = "--batch", description = "Number of items in each batch (default: ${DEFAULT-VALUE}).", paramLabel = "<size>")
	private int chunkSize = DEFAULT_CHUNK_SIZE;

	@Option(names = "--dry-run", description = "Enable dummy writes.")
	private boolean dryRun;

	@Option(names = "--skip", description = "Skip policy: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", paramLabel = "<name>")
	private SkipPolicy skipPolicy = DEFAULT_SKIP_POLICY;

	@Option(names = "--skip-limit", description = "Number of skips tolerated before failing.", paramLabel = "<int>")
	private int skipLimit;

	@Option(names = "--retry", description = "Retry policy: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", paramLabel = "<name>")
	private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;

	@Option(names = "--retry-limit", description = "Number of times to try failed items (default: ${DEFAULT-VALUE}). 0 and 1 both mean no retry.", paramLabel = "<int>")
	private int retryLimit = DEFAULT_RETRY_LIMIT;

	@ArgGroup(exclusive = false)
	private ProgressArgs progressArgs = new ProgressArgs();

	public long getSleep() {
		return sleep;
	}

	public void setSleep(long sleep) {
		this.sleep = sleep;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public ProgressArgs getProgressArgs() {
		return progressArgs;
	}

	public void setProgressArgs(ProgressArgs args) {
		this.progressArgs = args;
	}

	public int getSkipLimit() {
		return skipLimit;
	}

	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	public int getRetryLimit() {
		return retryLimit;
	}

	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	public SkipPolicy getSkipPolicy() {
		return skipPolicy;
	}

	public void setSkipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
	}

	public RetryPolicy getRetryPolicy() {
		return retryPolicy;
	}

	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	@Override
	public String toString() {
		return "StepArgs [sleep=" + sleep + ", threads=" + threads + ", chunkSize=" + chunkSize + ", dryRun=" + dryRun
				+ ", skipPolicy=" + skipPolicy + ", skipLimit=" + skipLimit + ", retryPolicy=" + retryPolicy
				+ ", retryLimit=" + retryLimit + ", progressArgs=" + progressArgs + "]";
	}

	public org.springframework.batch.core.step.skip.SkipPolicy skipPolicy() {
		switch (skipPolicy) {
		case ALWAYS:
			return new AlwaysSkipItemSkipPolicy();
		case NEVER:
			return new NeverSkipItemSkipPolicy();
		default:
			return new LimitCheckingItemSkipPolicy();
		}
	}

	public org.springframework.retry.RetryPolicy retryPolicy() {
		switch (retryPolicy) {
		case ALWAYS:
			return new AlwaysRetryPolicy();
		case NEVER:
			return new NeverRetryPolicy();
		default:
			return new MaxAttemptsRetryPolicy();
		}

	}

}
