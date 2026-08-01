package com.itau.ingestor.config;

import com.itau.ingestor.consumer.SqsQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region}") String region,
                               @Value("${aws.sqs.endpoint:}") String endpoint,
                               @Value("${aws.sqs.access-key}") String accessKey,
                               @Value("${aws.sqs.secret-key}") String secretKey) {

        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public SqsQueue sqsQueue(SqsClient sqsClient,
                             @Value("${aws.sqs.queue-name}") String queueName) {
        return new SqsQueue(sqsClient, queueName);
    }
}
