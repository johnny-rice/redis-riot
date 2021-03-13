package com.redislabs.riot.file;

import com.redislabs.riot.KeyValueProcessingOptions;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.AbstractLineTokenizer;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.xml.XmlItemReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Map;

@Slf4j
@Setter
@Command(name = "import", description = "Import file(s) into Redis")
public class KeyValueFileImportCommand extends AbstractFileImportCommand<Map<String, Object>> {

    @CommandLine.Mixin
    protected FileImportOptions options = FileImportOptions.builder().build();
    @CommandLine.Mixin
    private KeyValueProcessingOptions processingOptions = KeyValueProcessingOptions.builder().build();

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected AbstractItemStreamItemReader<Map<String, Object>> reader(String file, FileType fileType, Resource resource) {
        switch (fileType) {
            case CSV:
            case PSV:
            case TSV:
                DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
                tokenizer.setDelimiter(delimiter(fileType));
                tokenizer.setQuoteCharacter(options.getQuoteCharacter());
                if (!ObjectUtils.isEmpty(options.getIncludedFields())) {
                    tokenizer.setIncludedFields(options.getIncludedFields());
                }
                log.info("Creating delimited reader with {} for file {}", options, file);
                return flatFileReader(resource, tokenizer);
            case FW:
                FixedLengthTokenizer fixedLengthTokenizer = new FixedLengthTokenizer();
                Assert.notEmpty(options.getColumnRanges(), "Column ranges are required");
                fixedLengthTokenizer.setColumns(options.getColumnRanges());
                log.info("Creating fixed-width reader with {} for file {}", options, file);
                return flatFileReader(resource, fixedLengthTokenizer);
            case JSON:
                log.info("Creating JSON reader for file {}", file);
                return (JsonItemReader) FileUtils.jsonReader(resource, Map.class);
            case XML:
                log.info("Creating XML reader for file {}", file);
                return (XmlItemReader) FileUtils.xmlReader(resource, Map.class);
        }
        throw new IllegalArgumentException("Unsupported file type: " + fileType);
    }

    private String delimiter(FileType fileType) {
        if (options.getDelimiter() != null) {
            return options.getDelimiter();
        }
        switch (fileType) {
            case TSV:
                return DelimitedLineTokenizer.DELIMITER_TAB;
            case PSV:
                return "|";
            default:
                return DelimitedLineTokenizer.DELIMITER_COMMA;
        }
    }

    @Override
    protected ItemProcessor<Map<String, Object>, Map<String, Object>> processor() {
        return processingOptions.processor(connection);
    }

    private FlatFileItemReader<Map<String, Object>> flatFileReader(Resource resource, AbstractLineTokenizer tokenizer) {
        if (!ObjectUtils.isEmpty(options.getNames())) {
            tokenizer.setNames(options.getNames());
        }
        FlatFileItemReaderBuilder<Map<String, Object>> builder = new FlatFileItemReaderBuilder<>();
        builder.resource(resource);
        builder.encoding(getFileOptions().getEncoding());
        builder.lineTokenizer(tokenizer);
        builder.recordSeparatorPolicy(recordSeparatorPolicy());
        builder.linesToSkip(options.linesToSkip());
        builder.strict(true);
        builder.saveState(false);
        builder.fieldSetMapper(new MapFieldSetMapper());
        builder.skippedLinesCallback(new HeaderCallbackHandler(tokenizer));
        return builder.build();
    }

    private RecordSeparatorPolicy recordSeparatorPolicy() {
        return new DefaultRecordSeparatorPolicy(options.getQuoteCharacter().toString(), options.getContinuationString());
    }

    private static class HeaderCallbackHandler implements LineCallbackHandler {

        private final AbstractLineTokenizer tokenizer;

        public HeaderCallbackHandler(AbstractLineTokenizer tokenizer) {
            this.tokenizer = tokenizer;
        }

        @Override
        public void handleLine(String line) {
            log.info("Found header {}", line);
            tokenizer.setNames(tokenizer.tokenize(line).getValues());
        }
    }

}
