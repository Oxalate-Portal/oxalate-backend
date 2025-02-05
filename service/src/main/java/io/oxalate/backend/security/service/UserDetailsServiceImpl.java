package io.oxalate.backend.security.service;

import io.oxalate.backend.model.Role;
import io.oxalate.backend.repository.RoleRepository;
import io.oxalate.backend.repository.UserRepository;
import io.oxalate.backend.security.AuthenticationFailureListener;
import io.oxalate.backend.security.LoginAttemptService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LoginAttemptService loginAttemptService;
    // This may look like it is not used, but it is initialized here so that we then can get it to work in loginAttemptService
    private final AuthenticationFailureListener authenticationFailureListener;

    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.loginAttemptService = loginAttemptService;
        this.authenticationFailureListener = new AuthenticationFailureListener();
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (loginAttemptService.isBlocked()) {
            log.warn("User '{}' tried to login but is blocked", username);
            throw new RuntimeException("User has been blocked from logging in");
        }

        var optionalUser = userRepository.findByUsername(username.toLowerCase());

        if (optionalUser.isEmpty()) {
            log.error("Could not find user based on the name: '{}'", username);
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }

        var user = optionalUser.get();

        Set<Role> grantList = roleRepository.findByUser(user.getId());
        user.setRoles(grantList);

        return UserDetailsImpl.build(user);
    }
}
