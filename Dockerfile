FROM eclipse-temurin:17

WORKDIR /app

# Install Tesseract OCR with English language data
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       tesseract-ocr \
       tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy the shaded JAR
COPY cli/target/cli.jar /app/hotel-contract-parser.jar

# Create directory for input/output files
RUN mkdir -p /data

WORKDIR /data

ENTRYPOINT ["java", "-jar", "/app/hotel-contract-parser.jar"]
CMD ["--help"]
