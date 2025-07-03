package ai.functionals.api.neura.model.req;

import ai.functionals.api.neura.model.enums.MarketingEmailType;
import ai.functionals.api.neura.model.enums.UserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserReq {
    private String externalId;
    private String fullName;
    private Set<UserRoles> roles;
    private Long usedCredits;
    private Long totalCredits;
    private Long extraCredits;
    private Long creditSubscriptionTier;
    private List<MarketingEmailType> mailSubscriptions;

    private String tenantId;
}
