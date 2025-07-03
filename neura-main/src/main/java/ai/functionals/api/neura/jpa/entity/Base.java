package ai.functionals.api.neura.jpa.entity;

import ai.functionals.api.neura.model.commons.AppAuth;
import ai.functionals.api.neura.model.commons.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SoftDelete;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;

import static ai.functionals.api.neura.util.AppUtil.genSlug;

@Data
@Accessors(fluent = false, chain = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
@SoftDelete
@Slf4j
public abstract class Base implements Serializable {
    @Id
    @EqualsAndHashCode.Include()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long createdTime;
    private Long modifiedTime;
    private String createdBy;
    private String modifiedBy;
    @EqualsAndHashCode.Include()
    private String slug;
    private String tenantId;        // eg: google-oauth2|105172710531952360183

    @PrePersist
    public void beforeSave() {
        long currentTimeMillis = System.currentTimeMillis();
        setCreatedTime(currentTimeMillis);
        setModifiedTime(currentTimeMillis);
        AppUser loggedUser = getLoggedUser();
        if (loggedUser != null) {
            setCreatedBy(loggedUser.getEmail());
            setModifiedBy(loggedUser.getEmail());
            setTenantId(loggedUser.getTenantId());
        }
        setSlug(genSlug());
    }

    @PreUpdate
    public void beforeUpdate() {
        long currentTimeMillis = System.currentTimeMillis();
        setModifiedTime(currentTimeMillis);
        AppUser loggedUser = getLoggedUser();
        if (loggedUser != null) {
            setModifiedBy(loggedUser.getEmail());
        }
    }

    private AppUser getLoggedUser() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof AppAuth appAuth) {
            AppAuth authToken = (AppAuth) SecurityContextHolder.getContext().getAuthentication();
            if (authToken == null) {
                return null;
            }
            return authToken.getPrincipal();
        } else {
            return null;
        }
    }
}
