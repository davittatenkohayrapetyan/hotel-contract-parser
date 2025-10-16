package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PDFParserTest {

    @Test
    void testParseValidPDF(@TempDir Path tempDir) throws IOException {
        // Create a test PDF with 3 pages
        File testPdf = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle("Test Title");
            doc.setDocumentInformation(info);

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
        assertEquals("Test Title", result.getTitle());
        assertEquals(3, result.getPages().size());
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
        assertEquals(0, result.getPages().size());
    }

    @Test
    void testParseNonExistentFile() {
        File nonExistent = new File("/nonexistent/file.pdf");
        PDFParser parser = new PDFParser();

        assertThrows(IOException.class, () -> parser.parse(nonExistent));
    }
}
