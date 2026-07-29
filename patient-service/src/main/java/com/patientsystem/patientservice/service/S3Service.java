package com.patientsystem.patientservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import com.patientsystem.patientservice.dto.CompletedPartDTO;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public void upload(String key, byte[] bytes) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromBytes(bytes)
        );
    }

    public InputStream download(String key) {
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public String generatePresignedPutUrl(String key) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(2))
                .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String initiateMultipartUpload(String key) {
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(bucket).key(key).build()
        );
        return response.uploadId();
    }

    public String generatePresignedPartUrl(String key, String uploadId, int partNumber) {
        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
            .signatureDuration(Duration.ofHours(12))
            .uploadPartRequest(UploadPartRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId).partNumber(partNumber).build())
            .build();
        return s3Presigner.presignUploadPart(presignRequest).url().toString();
    }

    public void completeMultipartUpload(String key, String uploadId, List<CompletedPartDTO> parts) {
        List<CompletedPart> completedParts = parts.stream()
            .map(p -> CompletedPart.builder().partNumber(p.getPartNumber()).eTag(p.getEtag()).build())
            .collect(Collectors.toList());
        s3Client.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build()
        );
    }
}
