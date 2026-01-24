package service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class XmlBatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(XmlBatchProcessor.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${schedule.input-folder:input-xml}")
    private String inputFolder;

    @Value("${schedule.archive-folder:archive-xml}")
    private String archiveFolder;

    public XmlBatchProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initFolders() throws Exception {
        Files.createDirectories(Path.of(inputFolder));
        Files.createDirectories(Path.of(archiveFolder));
    }

    // Run daily at 01:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void processXmlFiles() {
        File folder = new File(inputFolder);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".xml"));

        if (files == null || files.length == 0) {
            log.info("No XML files found to process.");
            return;
        }

        for (File file : files) {
            log.info("Processing file: {}", file.getName());
            try {
                processFile(file);
                Files.move(file.toPath(), Path.of(archiveFolder, file.getName()));
            } catch (Exception e) {
                log.error("Error processing file {}: {}", file.getName(), e.getMessage());
            }
        }
    }

    private void processFile(File file) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList documents = doc.getElementsByTagName("document");

        for (int i = 0; i < documents.getLength(); i++) {
            Element document = (Element) documents.item(i);
            String fileName = document.getElementsByTagName("file_name").item(0).getTextContent();

            jdbcTemplate.update("""
                INSERT INTO document_access_log (document_name, access_date, access_count)
                VALUES (?, ?, ?)
                ON CONFLICT (document_name, access_date)
                DO UPDATE SET access_count = document_access_log.access_count + EXCLUDED.access_count
            """, fileName, LocalDateTime.now().toLocalDate(), 1);
        }
    }
}
