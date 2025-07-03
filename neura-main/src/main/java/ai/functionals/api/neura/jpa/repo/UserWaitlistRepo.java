package ai.functionals.api.neura.jpa.repo;

import ai.functionals.api.neura.jpa.entity.UserWaitlist;
import ai.functionals.api.neura.model.enums.UserWaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWaitlistRepo extends JpaRepository<UserWaitlist, Long> {
    Page<UserWaitlist> findByStatus(UserWaitlistStatus status, Pageable pageable);
    List<UserWaitlist> findByEmail(String email);
    List<UserWaitlist> findBySlug(String slug);
}
