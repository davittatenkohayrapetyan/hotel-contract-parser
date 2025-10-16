# hotel-contract-parser

Java CLI to parse hotel-vendor contracts (PDF). Extracts basic metadata and page count from PDF files, generating a DOCX report.

## Features

- Opens and parses PDF files using Apache PDFBox
- Iterates through all pages and reports page count
- Generates DOCX output using Apache POI
- Command-line interface using picocli
- Dockerized for easy deployment
- Java 17 compatible

## Requirements

- Java 17 or higher
- Maven 3.6 or higher (for building)

## Building

Build the project using Maven:

```bash
mvn clean install
```

This creates a shaded JAR in `cli/target/cli.jar` that includes all dependencies.

## Usage

### Command Line

Parse a PDF file and generate a DOCX report:

```bash
java -jar cli/target/cli.jar <input.pdf> -o <output.docx>
```

Example:

```bash
java -jar cli/target/cli.jar docs/sample.pdf -o report.docx
```

Show help:

```bash
java -jar cli/target/cli.jar --help
```

### Docker

Build the Docker image:

```bash
mvn clean install
docker build -t hotel-contract-parser .
```

Run with Docker:

```bash
docker run -v $(pwd)/docs:/data hotel-contract-parser sample.pdf -o output.docx
```

Show help:

```bash
docker run hotel-contract-parser --help
```

## Project Structure

- `core/` - Core parsing library using PDFBox and POI
- `cli/` - Command-line interface using picocli
- `docs/` - Sample PDF files
- `.github/workflows/` - GitHub Actions CI/CD configuration

## Dependencies

- **PDFBox 3.0.3** - PDF parsing and manipulation
- **Apache POI 5.3.0** - DOCX generation
- **picocli 4.7.6** - Command-line interface
- **SLF4J 2.0.16** - Logging facade
- **JUnit Jupiter 5.10.3** - Testing framework

## CI/CD

GitHub Actions automatically builds and tests the project on every push and pull request using:

```bash
mvn -B verify
```

## License

This project is open source and available under standard terms.

