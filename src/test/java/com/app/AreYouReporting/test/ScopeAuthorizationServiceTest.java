package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.DataScopeType;
import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.Entities.Task;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ScopeAuthorizationServiceTest {

    private ScopeAuthorizationService scopeSecurity;

    private UUID userId;
    private UUID deptId1;
    private UUID deptId2;
    private UUID subDeptId1;
    private UUID subDeptId2;

    private Department dept1;
    private SubDepartment subDept1;

    @BeforeEach
    void setUp() {
        scopeSecurity = new ScopeAuthorizationService();

        userId = UUID.randomUUID();
        deptId1 = UUID.randomUUID();
        deptId2 = UUID.randomUUID();
        subDeptId1 = UUID.randomUUID();
        subDeptId2 = UUID.randomUUID();

        dept1 = Department.builder().name("CSE").code("CSE").build();
        dept1.setId(deptId1);

        subDept1 = SubDepartment.builder().department(dept1).name("AI").code("AI").build();
        subDept1.setId(subDeptId1);
    }

    @Test
    @DisplayName("Super Admin should have universal access to all departments and tasks")
    void testSuperAdminAccess() {
        Role superAdminRole = Role.builder().name("SUPER_ADMIN").build();
        UserRoleAssignment saAssignment = UserRoleAssignment.builder()
                .role(superAdminRole)
                .dataScopeType(DataScopeType.GLOBAL)
                .isActive(true)
                .build();

        UserPrincipal superAdmin = UserPrincipal.builder()
                .id(userId)
                .username("superadmin")
                .active(true)
                .authorities(Collections.emptyList())
                .assignments(List.of(saAssignment))
                .departmentIds(Collections.emptySet())
                .subDepartmentIds(Collections.emptySet())
                .build();

        ActiveUserContext context = ActiveUserContext.builder()
                .userId(userId)
                .effectiveDataScope(DataScopeType.GLOBAL)
                .build();

        assertTrue(scopeSecurity.isSuperAdmin(superAdmin));
        assertTrue(scopeSecurity.canAccessDepartment(deptId2, context, superAdmin));
        assertTrue(scopeSecurity.canAccessSubDepartment(subDeptId2, deptId2, context, superAdmin));

        Task task = Task.builder()
                .title("Any Task")
                .assignedDepartments(Set.of(dept1))
                .assignedSubDepartments(Set.of(subDept1))
                .build();

        assertTrue(scopeSecurity.canViewTask(task, context, superAdmin));
        assertTrue(scopeSecurity.canManageTask(task, context, superAdmin));
    }

    @Test
    @DisplayName("Teacher can only view their own assigned or created tasks")
    void testTeacherScopeRestrictions() {
        Role teacherRole = Role.builder().name("TEACHER").build();
        UserRoleAssignment teacherAssignment = UserRoleAssignment.builder()
                .role(teacherRole)
                .department(dept1)
                .subDepartment(subDept1)
                .dataScopeType(DataScopeType.SELF)
                .isActive(true)
                .build();

        UserPrincipal teacher = UserPrincipal.builder()
                .id(userId)
                .username("teacher1")
                .active(true)
                .authorities(Collections.emptyList())
                .assignments(List.of(teacherAssignment))
                .departmentIds(Set.of(deptId1))
                .subDepartmentIds(Set.of(subDeptId1))
                .build();

        ActiveUserContext context = ActiveUserContext.builder()
                .userId(userId)
                .effectiveDataScope(DataScopeType.SELF)
                .departmentId(deptId1)
                .subDepartmentId(subDeptId1)
                .build();

        User otherUser = User.builder().username("other").build();
        otherUser.setId(UUID.randomUUID());

        Task foreignTask = Task.builder()
                .title("Foreign Task")
                .creator(otherUser)
                .assignedDepartments(Set.of(dept1))
                .assignedSubDepartments(Set.of(subDept1))
                .assignees(Set.of(otherUser))
                .build();

        // Foreign task should not be viewable by teacher
        assertFalse(scopeSecurity.canViewTask(foreignTask, context, teacher));

        // Self task created by teacher should be viewable & manageable
        User selfUser = User.builder().username("teacher1").build();
        selfUser.setId(userId);

        Task selfTask = Task.builder()
                .title("My Task")
                .creator(selfUser)
                .isSelfTask(true)
                .assignees(Set.of(selfUser))
                .build();

        assertTrue(scopeSecurity.canViewTask(selfTask, context, teacher));
        assertTrue(scopeSecurity.canManageTask(selfTask, context, teacher));
    }
}
