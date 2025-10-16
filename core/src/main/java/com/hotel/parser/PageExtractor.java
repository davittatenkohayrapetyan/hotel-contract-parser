package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Extracts text from each page of a PDF document using PDFBox and optionally OCR via Tess4J.
 */
public class PageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PageExtractor.class);
    private static final String OCR_LANGUAGE = "eng";

    private final PageExtractorOptions options;
    private Object tesseract; // Use Object instead of Tesseract to avoid class loading issues
    private boolean tesseractAvailable = true; // Track if Tesseract is available

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
        if (!tesseractAvailable) {
            logger.debug("Tesseract not available, skipping OCR for page {}", pageNumber);
            return "";
        }

        BufferedImage image = null;
        try {
            image = renderer.renderImageWithDPI(pageZeroBased, options.ocrDpi());

            Object tesseractInstance = getOrCreateTesseract();
            if (tesseractInstance == null) {
                logger.debug("Tesseract instance not available for page {}", pageNumber);
                return "";
            }

            // Use reflection to call doOCR method
            Class<?> tesseractClass = tesseractInstance.getClass();
            Object result = tesseractClass.getMethod("doOCR", BufferedImage.class).invoke(tesseractInstance, image);
            String ocrText = result != null ? result.toString() : "";
            logger.debug("OCR completed for page {} with {} characters", pageNumber, ocrText.length());
            return ocrText.trim();

        } catch (IOException e) {
            logger.warn("Failed to render page {} for OCR: {}", pageNumber, e.getMessage());
            logger.debug("Render failure details", e);
        } catch (NoSuchMethodException e) {
            logger.warn("Tesseract doOCR method not found - incompatible tess4j version");
            tesseractAvailable = false; // Disable further OCR attempts
        } catch (IllegalAccessException e) {
            logger.warn("Cannot access Tesseract doOCR method");
            tesseractAvailable = false;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                logger.warn("OCR processing failed on page {}: {} - {}", pageNumber, cause.getClass().getSimpleName(), cause.getMessage());
            } else {
                logger.warn("OCR processing failed on page {}: {}", pageNumber, e.getMessage());
            }
            logger.debug("OCR exception details", e);
        } catch (Exception e) {
            logger.warn("Unexpected error during OCR on page {}: {} - {}", pageNumber, e.getClass().getSimpleName(), e.getMessage());
            logger.debug("OCR exception details", e);
            tesseractAvailable = false; // Disable further OCR attempts after unexpected errors
        } finally {
            if (image != null) {
                image.flush();
            }
        }
        return "";
    }

    private synchronized Object getOrCreateTesseract() {
        if (tesseract == null && tesseractAvailable) {
            try {
                // Attempt to load Tesseract class
                Class<?> tesseractClass = Class.forName("net.sourceforge.tess4j.Tesseract");
                tesseract = tesseractClass.getDeclaredConstructor().newInstance();

                // Test if TessAPI can be initialized by attempting to load it
                try {
                    Class.forName("net.sourceforge.tess4j.TessAPI");
                    logger.debug("TessAPI class loaded successfully");
                } catch (ClassNotFoundException | NoClassDefFoundError | UnsatisfiedLinkError e) {
                    logger.warn("TessAPI native libraries not available; OCR will be unavailable: {}", e.getMessage());
                    tesseractAvailable = false;
                    tesseract = null;
                    return null;
                }

                // Set Tesseract options using reflection
                try {
                    tesseractClass.getMethod("setLanguage", String.class).invoke(tesseract, OCR_LANGUAGE);
                } catch (Exception e) {
                    logger.debug("Failed to set Tesseract language: {}", e.getMessage());
                }

                File dataPath = options.tessDataDir();
                if (dataPath == null) {
                    dataPath = autodetectTessDataDir();
                    if (dataPath != null) {
                        logger.debug("Auto-detected tessdata directory at: {}", dataPath.getAbsolutePath());
                    }
                }
                if (dataPath != null) {
                    try {
                        tesseractClass.getMethod("setDatapath", String.class).invoke(tesseract, dataPath.getAbsolutePath());
                    } catch (Exception e) {
                        logger.debug("Failed to set Tesseract data path: {}", e.getMessage());
                    }
                }

                logger.debug("Tesseract initialized successfully");
            } catch (ClassNotFoundException e) {
                logger.warn("Tesseract class not found; OCR will be unavailable. Ensure tess4j is in classpath.");
                tesseractAvailable = false;
                tesseract = null;
            } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
                logger.warn("Tesseract native libraries not found; OCR will be unavailable. Ensure Tesseract is installed: {}", e.getMessage());
                tesseractAvailable = false;
                tesseract = null;
            } catch (Exception e) {
                logger.warn("Failed to initialize Tesseract: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                logger.debug("Tesseract initialization details", e);
                tesseractAvailable = false;
                tesseract = null;
            }
        }
        return tesseract;
    }

    private File autodetectTessDataDir() {
        // Highest priority: environment variables
        String[] envKeys = new String[]{"TESSDATA_PREFIX", "TESSDATA_DIR"};
        for (String key : envKeys) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                File dir = new File(val);
                if (dir.isDirectory()) return dir;
                // Some set TESSDATA_PREFIX to parent of tessdata
                File sub = new File(dir, "tessdata");
                if (sub.isDirectory()) return sub;
            }
        }

        // Try common Homebrew locations on macOS
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            String[] candidates = new String[]{
                "/opt/homebrew/opt/tesseract/share/tessdata",
                "/opt/homebrew/share/tessdata",
                "/usr/local/opt/tesseract/share/tessdata",
                "/usr/local/share/tessdata"
            };
            for (String p : candidates) {
                File dir = new File(p);
                if (dir.isDirectory()) return dir;
            }
        }

        // Debian/Ubuntu typical
        String[] linuxCandidates = new String[]{
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/tessdata",
            "/usr/share/tessdata"
        };
        for (String p : linuxCandidates) {
            File dir = new File(p);
            if (dir.isDirectory()) return dir;
        }

        return null;
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
