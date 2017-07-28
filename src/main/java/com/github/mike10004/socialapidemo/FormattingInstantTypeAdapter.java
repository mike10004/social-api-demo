package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FormattingInstantTypeAdapter extends TypeAdapter<Instant> {

    private final DateTimeFormatter outputDateTimeFormatter;
    private final DateTimeFormatter firstParser;
    private final ImmutableList<DateTimeFormatter> otherParsers;

    public FormattingInstantTypeAdapter(DateTimeFormatter dateTimeFormatter, Iterable<DateTimeFormatter> inputDateTimeParsers) {
        this.outputDateTimeFormatter = checkNotNull(dateTimeFormatter);
        Iterator<DateTimeFormatter> parsers = inputDateTimeParsers.iterator();
        checkArgument(parsers.hasNext(), "input parsers list must be nonempty");
        this.firstParser = parsers.next();
        this.otherParsers = ImmutableList.copyOf(parsers);
    }

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            String instantStr = outputDateTimeFormatter.format(value);
            out.value(instantStr);
        }
    }

    protected Instant parse(String instantStr) throws JsonDateTimeParseException {
        if (instantStr == null) {
            return null;
        }
        TemporalAccessor accessor = null;
        try {
            accessor = firstParser.parse(instantStr);
        } catch (DateTimeParseException ignore) {
        }
        if (accessor == null) {
            for (DateTimeFormatter parser : otherParsers) {
                try {
                    accessor = parser.parse(instantStr);
                } catch (DateTimeParseException ignore) {
                }
            }
        }
        if (accessor == null) {
            throw new JsonDateTimeParseException("input string does not match any of " + getNumFormats() + " parsing formats used by this adapter");
        }
        return Instant.from(accessor);
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String instantStr = in.nextString();
        return parse(instantStr);
    }

    private long getNumFormats() {
        return 1L + otherParsers.size();
    }

    public static class JsonDateTimeParseException extends JsonParseException {

        public JsonDateTimeParseException(String msg) {
            super(msg);
        }

        public JsonDateTimeParseException(String msg, Throwable cause) {
            super(msg, cause);
        }

        public JsonDateTimeParseException(Throwable cause) {
            super(cause);
        }
    }
}
