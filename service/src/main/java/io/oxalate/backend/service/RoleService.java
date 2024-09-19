package io.oxalate.backend.service;

import io.oxalate.backend.api.RoleEnum;
import io.oxalate.backend.model.Role;
import io.oxalate.backend.repository.RoleRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    // Fetch all roles
    public Set<Role> findAll() {
        return roleRepository.findAll();
    }

    // Fetch role by name
    public Role findByName(String name) {
        return roleRepository.findByName(RoleEnum.fromString(name)).orElse(null);
    }

    public Set<Role> findAllByNames(Set<String> names) {
        var nameList = new HashSet<Role>();

        for (String name : names) {
            nameList.add(findByName(name));
        }

        return nameList;
    }

	public Set<Role> findRolesForUser(Long userId) {
        return roleRepository.findByUser(userId);
	}

    public long getRoleId(Role role) {
        // TODO There has to be a more elegant way of getting the role ID of the user, maybe rethink how the roles are stored
        return roleRepository.findByName(role.getName()).get().getId();
    }

    public void deleteUserRoles(long userId) {
        roleRepository.deleteAllUserRolesByUserId(userId);
    }

    public boolean userHasRole(long userId, RoleEnum roleEnum) {
        var roles = findRolesForUser(userId);

        for (Role role : roles) {
            if (role.getName().equals(roleEnum)) {
                return true;
            }
        }

        return false;
    }
}
