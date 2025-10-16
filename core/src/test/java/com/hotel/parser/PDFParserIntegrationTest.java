package com.hotel.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFParserIntegrationTest {

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanup() throws IOException {
        for (Path path : tempFiles) {
            Files.deleteIfExists(path);
        }
        tempFiles.clear();
    }

    @Test
    void parseExtractsTextAcrossThreeToFivePages() throws IOException {
        PDFParser parser = new PDFParser();

        for (int pageCount = 3; pageCount <= 5; pageCount++) {
            Path pdfPath = createSamplePdf(pageCount);
            PDFParser.ParseResult result = parser.parse(pdfPath.toFile());

            assertEquals("Test Document", result.getTitle());
            assertEquals(pageCount, result.getPageCount());
            assertEquals(pageCount, result.getPages().size());

            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                String expectedSnippet = "This is page " + pageIndex;
                PageExtractor.Page page = result.getPages().get(pageIndex - 1);
                assertEquals(pageIndex, page.pageNumber());
                assertTrue(page.text().contains(expectedSnippet));
            }
        }
    }

    @Test
    void docxWriterProducesSummaryTableAndPageSections() throws IOException {
        PDFParser parser = new PDFParser();
        Path pdfPath = createSamplePdf(3);
        PDFParser.ParseResult result = parser.parse(pdfPath.toFile());

        Path docxPath = Files.createTempFile("pdf-parser-test-", ".docx");
        tempFiles.add(docxPath);

        File docxFile = docxPath.toFile();
        new DocxWriter().write(result, docxFile);

        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(docxPath))) {
            List<String> paragraphTexts = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .toList();

            assertEquals("Test Document", paragraphTexts.get(0));
            assertTrue(paragraphTexts.contains("Summary"));
            assertTrue(paragraphTexts.contains("Page 1"));
            assertTrue(paragraphTexts.contains("Page 2"));
            assertTrue(paragraphTexts.contains("Page 3"));

            List<XWPFTable> tables = doc.getTables();
            assertEquals(1, tables.size());
            assertEquals("Summary", tables.get(0).getRow(0).getCell(0).getText());
        }
    }

    private Path createSamplePdf(int pageCount) throws IOException {
        Path pdfPath = Files.createTempFile("pdf-parser-test-", ".pdf");
        tempFiles.add(pdfPath);

        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle("Test Document");
            document.setDocumentInformation(info);

            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.setLeading(14);
                    contentStream.newLineAtOffset(72, 700);
                    contentStream.showText("This is page " + pageIndex);
                    contentStream.newLine();
                    contentStream.showText("Line two for page " + pageIndex);
                    contentStream.endText();
                }
            }

            document.save(pdfPath.toFile());
        }

        return pdfPath;
    }
}
