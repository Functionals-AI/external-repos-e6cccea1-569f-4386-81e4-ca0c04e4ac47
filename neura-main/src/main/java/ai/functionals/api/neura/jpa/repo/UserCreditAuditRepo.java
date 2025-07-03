package ai.functionals.api.neura.jpa.repo;

import ai.functionals.api.neura.jpa.entity.UserCreditAudit;
import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.model.enums.UserRoles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCreditAuditRepo extends JpaRepository<UserCreditAudit, Long> {
}
