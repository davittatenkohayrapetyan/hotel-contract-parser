package com.hotel.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * DOCX Writer for creating Word documents from parsed PDF data.
 * This is a stub implementation that creates a basic document.
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
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Hotel Contract Parser - Report");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            
            // Add blank line
            document.createParagraph();
            
            // Add source file info
            XWPFParagraph fileInfo = document.createParagraph();
            XWPFRun fileRun = fileInfo.createRun();
            fileRun.setText("Source File: " + result.getFileName());
            
            // Add page count
            XWPFParagraph pageInfo = document.createParagraph();
            XWPFRun pageRun = pageInfo.createRun();
            pageRun.setText("Total Pages: " + result.getPageCount());
            
            document.write(out);
            logger.info("Successfully wrote DOCX file");
        }
    }
}
