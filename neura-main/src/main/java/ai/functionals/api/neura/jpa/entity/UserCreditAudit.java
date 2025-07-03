package ai.functionals.api.neura.jpa.entity;

import ai.functionals.api.neura.model.enums.MarketingEmailType;
import ai.functionals.api.neura.model.enums.UserRoles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Immutable
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_credit_audit", schema = "neura")
public class UserCreditAudit implements Serializable {
    @Id
    @EqualsAndHashCode.Include()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long createdTime;
    private String tenantId;        // eg: google-oauth2|105172710531952360183

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private Long usedCredits;       // used current month
    private Long totalCredits;      // total available for current month
    private Long extraCredits;      // extra credits if added by admin
    private Long creditSubscriptionTier;    // subscription tier for credits, 1=5000, 2=10000, 3=20000, etc.

    @PrePersist
    public void beforePersist() {
        this.createdTime = System.currentTimeMillis();
        if (this.tenantId == null && user != null) {
            this.tenantId = user.getTenantId();
        }
    }
}
