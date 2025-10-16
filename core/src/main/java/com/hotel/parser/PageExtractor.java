package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Extracts text from each page of a PDF document using PDFBox.
 */
public class PageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PageExtractor.class);

    /**
     * Extract text content for every page in the provided {@link PDDocument}.
     *
     * @param document the loaded PDF document
     * @return immutable list of page data containing page numbers and their text
     * @throws IOException if text extraction fails
     */
    public List<Page> extractPages(PDDocument document) throws IOException {
        Objects.requireNonNull(document, "document");

        int totalPages = document.getNumberOfPages();
        logger.debug("Extracting text from {} pages", totalPages);

        if (totalPages == 0) {
            return Collections.emptyList();
        }

        List<Page> pages = new ArrayList<>(totalPages);
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);

        for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
            textStripper.setStartPage(pageIndex);
            textStripper.setEndPage(pageIndex);
            String text = textStripper.getText(document);
            String cleanedText = text == null ? "" : text.trim();
            logger.trace("Page {} extracted with {} characters", pageIndex, cleanedText.length());
            pages.add(new Page(pageIndex, cleanedText));
        }

        return Collections.unmodifiableList(pages);
    }

    /**
     * Immutable value object describing a single PDF page.
     */
    public record Page(int pageNumber, String text) {
        public Page {
            if (pageNumber < 1) {
                throw new IllegalArgumentException("pageNumber must be 1 or greater");
            }
            text = text == null ? "" : text;
        }
    }
}
