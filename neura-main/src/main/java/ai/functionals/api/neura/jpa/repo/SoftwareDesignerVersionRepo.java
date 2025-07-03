package ai.functionals.api.neura.jpa.repo;

import ai.functionals.api.neura.jpa.entity.SoftwareDesignerVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface SoftwareDesignerVersionRepo extends JpaRepository<SoftwareDesignerVersion, Long> {

    Page<SoftwareDesignerVersion> findByOwnerIdAndSessionId(Long ownerId, String sessionId, Pageable pageable);

    @Modifying
    void deleteByOwnerIdAndSessionIdAndCurrentDocumentVersionGreaterThan(Long ownerId, String sessionId, Integer currentVersion);

    Page<SoftwareDesignerVersion> findByOwnerIdAndSessionIdAndSectionPathsContaining(Long ownerId, String sessionId, String sectionPath, Pageable pageable);
}
