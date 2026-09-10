package io.oxalate.backend.tools;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class AuthTools {

    private AuthTools() {
        // Utility class
    }

    /**
     * OWASP A01:2025 - ownership check used by every "self or elevated role" endpoint.
     * <p>
     * The principal is only a {@code UserDetailsImpl} for authenticated users; for anonymous requests it is
     * the string {@code "anonymousUser"}. Casting unconditionally threw a {@link ClassCastException}, which
     * surfaced as a 500 instead of a denial. The check now fails closed.
     *
     * @param userId the user id the caller wants to act on
     * @return {@code true} only when the authenticated caller is that user
     */
    public static boolean isUserIdCurrentUser(long userId) {
        var userDetails = getCurrentUserDetails();

        if (userDetails == null) {
            return false;
        }

        return userDetails.getId() == userId;
    }

    public static boolean currentUserHasAnyRole(RoleEnum... roles) {
        for (RoleEnum role : roles) {
            if (currentUserHasRole(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean currentUserHasRole(RoleEnum role) {
        if (role == null) {
            return false;
        }

        var authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (role.name()
                    .equals(auth.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    public static boolean currentUserHasNotAcceptedTerms() {
        var userDetails = getCurrentUserDetails();

        if (userDetails == null) {
            return true;
        }

        return !userDetails.isApprovedTerms();
    }

    public static boolean currentUserHasNotAcceptedHealthStatement() {
        var userDetails = getCurrentUserDetails();

        if (userDetails == null) {
            log.debug("Authentication is null, treating as user that has not accepted health statement");
            return true;
        }

        var healthStatementId = userDetails.getHealthStatementId();
        log.debug("The retrieved health statement ID is: {}", healthStatementId);

        return (healthStatementId == null);
    }

    public static long getCurrentUserId() {
        var userDetails = getCurrentUserDetails();

        if (userDetails == null) {
            return -1;
        }

        return userDetails.getId();
    }

    public static String getLanguage() {
        var userDetails = getCurrentUserDetails();

        if (userDetails == null) {
            return "fi";
        }

        return userDetails.getLanguage();
    }

    public static Set<RoleEnum> getUserRoles() {
        var authentication = getAuthentication();
        var roleList = new HashSet<RoleEnum>();

        if (authentication != null) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                roleList.add(RoleEnum.fromString(auth.getAuthority()));
            }
        }

        // This is always present, as it represents a user that has not logged in
        roleList.add(RoleEnum.ROLE_ANONYMOUS);
        return roleList;
    }

    private static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null) {
            return null;
        }

        return context.getAuthentication();
    }

    /**
     * @return the authenticated principal, or {@code null} when the request is anonymous or the principal is
     * not an Oxalate user
     */
    private static UserDetailsImpl getCurrentUserDetails() {
        var authentication = getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails;
        }

        return null;
    }
}
