package ai.functionals.api.neura.model.commons;

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
public class AppUser {
    private Long id;
    private String externalId; // auth0
    private String tenantId;
    private String name;
    private String email;
    private String password;
    private String status;
    private Set<UserRoles> roles;
    private String createdAt;
    private String updatedAt;
}
