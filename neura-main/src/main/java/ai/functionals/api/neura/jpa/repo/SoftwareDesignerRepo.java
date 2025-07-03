package ai.functionals.api.neura.jpa.repo;

import ai.functionals.api.neura.jpa.entity.SoftwareDesigner;
import ai.functionals.api.neura.model.enums.DocSessionStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareDesignerRepo extends JpaRepository<SoftwareDesigner, Long> {
    Optional<SoftwareDesigner> findBySessionId(String sessionId);
    Page<SoftwareDesigner> findAllByTenantIdAndOwnerId(String tenantId, Long owenerId, Pageable pageable);

    List<SoftwareDesigner> findByTenantIdAndOwnerIdAndSessionId(String tenantId, Long owenerId, String sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE SoftwareDesigner sd SET sd.docSessionStatus = :docSessionStatus WHERE sd.tenantId = :tenantId AND sd.sessionId = :sessionId")
    void updateDocSessionStatus(String tenantId, String sessionId, DocSessionStatus docSessionStatus);
}
