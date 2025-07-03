package ai.functionals.api.neura.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ai.functionals.api.neura.util.AppUtil.getS3ObjectKey;

@Slf4j
@Service
@Profile("local")
@RequiredArgsConstructor
public class StorageServiceHDD implements StorageService {

    @Override
    public String getDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion) {
        String key = getS3ObjectKey(tenantId, sessionId, documentVersion);
        try {
            log.info("Reading from HDD with key: {}", key);
            return Files.readString(Paths.get(System.getProperty("user.home"), key));
        } catch (IOException e) {
            log.error("Error reading document from HDD with key '{}'", key, e);
            return null;
        }
    }

    public void putDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion, String productDesign) {
        String key = getS3ObjectKey(tenantId, sessionId, documentVersion);
        try {
            if (StringUtils.isBlank(productDesign)) return;
            Path path = Paths.get(System.getProperty("user.home"), key);
            Files.createDirectories(path.getParent());
            Files.writeString(path, productDesign);
            log.info("Document uploaded to HDD with key: {}", key);
        } catch (Exception e) {
            log.error("Error uploading document to HDD with key '{}'", key, e);
        }
    }

    public void deleteDocs(String docsBucket, String tenantId, String sessionId) {
        String key = getS3ObjectKey(tenantId, sessionId, null);
        try {
            Path path = Paths.get(System.getProperty("user.home"), key);
            Files.deleteIfExists(path);
            log.info("Documents deleted from HDD with key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting documents from HDD with key '{}'", key, e);
        }
    }
}