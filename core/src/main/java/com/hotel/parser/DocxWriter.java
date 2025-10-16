package com.hotel.parser;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * DOCX Writer for creating Word documents from parsed PDF data, including
 * per-page sections and a summary table placeholder.
 */
public class DocxWriter {
    private static final Logger logger = LoggerFactory.getLogger(DocxWriter.class);

    /**
     * Write parse result to a DOCX file.
     *
     * @param result the parse result to write
     * @param outputFile the output DOCX file
     * @throws IOException if the file cannot be written
     */
    public void write(PDFParser.ParseResult result, File outputFile) throws IOException {
        logger.info("Writing result to DOCX file: {}", outputFile.getAbsolutePath());
        
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputFile)) {
            
            // Create title
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            String documentTitle = result.getTitle().isBlank()
                    ? "Hotel Contract Parser - Report"
                    : result.getTitle();
            titleRun.setText(documentTitle);
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            // Add blank line
            document.createParagraph();

            // Summary table placeholder
            XWPFParagraph summaryHeading = document.createParagraph();
            XWPFRun summaryRun = summaryHeading.createRun();
            summaryRun.setText("Summary");
            summaryRun.setBold(true);

            XWPFTable summaryTable = document.createTable(1, 1);
            XWPFTableRow headerRow = summaryTable.getRow(0);
            headerRow.getCell(0).setText("Summary");

            document.createParagraph();

            for (PageExtractor.Page page : result.getPages()) {
                XWPFParagraph pageHeading = document.createParagraph();
                XWPFRun headingRun = pageHeading.createRun();
                headingRun.setBold(true);
                headingRun.setText(String.format("Page %d", page.pageNumber()));

                XWPFParagraph pageTextParagraph = document.createParagraph();
                XWPFRun textRun = pageTextParagraph.createRun();
                writeMultilineText(textRun, page.text());

                document.createParagraph();
            }

            document.write(out);
            logger.info("Successfully wrote DOCX file");
        }
    }

    private void writeMultilineText(XWPFRun run, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String[] lines = text.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
    }
}
