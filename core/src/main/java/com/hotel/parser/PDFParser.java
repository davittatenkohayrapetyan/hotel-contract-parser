package com.hotel.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * PDF Parser for hotel contract documents that extracts per-page text content
 * using Apache PDFBox.
 */
public class PDFParser {
    private static final Logger logger = LoggerFactory.getLogger(PDFParser.class);

    private final PageExtractor pageExtractor;

    public PDFParser() {
        this(new PageExtractor());
    }

    public PDFParser(PageExtractor pageExtractor) {
        this.pageExtractor = Objects.requireNonNull(pageExtractor, "pageExtractor");
    }

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
            String title = determineTitle(document, pdfFile);
            List<PageExtractor.Page> pages = pageExtractor.extractPages(document);
            logger.info("PDF has {} pages", pages.size());

            return new ParseResult(pages, pdfFile.getName(), title);
        }
    }

    private String determineTitle(PDDocument document, File pdfFile) {
        if (document.getDocumentInformation() != null) {
            String title = document.getDocumentInformation().getTitle();
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return pdfFile.getName();
    }

    /**
     * Result of PDF parsing.
     */
    public static class ParseResult {
        private final String fileName;
        private final String title;
        private final List<PageExtractor.Page> pages;

        public ParseResult(List<PageExtractor.Page> pages, String fileName, String title) {
            this.pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
            this.fileName = Objects.requireNonNull(fileName, "fileName");
            this.title = title == null ? "" : title;
        }

        public int getPageCount() {
            return pages.size();
        }

        public String getFileName() {
            return fileName;
        }

        public String getTitle() {
            return title;
        }

        public List<PageExtractor.Page> getPages() {
            return pages;
        }
    }
}
