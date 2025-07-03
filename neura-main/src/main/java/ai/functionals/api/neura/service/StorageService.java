package ai.functionals.api.neura.service;

public interface StorageService {
    String getDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion);

    void putDoc(String docsBucket, String tenantId, String sessionId, Integer documentVersion, String productDesign);

    void deleteDocs(String docsBucket, String tenantId, String sessionId);
}
