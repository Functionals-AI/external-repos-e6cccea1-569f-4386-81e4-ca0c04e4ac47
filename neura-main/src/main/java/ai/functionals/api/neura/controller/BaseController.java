package ai.functionals.api.neura.controller;

import ai.functionals.api.neura.filter.SecurityFilter;
import ai.functionals.api.neura.model.commons.AppAuth;
import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.enums.UserRoles;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

public interface BaseController {
    default AppUser getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof AppAuth appAuth) {
            return appAuth.getPrincipal();
        } else {
            return null;
        }
    }
    default boolean isAdmin() {
        if (getCurrentUser() == null) return false;
        Set<UserRoles> roles = getCurrentUser().getRoles();
        if (roles == null || roles.isEmpty()) return false;
        return roles.contains(UserRoles.ROLE_FUN_ROOT) || roles.contains(UserRoles.ROLE_FUN_ADMIN);
    }
    default boolean isRoot() {
        if (getCurrentUser() == null) return false;
        Set<UserRoles> roles = getCurrentUser().getRoles();
        if (roles == null || roles.isEmpty()) return false;
        return roles.contains(UserRoles.ROLE_FUN_ROOT);
    }
}
