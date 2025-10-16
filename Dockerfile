FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Tesseract OCR with English language data
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng

# Copy the shaded JAR
COPY cli/target/cli.jar /app/hotel-contract-parser.jar

# Create directory for input/output files
RUN mkdir -p /data

WORKDIR /data

ENTRYPOINT ["java", "-jar", "/app/hotel-contract-parser.jar"]
CMD ["--help"]
