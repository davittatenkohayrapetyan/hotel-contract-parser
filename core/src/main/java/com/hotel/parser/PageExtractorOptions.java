package com.hotel.parser;

import java.io.File;

/**
 * Configuration options for {@link PageExtractor}.
 */
public record PageExtractorOptions(int ocrDpi, File tessDataDir, int minNativeTextLength) {
    public static final int DEFAULT_MIN_NATIVE_TEXT_LENGTH = 32;
    private static final int DEFAULT_DPI = 300;

    public PageExtractorOptions {
        if (ocrDpi <= 0) {
            throw new IllegalArgumentException("ocrDpi must be positive");
        }
        if (minNativeTextLength < 0) {
            throw new IllegalArgumentException("minNativeTextLength must be zero or greater");
        }
        if (tessDataDir != null && !tessDataDir.isDirectory()) {
            throw new IllegalArgumentException("tessDataDir must be a directory");
        }
    }

    /**
     * Create options using default values (300 DPI, no tessdata override, minimum native text length of 32).
     */
    public static PageExtractorOptions defaults() {
        return new PageExtractorOptions(DEFAULT_DPI, null, DEFAULT_MIN_NATIVE_TEXT_LENGTH);
    }
}
