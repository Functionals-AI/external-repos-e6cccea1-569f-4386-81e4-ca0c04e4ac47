package ai.functionals.api.neura.jpa.repo;

import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.model.enums.UserRoles;
import io.grpc.MethodDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Long> {
    List<UserEntity> findByEmail(String email);

    List<UserEntity> findByExternalId(String externalId);

    List<UserEntity> findBySlug(String slug);

    Page<UserEntity> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    @Query(value = "SELECT * FROM user u WHERE u.roles ILIKE %:role%", nativeQuery = true)
    List<UserEntity> findByRolesContainingIgnoreCase(@Param("role") String role);
}
