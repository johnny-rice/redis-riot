package com.redis.riot.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import picocli.CommandLine.Option;

public class FlatFileOptions {

	public static final String DEFAULT_CONTINUATION_STRING = "\\";

	@Option(names = "--max", description = "Max number of lines to import.", paramLabel = "<count>")
	private Optional<Integer> maxItemCount = Optional.empty();
	@Option(names = "--fields", arity = "1..*", description = "Delimited/FW field names.", paramLabel = "<names>")
	private List<String> names = new ArrayList<>();
	@Option(names = { "-h", "--header" }, description = "Delimited/FW first line contains field names.")
	private boolean header;
	@Option(names = "--delimiter", description = "Delimiter character.", paramLabel = "<string>")
	private Optional<String> delimiter = Optional.empty();
	@Option(names = "--skip", description = "Delimited/FW lines to skip at start.", paramLabel = "<count>")
	private Optional<Integer> linesToSkip = Optional.empty();
	@Option(names = "--include", arity = "1..*", description = "Delimited/FW field indices to include (0-based).", paramLabel = "<index>")
	private int[] includedFields;
	@Option(names = "--ranges", arity = "1..*", description = "Fixed-width column ranges.", paramLabel = "<string>")
	private List<String> columnRanges = new ArrayList<>();
	@Option(names = "--quote", description = "Escape character for delimited files (default: ${DEFAULT-VALUE}).", paramLabel = "<char>")
	private Character quoteCharacter = DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER;
	@Option(names = "--cont", description = "Line continuation string (default: ${DEFAULT-VALUE}).", paramLabel = "<string>")
	private String continuationString = DEFAULT_CONTINUATION_STRING;

	public FlatFileOptions() {
	}

	private FlatFileOptions(Builder builder) {
		this.maxItemCount = builder.maxItemCount;
		this.names = builder.names;
		this.header = builder.header;
		this.delimiter = builder.delimiter;
		this.linesToSkip = builder.linesToSkip;
		this.includedFields = builder.includedFields;
		this.columnRanges = builder.columnRanges;
		this.quoteCharacter = builder.quoteCharacter;
		this.continuationString = builder.continuationString;
	}

	public Optional<Integer> getMaxItemCount() {
		return maxItemCount;
	}

	public void setMaxItemCount(int count) {
		this.maxItemCount = Optional.of(count);
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(String... names) {
		this.names = Arrays.asList(names);
	}

	public boolean isHeader() {
		return header;
	}

	public void setHeader(boolean header) {
		this.header = header;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = Optional.of(delimiter);
	}

	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = Optional.of(linesToSkip);
	}

	public int[] getIncludedFields() {
		return includedFields;
	}

	public void setIncludedFields(int... includedFields) {
		this.includedFields = includedFields;
	}

	public List<String> getColumnRanges() {
		return columnRanges;
	}

	public void setColumnRanges(String... columnRanges) {
		this.columnRanges = Arrays.asList(columnRanges);
	}

	public Optional<String> getDelimiter() {
		return delimiter;
	}

	public Character getQuoteCharacter() {
		return quoteCharacter;
	}

	public void setQuoteCharacter(Character quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	public String getContinuationString() {
		return continuationString;
	}

	public void setContinuationString(String continuationString) {
		this.continuationString = continuationString;
	}

	public int getLinesToSkip() {
		if (linesToSkip.isPresent()) {
			return linesToSkip.get();
		}
		if (header) {
			return 1;
		}
		return 0;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder extends FileOptions.Builder<Builder> {
		private Optional<Integer> maxItemCount = Optional.empty();
		private List<String> names = new ArrayList<>();
		private boolean header;
		private Optional<String> delimiter = Optional.empty();
		private Optional<Integer> linesToSkip = Optional.empty();
		private int[] includedFields;
		private List<String> columnRanges = new ArrayList<>();
		private Character quoteCharacter = DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER;
		private String continuationString = DEFAULT_CONTINUATION_STRING;

		private Builder() {
		}

		public Builder max(int count) {
			return max(Optional.of(count));
		}

		public Builder max(Optional<Integer> count) {
			this.maxItemCount = count;
			return this;
		}

		public Builder names(String... names) {
			this.names = Arrays.asList(names);
			return this;
		}

		public Builder header(boolean header) {
			this.header = header;
			return this;
		}

		public Builder delimiter(String delimiter) {
			return delimiter(Optional.of(delimiter));
		}

		public Builder delimiter(Optional<String> delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		public Builder linesToSkip(int linesToSkip) {
			return linesToSkip(Optional.of(linesToSkip));
		}

		public Builder linesToSkip(Optional<Integer> linesToSkip) {
			this.linesToSkip = linesToSkip;
			return this;
		}

		public Builder includedFields(int... includedFields) {
			this.includedFields = includedFields;
			return this;
		}

		public Builder columnRanges(String... columnRanges) {
			this.columnRanges = Arrays.asList(columnRanges);
			return this;
		}

		public Builder quoteCharacter(Character quoteCharacter) {
			this.quoteCharacter = quoteCharacter;
			return this;
		}

		public Builder continuationString(String continuationString) {
			this.continuationString = continuationString;
			return this;
		}

		public FlatFileOptions build() {
			return new FlatFileOptions(this);
		}
	}

	@Override
	public String toString() {
		return "FlatFileOptions [names=" + names + ", max=" + maxItemCount + ", header=" + header + ", delimiter="
				+ delimiter + ", linesToSkip=" + linesToSkip + ", includedFields=" + Arrays.toString(includedFields)
				+ ", columnRanges=" + columnRanges + ", quoteCharacter=" + quoteCharacter + ", continuationString="
				+ continuationString + "]";
	}

}
