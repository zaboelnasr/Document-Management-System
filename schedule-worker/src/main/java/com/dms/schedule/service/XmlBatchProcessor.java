package com.dms.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XmlBatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(XmlBatchProcessor.class);

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ResourcePatternResolver resourceResolver;
    private final ObjectMapper objectMapper;

    @Value("${schedule.input-pattern:}")
    private String inputPattern;

    @Value("${schedule.input-folder:classpath:/xml}")
    private String inputFolder;

    @Value("${schedule.file-pattern:*.xml}")
    private String filePattern;

    @Value("${schedule.archive-folder:archive-xml}")
    private String archiveFolder;

    @Value("${schedule.batch-size:100}")
    private int batchSize;

    @Value("${schedule.reindex.enabled:true}")
    private boolean reindexEnabled;

    @Value("${schedule.reindex.url:http://backend:8080/api/admin/reindex}")
    private String reindexUrl;

    @Value("${dms.rmq.exchange:dms.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.index:dms.document.index}")
    private String indexRoutingKey;

    public XmlBatchProcessor(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initFolders() throws Exception {
        String pattern = resolvePattern();
        if (!isClasspathSource(pattern)) {
            Path inputPath = resolveInputDirectory(pattern);
            if (inputPath != null) {
                Files.createDirectories(inputPath);
            }
        }
        Files.createDirectories(Path.of(archiveFolder));
    }

    public BatchResult processXmlBatch() {
        String pattern = resolvePattern();
        boolean classpathSource = isClasspathSource(pattern);
        int processedFiles = 0;
        int processedDocuments = 0;

        try {
            Resource[] resources = resourceResolver.getResources(pattern);
            if (resources.length == 0) {
                log.info("No XML resources found for pattern: {}", pattern);
                return new BatchResult(0, 0);
            }

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) {
                    log.warn("Skipping resource with no filename for pattern: {}", pattern);
                    continue;
                }

                if (isAlreadyProcessed(fileName)) {
                    log.info("Skipping already processed resource: {}", fileName);
                    continue;
                }

                log.info("Processing XML resource: {}", fileName);
                try (InputStream inputStream = resource.getInputStream()) {
                    List<DocumentRecord> records = parseDocumentRecords(inputStream);
                    if (records.isEmpty()) {
                        log.warn("No document records found in {}", fileName);
                        continue;
                    }
                    batchInsertDocuments(records);
                    upsertReviews(records);
                    upsertAccessLogs(records);
                    publishIndexMessages(records);
                    processedFiles += 1;
                    processedDocuments += records.size();
                    archiveResource(resource, fileName, classpathSource);
                } catch (Exception e) {
                    log.error("Failed to process {}: {}", fileName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve XML resources for pattern {}: {}", pattern, e.getMessage());
        }

        if (processedFiles > 0 && reindexEnabled) {
            triggerReindex();
        }

        return new BatchResult(processedFiles, processedDocuments);
    }

    private String resolvePattern() {
        if (inputPattern != null && !inputPattern.isBlank()) {
            return inputPattern;
        }

        if (inputFolder.startsWith("classpath:") || inputFolder.startsWith("classpath*:")) {
            String base = inputFolder.endsWith("/") ? inputFolder : inputFolder + "/";
            return base + filePattern;
        }

        Path folderPath = Path.of(inputFolder);
        String base = folderPath.toUri().toString();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + filePattern;
    }

    private boolean isClasspathSource(String pattern) {
        return pattern.startsWith("classpath:") || pattern.startsWith("classpath*:");
    }

    private Path resolveInputDirectory(String pattern) {
        if (pattern.startsWith("file:")) {
            int lastSlash = pattern.lastIndexOf('/');
            if (lastSlash <= "file:".length()) {
                return null;
            }
            String base = pattern.substring(0, lastSlash + 1);
            try {
                return Path.of(URI.create(base));
            } catch (Exception ex) {
                return null;
            }
        }
        return Path.of(inputFolder);
    }

    private boolean isAlreadyProcessed(String fileName) {
        Path marker = Path.of(archiveFolder, fileName + ".processed");
        return Files.exists(marker);
    }

    private void archiveResource(Resource resource, String fileName, boolean classpathSource) throws Exception {
        Path archiveDir = Path.of(archiveFolder);
        Files.createDirectories(archiveDir);

        if (!classpathSource && resource.isFile()) {
            Path source = resource.getFile().toPath();
            Files.move(source, archiveDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Path marker = archiveDir.resolve(fileName + ".processed");
        Files.writeString(marker, "Processed at " + LocalDateTime.now());
    }

    private List<DocumentRecord> parseDocumentRecords(InputStream inputStream) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();

        NodeList documents = doc.getElementsByTagName("document");
        List<DocumentRecord> records = new ArrayList<>();

        for (int i = 0; i < documents.getLength(); i++) {
            Node node = documents.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;

            String fileName = getDirectChildText(element, "file_name");
            if (fileName == null || fileName.isBlank()) {
                continue;
            }

            DocumentRecord record = new DocumentRecord();
            record.fileName = fileName;
            record.summary = getDirectChildText(element, "summary");
            record.bucket = getDirectChildText(element, "bucket");
            record.objectKey = getDirectChildText(element, "object_key");
            record.contentType = getDirectChildText(element, "content_type");
            record.size = parseLong(getDirectChildText(element, "size"));
            record.createdAt = parseDateTime(getDirectChildText(element, "created_at"));
            record.updatedAt = parseDateTime(getDirectChildText(element, "updated_at"));
            record.accessDate = parseDate(getDirectChildText(element, "access_date"));
            record.accessCount = parseInteger(getDirectChildText(element, "access_count"));

            Element reviewElement = getDirectChildElement(element, "review");
            if (reviewElement != null) {
                record.reviewStatus = getDirectChildText(reviewElement, "status");
                record.reviewUpdatedAt = parseDateTime(getDirectChildText(reviewElement, "updated_at"));
            }

            records.add(record);
        }

        return records;
    }

    private void batchInsertDocuments(List<DocumentRecord> records) {
        String insertSql = """
            INSERT INTO documents (
                file_name, summary, bucket, object_key, content_type, size, content, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        for (int start = 0; start < records.size(); start += batchSize) {
            int end = Math.min(start + batchSize, records.size());
            List<DocumentRecord> batch = records.subList(start, end);

            jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DocumentRecord record = batch.get(i);
                    ps.setString(1, record.fileName);
                    ps.setString(2, record.summary);
                    ps.setString(3, record.bucket);
                    ps.setString(4, record.objectKey);
                    ps.setString(5, record.contentType);
                    if (record.size != null) {
                        ps.setLong(6, record.size);
                    } else {
                        ps.setNull(6, Types.BIGINT);
                    }
                    ps.setNull(7, Types.BIGINT);
                    if (record.createdAt != null) {
                        ps.setTimestamp(8, Timestamp.valueOf(record.createdAt));
                    } else {
                        ps.setNull(8, Types.TIMESTAMP);
                    }
                    if (record.updatedAt != null) {
                        ps.setTimestamp(9, Timestamp.valueOf(record.updatedAt));
                    } else {
                        ps.setNull(9, Types.TIMESTAMP);
                    }
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        }
    }

    private void upsertReviews(List<DocumentRecord> records) {
        String reviewSql = """
            INSERT INTO document_reviews (document_id, status, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT (document_id)
            DO UPDATE SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at
            """;

        for (DocumentRecord record : records) {
            if (record.reviewStatus == null || record.reviewStatus.isBlank()) {
                continue;
            }

            Long documentId = jdbcTemplate.queryForObject(
                "SELECT id FROM documents WHERE file_name = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                record.fileName
            );

            if (documentId == null) {
                continue;
            }

            LocalDateTime reviewUpdatedAt =
                record.reviewUpdatedAt != null ? record.reviewUpdatedAt : LocalDateTime.now();

            jdbcTemplate.update(
                reviewSql,
                documentId,
                record.reviewStatus,
                Timestamp.valueOf(reviewUpdatedAt)
            );
        }
    }

    private void upsertAccessLogs(List<DocumentRecord> records) {
        for (DocumentRecord record : records) {
            if (record.accessDate == null || record.accessCount == null) {
                continue;
            }

            Long documentId = jdbcTemplate.queryForObject(
                "SELECT id FROM documents WHERE file_name = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                record.fileName
            );

            if (documentId == null) {
                continue;
            }

            jdbcTemplate.update(
                """
                INSERT INTO document_access_log (document_id, access_date, access_count)
                VALUES (?, ?, ?)
                ON CONFLICT (document_id, access_date)
                DO UPDATE SET access_count = EXCLUDED.access_count
                """,
                documentId,
                Date.valueOf(record.accessDate),
                record.accessCount
            );
        }
    }

    private void publishIndexMessages(List<DocumentRecord> records) {
        for (DocumentRecord record : records) {
            Long documentId = jdbcTemplate.queryForObject(
                "SELECT id FROM documents WHERE file_name = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                record.fileName
            );

            if (documentId == null) {
                continue;
            }

            Map<String, Object> message = new HashMap<>();
            message.put("documentId", documentId);
            message.put("fileName", record.fileName);
            message.put("content", "");
            message.put("summary", record.summary);
            message.put("createdAt", record.createdAt != null ? record.createdAt.toString() : null);
            message.put("reviewStatus", record.reviewStatus);

            try {
                String payload = objectMapper.writeValueAsString(message);
                rabbitTemplate.convertAndSend(exchange, indexRoutingKey, payload);
            } catch (Exception e) {
                log.warn("Failed to publish index message for document id={}", documentId);
            }
        }
    }

    private void triggerReindex() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(reindexUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Triggered reindex successfully.");
            } else {
                log.warn("Reindex request returned status {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to trigger reindex: {}", e.getMessage());
        }
    }

    private Element getDirectChildElement(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (tag.equals(element.getTagName())) {
                    return element;
                }
            }
        }
        return null;
    }

    private String getDirectChildText(Element parent, String tag) {
        Element child = getDirectChildElement(parent, tag);
        return child == null ? null : child.getTextContent();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static class DocumentRecord {
        private String fileName;
        private String summary;
        private String bucket;
        private String objectKey;
        private String contentType;
        private Long size;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String reviewStatus;
        private LocalDateTime reviewUpdatedAt;
        private LocalDate accessDate;
        private Integer accessCount;
    }

    public static class BatchResult {
        private final int processedFiles;
        private final int processedDocuments;

        public BatchResult(int processedFiles, int processedDocuments) {
            this.processedFiles = processedFiles;
            this.processedDocuments = processedDocuments;
        }

        public int getProcessedFiles() {
            return processedFiles;
        }

        public int getProcessedDocuments() {
            return processedDocuments;
        }
    }
}
