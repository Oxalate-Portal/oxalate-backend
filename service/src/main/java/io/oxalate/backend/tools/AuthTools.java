package io.oxalate.backend.tools;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.security.service.UserDetailsImpl;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthTools {

    public static boolean isUserIdCurrentUser(long userId) {
        var authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

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

    public static boolean currentUserHasAcceptedTerms() {
        var authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        return ((UserDetailsImpl) authentication.getPrincipal()).isApprovedTerms();
    }

    public static long getCurrentUserId() {
        var authentication = getAuthentication();

        if (authentication == null ||
                authentication.getPrincipal() instanceof String) {
            return -1;
        }

        return ((UserDetailsImpl) authentication.getPrincipal()).getId();
    }

    public static String getLanguage() {
        var authentication = getAuthentication();

        if (authentication == null ||
                authentication.getPrincipal() instanceof String) {
            return "fi";
        }

        return ((UserDetailsImpl) authentication.getPrincipal()).getLanguage();
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
}
