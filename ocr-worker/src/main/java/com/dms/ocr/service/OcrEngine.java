package com.dms.ocr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

@Service
public class OcrEngine {
    private static final Logger log = LoggerFactory.getLogger(OcrEngine.class);

    public String extractText(Path pdf, Long documentId) throws Exception {
        // 1) render PDF -> PNG
        Path outDir = Files.createTempDirectory("ocr-" + documentId);
        Process gs = new ProcessBuilder(
                "gs", "-dSAFER", "-dBATCH", "-dNOPAUSE",
                "-sDEVICE=png16m", "-r300",
                "-sOutputFile=" + outDir.resolve("page-%03d.png"),
                pdf.toString()
        ).redirectErrorStream(true).start();

        if (gs.waitFor() != 0) {
            log.error("Ghostscript failed for doc id={}", documentId);
            return "";
        }

        // 2) OCR
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.list(outDir)) {
            List<Path> pages = files
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .toList();

            for (Path page : pages) {
                Process p = new ProcessBuilder(
                        "tesseract", page.toString(), "stdout", "-l", "eng+deu"
                ).redirectErrorStream(true).start();

                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    br.lines().forEach(line -> all.append(line).append('\n'));
                }
                p.waitFor();
            }
        }

        return all.toString().trim();
    }
}
