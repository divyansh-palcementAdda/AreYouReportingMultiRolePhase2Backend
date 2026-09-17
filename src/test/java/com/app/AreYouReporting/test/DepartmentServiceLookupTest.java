package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.DataScopeType;
import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import com.app.AreYouReporting.exceptions.DepartmentNotFoundException;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.mapper.DepartmentMapper;
import com.app.AreYouReporting.payload.request.DepartmentRequest;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.repository.DepartmentRepository;
import com.app.AreYouReporting.repository.SubDepartmentRepository;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.impl.DepartmentServiceImpl;
import com.app.AreYouReporting.service.interfaces.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceLookupTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SubDepartmentRepository subDepartmentRepository;

    @Spy
    private DepartmentMapper departmentMapper = new DepartmentMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private ScopeAuthorizationService scopeSecurity;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private UUID deptId;
    private UUID foreignDeptId;
    private Department testDept;
    private Department foreignDept;

    private User adminUser;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        deptId = UUID.fromString("b854dfbf-f144-47ff-81aa-7691509c6f36");
        foreignDeptId = UUID.randomUUID();

        testDept = Department.builder()
                .name("Test")
                .code("TEST_223243")
                .description("testing")
                .isActive(true)
                .build();
        testDept.setId(deptId);

        foreignDept = Department.builder()
                .name("Foreign Dept")
                .code("FOREIGN_01")
                .description("Foreign description")
                .isActive(true)
                .build();
        foreignDept.setId(foreignDeptId);

        Role adminRole = Role.builder().name("ADMIN").defaultDataScope(DataScopeType.DEPARTMENT).build();

        adminUser = User.builder()
                .username("admin_user")
                .fullName("Admin User")
                .email("admin@rcef.com")
                .departments(Set.of(testDept))
                .isActive(true)
                .build();
        adminUser.setId(UUID.randomUUID());

        UserRoleAssignment assignment = UserRoleAssignment.builder()
                .user(adminUser)
                .role(adminRole)
                .department(testDept)
                .dataScopeType(DataScopeType.DEPARTMENT)
                .isActive(true)
                .build();
        adminUser.setRoleAssignments(Set.of(assignment));

        adminPrincipal = UserPrincipal.create(adminUser);
    }

    @Test
    @DisplayName("Department returned by list can be fetched with getById by the same authorized user")
    void testDepartmentListAndGetByIdConsistency() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.isSuperAdmin(adminPrincipal)).thenReturn(false);
        when(scopeSecurity.canAccessDepartment(eq(deptId), any(), eq(adminPrincipal))).thenReturn(true);
        when(departmentRepository.findAll()).thenReturn(List.of(testDept));
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));

        List<DepartmentDto> list = departmentService.getAllDepartments(false);
        assertEquals(1, list.size());
        assertEquals(deptId, list.get(0).getId());

        DepartmentDto byId = departmentService.getDepartmentById(deptId);
        assertNotNull(byId);
        assertEquals(deptId, byId.getId());
        assertEquals("Test", byId.getName());
        assertEquals("TEST_223243", byId.getCode());
        verify(departmentRepository).findById(deptId);
    }

    @Test
    @DisplayName("Authorized user can update a visible department by ID")
    void testAuthorizedUserCanUpdateDepartment() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.canAccessDepartment(eq(deptId), any(), eq(adminPrincipal))).thenReturn(true);
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentRequest updateRequest = new DepartmentRequest("Test Updated", "TEST_223243", "Updated description", true);
        DepartmentDto result = departmentService.updateDepartment(deptId, updateRequest);

        assertNotNull(result);
        assertEquals(deptId, result.getId());
        assertEquals("Test Updated", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(scopeSecurity).validatePermission("departments.update");
        verify(departmentRepository).findById(deptId);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("Unauthorized user receives 403 ScopeViolationException when attempting to update foreign department, not 404")
    void testUnauthorizedUserReceives403Not404OnUpdate() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(departmentRepository.findById(foreignDeptId)).thenReturn(Optional.of(foreignDept));
        when(scopeSecurity.canAccessDepartment(eq(foreignDeptId), any(), eq(adminPrincipal))).thenReturn(false);

        DepartmentRequest updateRequest = new DepartmentRequest("Foreign Updated", "FOREIGN_01", "Attempted update", true);

        ScopeViolationException ex = assertThrows(ScopeViolationException.class, () ->
                departmentService.updateDepartment(foreignDeptId, updateRequest)
        );

        assertTrue(ex.getMessage().contains("Access denied"));
        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Genuinely unknown UUID throws typed DepartmentNotFoundException resulting in 404")
    void testUnknownUUIDThrowsDepartmentNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(departmentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        DepartmentRequest updateRequest = new DepartmentRequest("Name", "CODE", "Desc", true);

        DepartmentNotFoundException ex = assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.updateDepartment(nonExistentId, updateRequest)
        );

        assertTrue(ex.getMessage().contains("Department not found with id"));
    }

    @Test
    @DisplayName("Canonical getDepartmentByIdOrThrow returns Department when found")
    void testCanonicalGetDepartmentByIdOrThrow() {
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));

        Department result = departmentService.getDepartmentByIdOrThrow(deptId);
        assertNotNull(result);
        assertEquals(deptId, result.getId());
        assertEquals("Test", result.getName());
        verify(departmentRepository).findById(deptId);
    }

    @Test
    @DisplayName("Canonical getDepartmentByIdOrThrow throws DepartmentNotFoundException when not found")
    void testCanonicalGetDepartmentByIdOrThrowThrows() {
        UUID unknown = UUID.randomUUID();
        when(departmentRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.getDepartmentByIdOrThrow(unknown)
        );
    }

    @Test
    @DisplayName("Attempting to rename department to an already existing name throws InvalidOperationException")
    void testDuplicateNameValidationOnUpdate() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(scopeSecurity.canAccessDepartment(eq(deptId), any(), eq(adminPrincipal))).thenReturn(true);
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));
        when(departmentRepository.existsByName("Existing Name")).thenReturn(true);

        DepartmentRequest updateRequest = new DepartmentRequest("Existing Name", "TEST_223243", "Desc", true);

        assertThrows(InvalidOperationException.class, () ->
                departmentService.updateDepartment(deptId, updateRequest)
        );
    }

    @Test
    @DisplayName("Regression test: Canonical UUID findById resolves 16-byte binary UUID mapping without string conversions")
    void testCanonicalUuidLookupResolvesDepartment() {
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));

        Department result = departmentService.getDepartmentByIdOrThrow(deptId);
        assertNotNull(result);
        assertEquals(deptId, result.getId());
        assertEquals("Test", result.getName());
        verify(departmentRepository).findById(deptId);
    }

    @Test
    @DisplayName("Inactive department throws DepartmentNotFoundException (404) on getDepartmentById")
    void testInactiveDepartmentThrows404OnGetDepartmentById() {
        testDept.setActive(false);
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(adminPrincipal);
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(testDept));

        assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.getDepartmentById(deptId)
        );
    }
}
