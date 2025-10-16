package com.hotel.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocxWriterTest {

    @Test
    void testWriteDocx(@TempDir Path tempDir) throws IOException {
        // Create a parse result
        PDFParser.ParseResult result = new PDFParser.ParseResult(
                List.of(
                        new PageExtractor.Page(1, "Content for page 1"),
                        new PageExtractor.Page(2, "Content for page 2"),
                        new PageExtractor.Page(3, "Content for page 3"),
                        new PageExtractor.Page(4, "Content for page 4"),
                        new PageExtractor.Page(5, "Content for page 5")
                ),
                "test.pdf",
                "Test Document");

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
        PDFParser.ParseResult result = new PDFParser.ParseResult(
                List.of(new PageExtractor.Page(1, "Content")),
                "test.pdf",
                "Test Document");
        File invalidFile = new File("/nonexistent/directory/output.docx");
        DocxWriter writer = new DocxWriter();

        assertThrows(IOException.class, () -> writer.write(result, invalidFile));
    }
}
