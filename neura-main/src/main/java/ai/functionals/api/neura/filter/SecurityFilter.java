package ai.functionals.api.neura.filter;


import ai.functionals.api.neura.jpa.entity.UserEntity;
import ai.functionals.api.neura.model.commons.AppAuth;
import ai.functionals.api.neura.model.commons.AppUser;
import ai.functionals.api.neura.model.enums.UserRoles;
import ai.functionals.api.neura.service.UserService;
import com.google.common.collect.Sets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SecurityFilter extends OncePerRequestFilter {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Value("${app.auth0.role.claim.key}")
    private String roleClaimKey;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                token = token.substring(7);
                Jwt jwt = jwtDecoder.decode(token);
                if (jwt != null && jwt.getClaims() != null) {
                    String tenantId = getClaim(jwt.getClaims(), "tenantId");
                    if (StringUtils.isBlank(tenantId)) {
                        tenantId = getClaim(jwt.getClaims(), "azp");
                    }
                    String email = getClaim(jwt.getClaims(), "email");
                    if (StringUtils.isBlank(email)) {
                        email = getClaim(jwt.getClaims(), "https://api.functionals.ai/email");
                    }
                    String userId = getClaim(jwt.getClaims(), "sub");
                    String name = getClaim(jwt.getClaims(), "name");
                    if (StringUtils.isBlank(name)) {
                        name = getClaim(jwt.getClaims(), "https://api.functionals.ai/fullName");
                    }
                    if (StringUtils.isBlank(name)) {
                        name = getClaim(jwt.getClaims(), "https://api.functionals.ai/givenName") + " " +
                                getClaim(jwt.getClaims(), "https://api.functionals.ai/familyName");
                    }
                    Set<UserRoles> roles = Sets.newHashSet();
                    Object rolesObject = jwt.getClaim(roleClaimKey);
                    if (rolesObject instanceof List<?> rolesList) {
                        roles.addAll(rolesList.stream()
                                .map(item -> {
                                    if (item == null) return null;
                                    if (item instanceof String itemStr) {
                                        return UserRoles.valueOf(itemStr);
                                    } else {
                                        return null;
                                    }
                                }).filter(Objects::nonNull)
                                .collect(Collectors.toSet()));
                    }
                    UserEntity userEntity = userService.persistIfValidUser(email, userId, name, tenantId, roles);
                    Long id = null;
                    if (userEntity != null) {
                        id = userEntity.getId();
                    }
                    roles.addAll(userService.findRolesByEmail(email, userId));
                    AppUser appUser = AppUser.builder().email(email).roles(roles)
                            .name(name).tenantId(tenantId).id(id).externalId(userId).build();
                    assignSecurityContext(appUser, jwt);
                }
            } catch (Exception e) {
                log.error("SecurityFilter exception for URI: {}", request.getRequestURI(), e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token: " + token);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getClaim(Map<String, Object> claims, String key) {
        if (claims != null) {
            Object tenantIdObject = claims.get(key);
            if (tenantIdObject instanceof String) {
                return (String) tenantIdObject;
            }
        }
        return null;
    }

    private void assignSecurityContext(AppUser user, Jwt jwt) throws ParseException {
        SecurityContextHolder.getContext().setAuthentication(new AppAuth(user, SecurityContextHolder.getContext().getAuthentication(),
                getAuthorities(jwt.getClaims(), user), jwt.getTokenValue()));
    }

    private List<SimpleGrantedAuthority> getAuthorities(Map<String, Object> claims, AppUser user) {
        List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        Object rolesObject = claims.get(roleClaimKey);
        if (rolesObject instanceof List<?>) {
            List<SimpleGrantedAuthority> list = ((List<?>) rolesObject).stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .map(SimpleGrantedAuthority::new).toList();
            simpleGrantedAuthorities.addAll(list);
        }
        if (user != null) {
            if (user.getRoles() != null) {
                List<SimpleGrantedAuthority> list = user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.name()))
                        .toList();
                simpleGrantedAuthorities.addAll(list);
            }
            Set<String> rootUsers = userService.getRootUsers();
            Set<String> adminUsers = userService.getAdminUsers();
            String lowerCaseEmail = StringUtils.isNotBlank(user.getEmail()) ? user.getEmail().toLowerCase() : "";
            if (rootUsers.contains(lowerCaseEmail)) {
                simpleGrantedAuthorities.add(new SimpleGrantedAuthority(UserRoles.ROLE_FUN_ROOT.name()));
            }
            if (adminUsers.contains(lowerCaseEmail)) {
                simpleGrantedAuthorities.add(new SimpleGrantedAuthority(UserRoles.ROLE_FUN_ADMIN.name()));
            }
        }

        if (simpleGrantedAuthorities.isEmpty()) {
            simpleGrantedAuthorities.add(new SimpleGrantedAuthority(UserRoles.ROLE_FUN_ANONYMOUS.name()));
        }
        return simpleGrantedAuthorities;
    }
}