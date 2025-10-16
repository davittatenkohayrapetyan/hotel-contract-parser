package com.hotel.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * PDF Parser for hotel contract documents.
 * This is a stub implementation that opens a PDF and extracts basic metadata.
 */
public class PDFParser {
    private static final Logger logger = LoggerFactory.getLogger(PDFParser.class);

    /**
     * Parse a PDF file and return the result.
     *
     * @param pdfFile the PDF file to parse
     * @return ParseResult containing page count and other metadata
     * @throws IOException if the file cannot be read
     */
    public ParseResult parse(File pdfFile) throws IOException {
        logger.info("Opening PDF file: {}", pdfFile.getAbsolutePath());
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int pageCount = document.getNumberOfPages();
            logger.info("PDF has {} pages", pageCount);
            
            // Iterate through pages (stub - not extracting content yet)
            for (int i = 0; i < pageCount; i++) {
                logger.debug("Processing page {}", i + 1);
            }
            
            return new ParseResult(pageCount, pdfFile.getName());
        }
    }

    /**
     * Result of PDF parsing.
     */
    public static class ParseResult {
        private final int pageCount;
        private final String fileName;

        public ParseResult(int pageCount, String fileName) {
            this.pageCount = pageCount;
            this.fileName = fileName;
        }

        public int getPageCount() {
            return pageCount;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
