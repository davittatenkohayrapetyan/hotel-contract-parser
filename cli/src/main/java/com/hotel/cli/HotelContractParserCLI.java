package com.hotel.cli;

import com.hotel.parser.DocxWriter;
import com.hotel.parser.PDFParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * CLI application for parsing hotel contract PDFs.
 */
@Command(
    name = "parse",
    description = "Parse a hotel contract PDF and generate a DOCX report",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class HotelContractParserCLI implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Input PDF file to parse"
    )
    private File inputFile;

    @Option(
        names = {"-o", "--output"},
        description = "Output DOCX file (default: out.docx)",
        defaultValue = "out.docx"
    )
    private File outputFile;

    @Override
    public Integer call() throws Exception {
        // Validate input file
        if (!inputFile.exists()) {
            System.err.println("Error: Input file does not exist: " + inputFile.getAbsolutePath());
            return 1;
        }
        if (!inputFile.isFile()) {
            System.err.println("Error: Input path is not a file: " + inputFile.getAbsolutePath());
            return 1;
        }
        if (!inputFile.getName().toLowerCase().endsWith(".pdf")) {
            System.err.println("Error: Input file must be a PDF: " + inputFile.getAbsolutePath());
            return 1;
        }

        System.out.println("Parsing PDF: " + inputFile.getAbsolutePath());
        
        // Parse PDF
        PDFParser parser = new PDFParser();
        PDFParser.ParseResult result = parser.parse(inputFile);
        
        // Print page count
        System.out.println("Page count: " + result.getPageCount());
        
        // Write output
        System.out.println("Writing output to: " + outputFile.getAbsolutePath());
        DocxWriter writer = new DocxWriter();
        writer.write(result, outputFile);
        
        System.out.println("Done!");
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HotelContractParserCLI()).execute(args);
        System.exit(exitCode);
    }
}
