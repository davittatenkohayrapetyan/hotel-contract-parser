package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocxWriterTest {

    @Test
    void testWriteDocx(@TempDir Path tempDir) throws IOException {
        // Create a parse result
        PDFParser.ParseResult result = new PDFParser.ParseResult(5, "test.pdf");

        // Write to DOCX
        File outputFile = tempDir.resolve("output.docx").toFile();
        DocxWriter writer = new DocxWriter();
        writer.write(result, outputFile);

        // Verify file was created and is valid
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        // Verify we can read it as a DOCX
        try (FileInputStream fis = new FileInputStream(outputFile);
             XWPFDocument doc = new XWPFDocument(fis)) {
            assertNotNull(doc);
            assertTrue(doc.getParagraphs().size() > 0);
        }
    }

    @Test
    void testWriteToInvalidDirectory() {
        PDFParser.ParseResult result = new PDFParser.ParseResult(3, "test.pdf");
        File invalidFile = new File("/nonexistent/directory/output.docx");
        DocxWriter writer = new DocxWriter();

        assertThrows(IOException.class, () -> writer.write(result, invalidFile));
    }
}
