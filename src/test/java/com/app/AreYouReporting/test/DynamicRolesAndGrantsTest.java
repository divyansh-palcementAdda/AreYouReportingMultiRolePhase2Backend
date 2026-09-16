package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.DataScopeType;
import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicRolesAndGrantsTest {

    private final PermissionMapper permissionMapper = new PermissionMapper();

    @Test
    @DisplayName("Resource grants grouping should structure permissions by resource with granted flags")
    void testResourceGrantsGrouping() {
        Permission p1 = Permission.builder()
                .resource("tasks")
                .action("read")
                .authority("tasks.read")
                .build();
        p1.setId(UUID.randomUUID());

        Permission p2 = Permission.builder()
                .resource("tasks")
                .action("create")
                .authority("tasks.create")
                .build();
        p2.setId(UUID.randomUUID());

        Permission p3 = Permission.builder()
                .resource("users")
                .action("delete")
                .authority("users.delete")
                .build();
        p3.setId(UUID.randomUUID());

        List<Permission> allPermissions = List.of(p1, p2, p3);
        Set<Permission> roleGrantedPermissions = Set.of(p1, p2); // Role only has tasks.read & tasks.create

        List<ResourceGrantsDto> grants = permissionMapper.toResourceGrants(allPermissions, roleGrantedPermissions);

        assertNotNull(grants);
        assertEquals(2, grants.size()); // "tasks" and "users"

        ResourceGrantsDto tasksGrant = grants.stream().filter(g -> "tasks".equals(g.getResource())).findFirst().orElse(null);
        assertNotNull(tasksGrant);
        assertEquals(2, tasksGrant.getPermissions().size());
        assertTrue(tasksGrant.getPermissions().get(0).isGranted());
        assertTrue(tasksGrant.getPermissions().get(1).isGranted());

        ResourceGrantsDto usersGrant = grants.stream().filter(g -> "users".equals(g.getResource())).findFirst().orElse(null);
        assertNotNull(usersGrant);
        assertEquals(1, usersGrant.getPermissions().size());
        assertFalse(usersGrant.getPermissions().get(0).isGranted()); // users.delete is false
    }

    @Test
    @DisplayName("Dynamic role entity stores custom data scopes and permissions")
    void testDynamicRoleConfiguration() {
        Role customRole = Role.builder()
                .name("AUDITOR")
                .description("Quality and Compliance Auditor")
                .defaultDataScope(DataScopeType.DEPARTMENT)
                .isSystemRole(false)
                .build();

        assertEquals("AUDITOR", customRole.getName());
        assertEquals(DataScopeType.DEPARTMENT, customRole.getDefaultDataScope());
        assertFalse(customRole.isSystemRole());
    }
}
