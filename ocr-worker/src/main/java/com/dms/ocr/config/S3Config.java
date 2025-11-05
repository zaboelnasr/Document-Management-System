package com.dms.ocr.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
    @Value("${s3.endpoint}") private URI endpoint;
    @Value("${s3.region:us-east-1}") private String region;
    @Value("${s3.accessKey}") private String accessKey;
    @Value("${s3.secretKey}") private String secretKey;
    @Value("${s3.pathStyleAccess:true}") private boolean pathStyle;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of(region))
                .forcePathStyle(pathStyle)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
