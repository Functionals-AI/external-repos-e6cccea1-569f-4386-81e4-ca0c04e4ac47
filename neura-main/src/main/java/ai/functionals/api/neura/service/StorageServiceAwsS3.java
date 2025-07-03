package ai.functionals.api.neura.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static ai.functionals.api.neura.util.AppUtil.getS3ObjectKey;

@Slf4j
@Service
@Profile({"test", "prod"})
@RequiredArgsConstructor
public class StorageServiceAwsS3 implements StorageService {

    private final S3Client s3Client;

    @Override
    public String getDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion) {
        String key = getS3ObjectKey(tenantId, sessionId, documentVersion);
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(docsBucket)
                        .key(key)
                        .build()
        )) {
            byte[] bytes = s3Object.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("I/O error reading S3 object '{}'", key, e);
        } catch (Exception e) {
            log.error("Error fetching document from S3 with key '{}'", key, e);
        }
        return null;
    }

    @Override
    public void putDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion, String productDesign) {
        if (StringUtils.isBlank(productDesign)) {
            log.warn("Product design is empty, skipping upload to S3: {}", sessionId);
            return;
        }
        String key = getS3ObjectKey(tenantId, sessionId, documentVersion);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(docsBucket)
                    .key(key)
                    .build();
            s3Client.putObject(request, RequestBody.fromString(productDesign));
            log.info("Document uploaded to S3 with key: {}", key);
        } catch (Exception e) {
            log.error("Error uploading document to S3 with key '{}'", key, e);
        }
    }

    @Override
    public void deleteDocs(String docsBucket, String tenantId, String sessionId) {
        String key = getS3ObjectKey(tenantId, sessionId, null);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(docsBucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Documents deleted from S3 with key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting documents from S3 with key '{}'", key, e);
        }
    }
}