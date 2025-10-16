package com.hotel.cli;

import com.hotel.parser.DocxWriter;
import com.hotel.parser.PageExtractorOptions;
import com.hotel.parser.PDFParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.*;

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

    @Option(
        names = "--dpi",
        description = "DPI used for rasterizing pages before OCR (default: ${DEFAULT-VALUE})",
        defaultValue = "300"
    )
    private int ocrDpi = 300;

    @Option(
        names = "--tess-data-dir",
        description = "Directory containing Tesseract traineddata files",
        paramLabel = "DIR"
    )
    private File tessDataDir;

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

        if (ocrDpi <= 0) {
            System.err.println("Error: --dpi must be a positive integer");
            return 1;
        }
        if (tessDataDir != null && !tessDataDir.isDirectory()) {
            System.err.println("Error: --tess-data-dir must point to an existing directory");
            return 1;
        }

        System.out.println("Parsing PDF: " + inputFile.getAbsolutePath());

        // Parse PDF
        PageExtractorOptions options = new PageExtractorOptions(
            ocrDpi,
            tessDataDir == null ? null : tessDataDir.getAbsoluteFile(),
            PageExtractorOptions.DEFAULT_MIN_NATIVE_TEXT_LENGTH
        );
        PDFParser parser = new PDFParser(options);
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

    private static void configureMacHomebrewNativeLibs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("mac")) return;

        String key = "jna.library.path";
        String existing = System.getProperty(key, "");

        // Common Homebrew locations (Apple Silicon and Intel)
        List<String> candidates = Arrays.asList(
            "/opt/homebrew/opt/tesseract/lib",
            "/opt/homebrew/opt/leptonica/lib",
            "/opt/homebrew/lib",
            "/usr/local/opt/tesseract/lib",
            "/usr/local/opt/leptonica/lib",
            "/usr/local/lib"
        );

        List<String> present = new ArrayList<>();
        for (String p : candidates) {
            if (new File(p).isDirectory()) {
                present.add(p);
            }
        }
        if (present.isEmpty()) return; // nothing to add

        if (existing == null || existing.isBlank()) {
            System.setProperty(key, String.join(File.pathSeparator, present));
        } else {
            // Append any missing ones
            Set<String> parts = new LinkedHashSet<>(Arrays.asList(existing.split(java.util.regex.Pattern.quote(File.pathSeparator))));
            parts.addAll(present);
            System.setProperty(key, String.join(File.pathSeparator, parts));
        }
    }

    public static void main(String[] args) {
        // Configure Homebrew Tesseract native libraries on macOS so Tess4J can load them
        configureMacHomebrewNativeLibs();

        int exitCode = new CommandLine(new HotelContractParserCLI()).execute(args);
        System.exit(exitCode);
    }
}
