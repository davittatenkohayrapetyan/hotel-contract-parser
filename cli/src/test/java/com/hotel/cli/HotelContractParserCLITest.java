package com.hotel.cli;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HotelContractParserCLITest {

    @Test
    void testParseCommand(@TempDir Path tempDir) throws IOException {
        // Create a test PDF
        File testPdf = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(testPdf);
        }

        // Run the command
        File outputFile = tempDir.resolve("output.docx").toFile();
        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute(testPdf.getAbsolutePath(), "-o", outputFile.getAbsolutePath());

        // Verify success
        assertEquals(0, exitCode);
        assertTrue(outputFile.exists());
    }

    @Test
    void testParseNonExistentFile(@TempDir Path tempDir) {
        File outputFile = tempDir.resolve("output.docx").toFile();
        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute("/nonexistent/file.pdf", "-o", outputFile.getAbsolutePath());

        // Verify failure
        assertEquals(1, exitCode);
        assertFalse(outputFile.exists());
    }

    @Test
    void testParseNonPdfFile(@TempDir Path tempDir) throws IOException {
        // Create a non-PDF file
        File txtFile = tempDir.resolve("test.txt").toFile();
        txtFile.createNewFile();

        File outputFile = tempDir.resolve("output.docx").toFile();
        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute(txtFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath());

        // Verify failure
        assertEquals(1, exitCode);
        assertFalse(outputFile.exists());
    }

    @Test
    void testInvalidDpiOption(@TempDir Path tempDir) throws IOException {
        File testPdf = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(testPdf);
        }

        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute(testPdf.getAbsolutePath(), "--dpi", "0");

        assertEquals(1, exitCode);
    }

    @Test
    void testInvalidTessDataDir(@TempDir Path tempDir) throws IOException {
        File testPdf = tempDir.resolve("test.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(testPdf);
        }

        File missingDir = tempDir.resolve("missing").toFile();
        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute(testPdf.getAbsolutePath(), "--tess-data-dir", missingDir.getAbsolutePath());

        assertEquals(1, exitCode);
    }

    @Test
    void testHelpOption() {
        HotelContractParserCLI cli = new HotelContractParserCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute("--help");

        // Verify success (help returns 0)
        assertEquals(0, exitCode);
    }
}
