package br.com.kuntzedevprojects.money_master_2.config.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter defaultScopesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>(defaultScopesConverter.convert(jwt));
        addClaimAuthorities(jwt, authorities, "roles");
        addClaimAuthorities(jwt, authorities, "permissions");
        return authorities;
    }

    private void addClaimAuthorities(Jwt jwt, Set<GrantedAuthority> authorities, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }
}
