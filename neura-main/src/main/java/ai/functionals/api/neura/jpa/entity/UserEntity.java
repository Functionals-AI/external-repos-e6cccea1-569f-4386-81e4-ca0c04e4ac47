package ai.functionals.api.neura.jpa.entity;

import ai.functionals.api.neura.model.enums.MarketingEmailType;
import ai.functionals.api.neura.model.enums.UserRoles;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user", schema = "neura")
public class UserEntity extends Base {
    private String externalId;      // external id, eg: google-oauth2|105172710531952360183
    private String fullName;        // full name
    private String email;           // email
    @Column(name="roles", columnDefinition = "text")
    @Convert(converter = UserRoles.Converter.class)
    private Set<UserRoles> roles;   // UserRoles
    private Long usedCredits;       // used current month
    private Long totalCredits;      // total available for current month
    private Long extraCredits;      // extra credits if added by admin
    private Long creditSubscriptionTier;    // subscription tier for credits, 1=5000, 2=10000, 3=20000, etc.
    @Convert(converter = MarketingEmailType.Converter.class)
    private List<MarketingEmailType> mailSubscriptions;
}
