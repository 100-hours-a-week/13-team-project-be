package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.settlement.config.AppS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3PresignedGetUrlService {

    private final S3Presigner s3Presigner;
    private final AppS3Properties s3Props;

    public String presignGet(String objectKey, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Props.getBucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();

        return s3Presigner.presignGetObject(req).url().toString();
    }
}