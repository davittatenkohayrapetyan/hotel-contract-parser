package com.hotel.parser;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Extracts text from each page of a PDF document using PDFBox and optionally OCR via Tess4J.
 */
public class PageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PageExtractor.class);
    private static final String OCR_LANGUAGE = "eng";

    private final PageExtractorOptions options;
    private Tesseract tesseract;

    public PageExtractor() {
        this(PageExtractorOptions.defaults());
    }

    public PageExtractor(PageExtractorOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

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
        PDFRenderer renderer = new PDFRenderer(document);

        for (int pageZeroBased = 0; pageZeroBased < totalPages; pageZeroBased++) {
            int pageNumber = pageZeroBased + 1;
            textStripper.setStartPage(pageNumber);
            textStripper.setEndPage(pageNumber);
            String text = textStripper.getText(document);
            String cleanedText = text == null ? "" : text.trim();
            logger.trace("Page {} extracted with {} characters", pageNumber, cleanedText.length());

            if (needsOcr(cleanedText)) {
                logger.debug("Page {} yielded {} native characters; attempting OCR", pageNumber, cleanedText.length());
                String ocrText = performOcr(renderer, pageZeroBased, pageNumber);
                cleanedText = mergeText(cleanedText, ocrText);
            }

            pages.add(new Page(pageNumber, cleanedText));
        }

        return Collections.unmodifiableList(pages);
    }

    private boolean needsOcr(String text) {
        return text == null || text.isBlank() || text.length() < options.minNativeTextLength();
    }

    private String mergeText(String nativeText, String ocrText) {
        String safeNative = nativeText == null ? "" : nativeText;
        String safeOcr = ocrText == null ? "" : ocrText;

        if (safeNative.isBlank()) {
            return safeOcr;
        }
        if (safeOcr.isBlank()) {
            return safeNative;
        }
        if (safeNative.contains(safeOcr)) {
            return safeNative;
        }
        return safeNative + System.lineSeparator() + safeOcr;
    }

    private String performOcr(PDFRenderer renderer, int pageZeroBased, int pageNumber) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(pageZeroBased, options.ocrDpi());
            try {
                String ocrText = getOrCreateTesseract().doOCR(image);
                return ocrText == null ? "" : ocrText.trim();
            } finally {
                image.flush();
            }
        } catch (IOException e) {
            logger.warn("Failed to render page {} for OCR: {}", pageNumber, e.getMessage());
            logger.debug("Render failure details", e);
        } catch (TesseractException e) {
            logger.warn("Tesseract OCR failed on page {}: {}", pageNumber, e.getMessage());
            logger.debug("Tesseract exception details", e);
        } catch (UnsatisfiedLinkError e) {
            logger.warn("Tesseract native libraries are unavailable; skipping OCR for page {}", pageNumber);
            logger.debug("UnsatisfiedLinkError", e);
        }
        return "";
    }

    private synchronized Tesseract getOrCreateTesseract() {
        if (tesseract == null) {
            Tesseract created = new Tesseract();
            if (options.tessDataDir() != null) {
                created.setDatapath(options.tessDataDir().getAbsolutePath());
            }
            created.setLanguage(OCR_LANGUAGE);
            tesseract = created;
        }
        return tesseract;
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
