package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFParserTest {

    @Test
    void testParseValidPDF(@TempDir Path tempDir) throws IOException {
        // Create a test PDF with 3 pages
        File testPdf = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(testPdf);
        }

        // Parse the PDF
        PDFParser parser = new PDFParser();
        PDFParser.ParseResult result = parser.parse(testPdf);

        // Verify result
        assertNotNull(result);
        assertEquals(3, result.getPageCount());
        assertEquals("test.pdf", result.getFileName());
    }

    @Test
    void testParseEmptyPDF(@TempDir Path tempDir) throws IOException {
        // Create a test PDF with no pages
        File testPdf = tempDir.resolve("empty.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.save(testPdf);
        }

        // Parse the PDF
        PDFParser parser = new PDFParser();
        PDFParser.ParseResult result = parser.parse(testPdf);

        // Verify result
        assertNotNull(result);
        assertEquals(0, result.getPageCount());
        assertEquals("empty.pdf", result.getFileName());
    }

    @Test
    void testParseNonExistentFile() {
        File nonExistent = new File("/nonexistent/file.pdf");
        PDFParser parser = new PDFParser();
        
        assertThrows(IOException.class, () -> parser.parse(nonExistent));
    }
}
