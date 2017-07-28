package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.gson.TypeAdapter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class IsoFormatInstantTypeAdapter extends FormattingInstantTypeAdapter {
    public static final DateTimeFormatter DEFAULT_OUTPUT_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    public static final ImmutableList<DateTimeFormatter> DEFAULT_INPUT_PARSERS = ImmutableList.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private static final IsoFormatInstantTypeAdapter DEFAULT_INSTANCE = new IsoFormatInstantTypeAdapter();

    /**
     * Returns the statically defined instance constructed with the default formatter.
     * @return the instance
     * @see #DEFAULT_OUTPUT_FORMATTER
     * @see #DEFAULT_INPUT_PARSERS
     */
    public static TypeAdapter<Instant> getInstance() {
        return DEFAULT_INSTANCE;
    }

    public IsoFormatInstantTypeAdapter() {
        super(DEFAULT_OUTPUT_FORMATTER, DEFAULT_INPUT_PARSERS);
    }


}
