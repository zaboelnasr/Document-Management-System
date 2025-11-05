package com.dms.documentmanagementsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${s3.endpoint}")
    private String endpointsCsv;

    @Value("${s3.region:us-east-1}")
    private String region;

    @Value("${s3.accessKey}")
    private String accessKey;

    @Value("${s3.secretKey}")
    private String secretKey;

    @Value("${s3.pathStyleAccess:true}")
    private boolean pathStyle;

    /**
     * Build a single S3Client choosing the first endpoint that responds.
     * Endpoints are provided as CSV, e.g. "http://minio:9000,http://localhost:9000".
     */
    @Bean
    public S3Client s3Client() {
        List<String> candidates = Arrays.stream(endpointsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        S3Client lastTried = null;

        for (String ep : candidates) {
            try {
                S3Client client = baseClientFor(URI.create(ep));
                // quick connectivity probe; lightweight and fails fast if host is wrong
                client.listBuckets(); // if it throws, we try the next endpoint
                log.info("S3 endpoint selected: {}", ep);
                return client;
            } catch (SdkClientException ex) {
                log.warn("S3 endpoint not reachable yet: {} ({})", ep, ex.getMessage());
                // remember the last tried, we might return it if none works (to avoid NPEs)
                lastTried = baseClientFor(URI.create(ep));
            }
        }

        log.warn("No S3 endpoint responded; using the first candidate anyway: {}", candidates.isEmpty() ? "<none>" : candidates.get(0));
        return lastTried != null ? lastTried : baseClientFor(URI.create("http://localhost:9000"));
    }

    private S3Client baseClientFor(URI endpoint) {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of(region))
                .forcePathStyle(pathStyle)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
