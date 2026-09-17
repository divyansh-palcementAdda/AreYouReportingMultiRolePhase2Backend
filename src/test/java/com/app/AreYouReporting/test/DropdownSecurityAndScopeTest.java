package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.mapper.DropdownMapper;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.dropdown.*;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.impl.DropdownServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DropdownSecurityAndScopeTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SubDepartmentRepository subDepartmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskTemplateRepository templateRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Spy
    private DropdownMapper dropdownMapper = new DropdownMapper();

    @Mock
    private ScopeAuthorizationService scopeSecurity;

    @InjectMocks
    private DropdownServiceImpl dropdownService;

    private UUID superAdminId;
    private UUID adminId;
    private UUID hodId;
    private UUID teacherId;

    private UUID deptCseId;
    private UUID deptMgmtId;
    private UUID subDeptAimlId;
    private UUID subDeptDsId;
    private UUID subDeptFinId;

    private Department deptCse;
    private Department deptMgmt;
    private SubDepartment subDeptAiml;
    private SubDepartment subDeptDs;
    private SubDepartment subDeptFin;

    private User superAdminUser;
    private User adminUser;
    private User hodUser;
    private User teacherUser;

    private UserPrincipal superAdminPrincipal;
    private UserPrincipal adminPrincipal;
    private UserPrincipal hodPrincipal;
    private UserPrincipal teacherPrincipal;

    @BeforeEach
    void setUp() {
        superAdminId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        hodId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        deptCseId = UUID.randomUUID();
        deptMgmtId = UUID.randomUUID();
        subDeptAimlId = UUID.randomUUID();
        subDeptDsId = UUID.randomUUID();
        subDeptFinId = UUID.randomUUID();

        deptCse = Department.builder()
                .name("Computer Science & Engineering")
                .code("CSE")
                .isActive(true)
                .build();
        deptCse.setId(deptCseId);

        deptMgmt = Department.builder()
                .name("Management Studies")
                .code("MGMT")
                .isActive(true)
                .build();
        deptMgmt.setId(deptMgmtId);

        subDeptAiml = SubDepartment.builder()
                .name("Artificial Intelligence & ML")
                .code("CSE-AIML")
                .department(deptCse)
                .isActive(true)
                .build();
        subDeptAiml.setId(subDeptAimlId);

        subDeptDs = SubDepartment.builder()
                .name("Data Science")
                .code("CSE-DS")
                .department(deptCse)
                .isActive(true)
                .build();
        subDeptDs.setId(subDeptDsId);

        subDeptFin = SubDepartment.builder()
                .name("Finance & Banking")
                .code("MGMT-FIN")
                .department(deptMgmt)
                .isActive(true)
                .build();
        subDeptFin.setId(subDeptFinId);

        deptCse.setSubDepartments(List.of(subDeptAiml, subDeptDs));
        deptMgmt.setSubDepartments(List.of(subDeptFin));

        Role superAdminRole = Role.builder().name("SUPER_ADMIN").defaultDataScope(DataScopeType.GLOBAL).build();
        Role adminRole = Role.builder().name("ADMIN").defaultDataScope(DataScopeType.DEPARTMENT).build();
        Role hodRole = Role.builder().name("HOD").defaultDataScope(DataScopeType.SUB_DEPARTMENT).build();
        Role teacherRole = Role.builder().name("TEACHER").defaultDataScope(DataScopeType.SELF).build();

        superAdminUser = User.builder()
                .username("superadmin")
                .fullName("Super Administrator")
                .email("superadmin@rcef.com")
                .isActive(true)
                .build();
        superAdminUser.setId(superAdminId);

        adminUser = User.builder()
                .username("admin_cse")
                .fullName("CSE Admin")
                .email("admin_cse@rcef.com")
                .departments(Set.of(deptCse))
                .isActive(true)
                .build();
        adminUser.setId(adminId);

        hodUser = User.builder()
                .username("hod_aiml")
                .fullName("AIML HOD")
                .email("hod_aiml@rcef.com")
                .departments(Set.of(deptCse))
                .subDepartments(Set.of(subDeptAiml))
                .isActive(true)
                .build();
        hodUser.setId(hodId);

        teacherUser = User.builder()
                .username("teacher_aiml")
                .fullName("AIML Teacher")
                .email("teacher_aiml@rcef.com")
                .departments(Set.of(deptCse))
                .subDepartments(Set.of(subDeptAiml))
                .isActive(true)
                .build();
        teacherUser.setId(teacherId);

        UserRoleAssignment saAssignment = UserRoleAssignment.builder()
                .user(superAdminUser)
                .role(superAdminRole)
                .dataScopeType(DataScopeType.GLOBAL)
                .isActive(true)
                .build();
        superAdminUser.setRoleAssignments(Set.of(saAssignment));

        UserRoleAssignment adminAssignment = UserRoleAssignment.builder()
                .user(adminUser)
                .role(adminRole)
                .department(deptCse)
                .dataScopeType(DataScopeType.DEPARTMENT)
                .isActive(true)
                .build();
        adminUser.setRoleAssignments(Set.of(adminAssignment));

        UserRoleAssignment hodAssignment = UserRoleAssignment.builder()
                .user(hodUser)
                .role(hodRole)
                .department(deptCse)
                .subDepartment(subDeptAiml)
                .dataScopeType(DataScopeType.SUB_DEPARTMENT)
                .isActive(true)
                .build();
        hodUser.setRoleAssignments(Set.of(hodAssignment));

        UserRoleAssignment teacherAssignment = UserRoleAssignment.builder()
                .user(teacherUser)
                .role(teacherRole)
                .department(deptCse)
                .subDepartment(subDeptAiml)
                .dataScopeType(DataScopeType.SELF)
                .isActive(true)
                .build();
        teacherUser.setRoleAssignments(Set.of(teacherAssignment));

        superAdminPrincipal = UserPrincipal.create(superAdminUser);
        adminPrincipal = UserPrincipal.create(adminUser);
        hodPrincipal = UserPrincipal.create(hodUser);
        teacherPrincipal = UserPrincipal.create(teacherUser);
    }

    @Test
    @DisplayName("Super Admin can query all departments with pagination and sorting")
    void testSuperAdminGetDepartments() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(scopeSecurity.isSuperAdmin(superAdminPrincipal)).thenReturn(true);

        Page<Department> mockPage = new PageImpl<>(List.of(deptCse, deptMgmt));
        when(departmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<DepartmentDropdownDto> response = dropdownService.getDepartments("CSE", true, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Computer Science & Engineering", response.getContent().get(0).getName());
        assertEquals("CSE", response.getContent().get(0).getCode());
        assertTrue(response.getContent().get(0).isActive());
        verify(scopeSecurity).validatePermission("dropdowns.departments");
    }

    @Test
    @DisplayName("Sub-department query validates department ID against caller scope and blocks unauthorized access")
    void testSubDepartmentScopeValidation() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.isSuperAdmin(adminPrincipal)).thenReturn(false);
        when(scopeSecurity.canAccessDepartment(eq(deptMgmtId), any(), eq(adminPrincipal))).thenReturn(false);

        assertThrows(ScopeViolationException.class, () ->
                dropdownService.getSubDepartments(deptMgmtId, null, true, PageRequest.of(0, 20))
        );
    }

    @Test
    @DisplayName("Department to Sub-Department filtering returns mapped sub-departments")
    void testSubDepartmentFilteringByDepartmentId() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.isSuperAdmin(adminPrincipal)).thenReturn(false);
        when(scopeSecurity.canAccessDepartment(eq(deptCseId), any(), eq(adminPrincipal))).thenReturn(true);

        Page<SubDepartment> mockPage = new PageImpl<>(List.of(subDeptAiml, subDeptDs));
        when(subDepartmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<SubDepartmentDropdownDto> response = dropdownService.getSubDepartments(deptCseId, null, true, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Artificial Intelligence & ML", response.getContent().get(0).getLabel());
        assertEquals(deptCseId, response.getContent().get(0).getDepartmentId());
    }

    @Test
    @DisplayName("Eligible reporting managers query returns users with managerial roles")
    void testEligibleReportingManagersQuery() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(teacherPrincipal);
        when(scopeSecurity.isSuperAdmin(teacherPrincipal)).thenReturn(false);
        when(scopeSecurity.canAccessDepartment(eq(deptCseId), any(), eq(teacherPrincipal))).thenReturn(true);
        when(scopeSecurity.canAccessSubDepartment(eq(subDeptAimlId), eq(deptCseId), any(), eq(teacherPrincipal))).thenReturn(true);

        Page<User> mockPage = new PageImpl<>(List.of(hodUser, adminUser, superAdminUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<UserDropdownDto> response = dropdownService.getEligibleReportingManagers(deptCseId, subDeptAimlId, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(3, response.getContent().size());
        assertEquals("AIML HOD", response.getContent().get(0).getFullName());
        assertTrue(response.getContent().get(0).getRoles().contains("HOD"));
    }

    @Test
    @DisplayName("Invalid sort property throws InvalidOperationException to protect query construction")
    void testInvalidSortPropertyRejected() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(scopeSecurity.isSuperAdmin(superAdminPrincipal)).thenReturn(true);

        Pageable maliciousPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "password"));

        assertThrows(InvalidOperationException.class, () ->
                dropdownService.getDepartments(null, true, maliciousPageable)
        );
    }

    @Test
    @DisplayName("Task template dropdown returns lightweight templates with applicable department scope")
    void testTaskTemplatesDropdown() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.isSuperAdmin(adminPrincipal)).thenReturn(false);
        when(scopeSecurity.canAccessDepartment(eq(deptCseId), any(), eq(adminPrincipal))).thenReturn(true);

        TaskTemplate template = TaskTemplate.builder()
                .name("Monthly Progress Report")
                .defaultPriority(TaskPriority.HIGH)
                .defaultDurationDays(30)
                .applicableDepartments(Set.of(deptCse))
                .isActive(true)
                .build();
        template.setId(UUID.randomUUID());

        Page<TaskTemplate> mockPage = new PageImpl<>(List.of(template));
        when(templateRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<TaskTemplateDropdownDto> response = dropdownService.getTaskTemplates(deptCseId, null, null, true, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Monthly Progress Report", response.getContent().get(0).getName());
        assertEquals(TaskPriority.HIGH, response.getContent().get(0).getDefaultPriority());
        assertTrue(response.getContent().get(0).getApplicableDepartmentIds().contains(deptCseId));
    }

    @Test
    @DisplayName("Roles dropdown filters out SUPER_ADMIN for non-superadmin callers")
    void testRolesDropdownScopeFiltering() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.isSuperAdmin(adminPrincipal)).thenReturn(false);

        Role subAdminRole = Role.builder().name("SUB_ADMIN").description("Sub-Admin").defaultDataScope(DataScopeType.DEPARTMENT).build();
        Role hodRole = Role.builder().name("HOD").description("HOD").defaultDataScope(DataScopeType.SUB_DEPARTMENT).build();
        subAdminRole.setId(UUID.randomUUID());
        hodRole.setId(UUID.randomUUID());

        Page<Role> mockPage = new PageImpl<>(List.of(subAdminRole, hodRole));
        when(roleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<RoleDropdownDto> response = dropdownService.getRoles(null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("SUB_ADMIN", response.getContent().get(0).getName());
        assertEquals("HOD", response.getContent().get(1).getName());
    }

    @Test
    @DisplayName("Static options endpoints return complete, non-null enum values")
    void testStaticOptionsEndpoints() {
        List<StaticOptionDto> taskStatuses = dropdownService.getTaskStatuses();
        assertFalse(taskStatuses.isEmpty());
        assertTrue(taskStatuses.stream().anyMatch(s -> s.getValue().equals("PENDING")));
        assertTrue(taskStatuses.stream().noneMatch(s -> s.getValue().equals("DELETED")));

        List<StaticOptionDto> requestStatuses = dropdownService.getRequestStatuses();
        assertFalse(requestStatuses.isEmpty());
        assertTrue(requestStatuses.stream().anyMatch(s -> s.getValue().equals("APPROVED")));

        List<StaticOptionDto> priorities = dropdownService.getTaskPriorities();
        assertFalse(priorities.isEmpty());
        assertTrue(priorities.stream().anyMatch(p -> p.getValue().equals("HIGH")));

        List<StaticOptionDto> proofTypes = dropdownService.getProofRequirementTypes();
        assertFalse(proofTypes.isEmpty());
        assertTrue(proofTypes.stream().anyMatch(pt -> pt.getValue().equals("DOCUMENT")));

        List<StaticOptionDto> scopes = dropdownService.getDataScopeTypes();
        assertFalse(scopes.isEmpty());
        assertTrue(scopes.stream().anyMatch(ds -> ds.getValue().equals("GLOBAL")));
    }
}
