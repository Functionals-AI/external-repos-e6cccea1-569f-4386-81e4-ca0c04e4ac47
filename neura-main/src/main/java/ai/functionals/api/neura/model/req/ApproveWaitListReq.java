package ai.functionals.api.neura.model.req;

import ai.functionals.api.neura.model.enums.UserRoles;
import ai.functionals.api.neura.model.enums.UserWaitlistStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveWaitListReq {
    private Set<UserRoles> roles;
    private String description; // internal notes/comments while approving
    private UserWaitlistStatus status;
}
