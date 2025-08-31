package io.oxalate.backend.security.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import static io.oxalate.backend.api.UserStatusEnum.ACTIVE;
import static io.oxalate.backend.api.UserStatusEnum.REGISTERED;
import io.oxalate.backend.model.User;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@ToString
@Getter
@Slf4j
public class UserDetailsImpl implements UserDetails {

    @Serial private static final long serialVersionUID = 1L;
    private final Long id;
    private final String username;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean approvedTerms;
    private final boolean locked;
    private final String language;

    public UserDetailsImpl(Long id, String username, String password,
            Collection<? extends GrantedAuthority> authorities, boolean approvedTerms, boolean locked, String language) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.approvedTerms = approvedTerms;
        this.locked = locked;
        this.language = language;
    }

    public static UserDetailsImpl build(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .toList();
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.isApprovedTerms(),
                (user.getStatus() != ACTIVE && user.getStatus() != REGISTERED),
                user.getLanguage());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !locked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}
