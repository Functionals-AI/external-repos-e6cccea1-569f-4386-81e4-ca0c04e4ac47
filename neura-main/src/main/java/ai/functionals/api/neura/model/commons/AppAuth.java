package ai.functionals.api.neura.model.commons;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AppAuth extends AbstractAuthenticationToken {
    private final AppUser principal;
    private final Object credentials;
    private final String token;

    public AppAuth(AppUser appUser, Object credentials, Collection<? extends GrantedAuthority> authorities, String token) {
        super(authorities);
        this.principal = appUser;
        this.credentials = credentials;
        this.token = token;
        setAuthenticated(true);
    }
}
