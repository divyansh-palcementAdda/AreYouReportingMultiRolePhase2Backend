package com.app.AreYouReporting.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.ProofType;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.Entities.Task;
import com.app.AreYouReporting.Entities.TaskProof;
import com.app.AreYouReporting.Entities.TaskRequest;
import com.app.AreYouReporting.Entities.TaskRequestStatus;
import com.app.AreYouReporting.Entities.TaskRequestType;
import com.app.AreYouReporting.Entities.TaskStatus;
import com.app.AreYouReporting.Entities.TaskTemplate;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.exceptions.DepartmentNotFoundException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.DepartmentMapper;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.mapper.TaskTemplateMapper;
import com.app.AreYouReporting.mapper.UserMapper;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;
import com.app.AreYouReporting.repository.DepartmentRepository;
import com.app.AreYouReporting.repository.PermissionRepository;
import com.app.AreYouReporting.repository.SubDepartmentRepository;
import com.app.AreYouReporting.repository.TaskProofRepository;
import com.app.AreYouReporting.repository.TaskRepository;
import com.app.AreYouReporting.repository.TaskRequestRepository;
import com.app.AreYouReporting.repository.TaskTemplateRepository;
import com.app.AreYouReporting.repository.UserRepository;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.impl.DepartmentServiceImpl;
import com.app.AreYouReporting.service.impl.TaskProofServiceImpl;
import com.app.AreYouReporting.service.impl.TaskRequestServiceImpl;
import com.app.AreYouReporting.service.impl.TaskServiceImpl;
import com.app.AreYouReporting.service.impl.TaskTemplateServiceImpl;
import com.app.AreYouReporting.service.impl.UserServiceImpl;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.storage.StorageService;

@ExtendWith(MockitoExtension.class)
public class ActiveDataDefaultVisibilityTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SubDepartmentRepository subDepartmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskProofRepository proofRepository;
    @Mock
    private TaskRequestRepository requestRepository;
    @Mock
    private TaskTemplateRepository taskTemplateRepository;
    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private ScopeAuthorizationService scopeSecurity;
    @Mock
    private AuditService auditService;
    @Mock
    private StorageService storageService;

    @Spy
    private DepartmentMapper departmentMapper = new DepartmentMapper();
    @Spy
    private PermissionMapper permissionMapper = new PermissionMapper();
    @Spy
    private UserMapper userMapper = new UserMapper(departmentMapper);
    @Spy
    private TaskTemplateMapper taskTemplateMapper = new TaskTemplateMapper(departmentMapper);

    @InjectMocks
    private DepartmentServiceImpl departmentService;
    @InjectMocks
    private UserServiceImpl userService;
    @InjectMocks
    private TaskServiceImpl taskService;
    @InjectMocks
    private TaskTemplateServiceImpl taskTemplateService;
    @InjectMocks
    private TaskProofServiceImpl taskProofService;
    @InjectMocks
    private TaskRequestServiceImpl taskRequestService;

    private UserPrincipal superAdminPrincipal;
    private UUID activeDeptId;
    private UUID inactiveDeptId;
    private Department activeDept;
    private Department inactiveDept;
    private SubDepartment activeSubDeptUnderActiveDept;
    private SubDepartment subDeptUnderInactiveDept;
    private User activeUser;
    private User inactiveUser;
    private Task activeTask;
    private Task softDeletedTask;
    private TaskTemplate activeTemplate;
    private TaskTemplate inactiveTemplate;

    @BeforeEach
    void setUp() {
        activeDeptId = UUID.randomUUID();
        inactiveDeptId = UUID.randomUUID();

        activeDept = Department.builder()
                .name("Computer Science")
                .code("CS")
                .isActive(true)
                .build();
        activeDept.setId(activeDeptId);

        inactiveDept = Department.builder()
                .name("Old History")
                .code("HIST")
                .isActive(false)
                .build();
        inactiveDept.setId(inactiveDeptId);

        activeSubDeptUnderActiveDept = SubDepartment.builder()
                .department(activeDept)
                .name("Software Engineering")
                .code("SE")
                .isActive(true)
                .build();
        activeSubDeptUnderActiveDept.setId(UUID.randomUUID());

        subDeptUnderInactiveDept = SubDepartment.builder()
                .department(inactiveDept)
                .name("Ancient History")
                .code("AH")
                .isActive(true)
                .build();
        subDeptUnderInactiveDept.setId(UUID.randomUUID());

        activeUser = User.builder()
                .username("active_user")
                .fullName("Active User")
                .email("active@test.com")
                .isActive(true)
                .departments(Set.of(activeDept))
                .build();
        activeUser.setId(UUID.randomUUID());

        inactiveUser = User.builder()
                .username("inactive_user")
                .fullName("Inactive User")
                .email("inactive@test.com")
                .isActive(false)
                .departments(Set.of(activeDept))
                .build();
        inactiveUser.setId(UUID.randomUUID());

        superAdminPrincipal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .username("superadmin")
                .active(true)
                .departmentIds(Set.of(activeDeptId))
                .subDepartmentIds(Collections.emptySet())
                .build();

        activeTemplate = TaskTemplate.builder()
                .name("Active Template")
                .isActive(true)
                .applicableDepartments(Set.of(activeDept, inactiveDept))
                .build();
        activeTemplate.setId(UUID.randomUUID());

        inactiveTemplate = TaskTemplate.builder()
                .name("Inactive Template")
                .isActive(false)
                .build();
        inactiveTemplate.setId(UUID.randomUUID());

        activeTask = Task.builder()
                .title("Active Task")
                .status(TaskStatus.IN_PROGRESS)
                .creator(activeUser)
                .assignedDepartments(Set.of(activeDept, inactiveDept))
                .assignees(Set.of(activeUser, inactiveUser))
                .template(activeTemplate)
                .dueDate(Instant.now().plusSeconds(86400))
                .build();
        activeTask.setId(UUID.randomUUID());

        softDeletedTask = Task.builder()
                .title("Soft Deleted Task")
                .status(TaskStatus.DELETED)
                .creator(activeUser)
                .dueDate(Instant.now().plusSeconds(86400))
                .build();
        softDeletedTask.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Department list returns only active departments when activeOnly is true (default)")
    void testDepartmentListReturnsOnlyActiveByDefault() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(scopeSecurity.isSuperAdmin(superAdminPrincipal)).thenReturn(true);
        when(departmentRepository.findByIsActiveTrue()).thenReturn(List.of(activeDept));

        List<DepartmentDto> result = departmentService.getAllDepartments(true);

        assertEquals(1, result.size());
        assertEquals("Computer Science", result.get(0).getName());
        verify(departmentRepository).findByIsActiveTrue();
        verify(departmentRepository, never()).findAll();
    }

    @Test
    @DisplayName("Get department by ID throws DepartmentNotFoundException (404) for inactive department")
    void testGetDepartmentByIdThrows404ForInactive() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(departmentRepository.findById(inactiveDeptId)).thenReturn(Optional.of(inactiveDept));

        assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.getDepartmentById(inactiveDeptId)
        );
    }

    @Test
    @DisplayName("Subdepartments of inactive department throw 404 when requested by deptId")
    void testSubDepartmentsOfInactiveDepartmentThrow404() {
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(departmentRepository.findById(inactiveDeptId)).thenReturn(Optional.of(inactiveDept));

        assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.getSubDepartmentsByDeptId(inactiveDeptId, true)
        );
    }

    @Test
    @DisplayName("Get subdepartment by ID throws 404 if its parent department is inactive")
    void testSubDepartmentUnderInactiveParentDepartmentThrows404() {
        UUID subDeptId = subDeptUnderInactiveDept.getId();
        when(scopeSecurity.getCurrentPrincipal()).thenReturn(superAdminPrincipal);
        when(subDepartmentRepository.findById(subDeptId)).thenReturn(Optional.of(subDeptUnderInactiveDept));

        assertThrows(ResourceNotFoundException.class, () ->
                departmentService.getSubDepartmentById(subDeptId)
        );
    }

    @Test
    @DisplayName("User list returns only active users by default")
    void testUserListReturnsOnlyActiveByDefault() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findByIsActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(activeUser)));

        var response = userService.getAllUsers(pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("active_user", response.getContent().get(0).getUsername());
        verify(userRepository).findByIsActiveTrue(pageable);
        verify(userRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("Get user by ID throws 404 for inactive user")
    void testGetUserByIdThrows404ForInactiveUser() {
        UUID inactiveUserId = inactiveUser.getId();
        when(userRepository.findByIdAndIsActiveTrue(inactiveUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                userService.getUserById(inactiveUserId)
        );
    }

    @Test
    @DisplayName("Get task template by ID throws 404 for inactive template")
    void testGetTaskTemplateByIdThrows404ForInactive() {
        UUID templateId = inactiveTemplate.getId();
        when(taskTemplateRepository.findActiveByIdWithDetails(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                taskTemplateService.getTemplateById(templateId)
        );
    }

    @Test
    @DisplayName("Get task by ID throws 404 for soft-deleted task")
    void testGetTaskByIdThrows404ForSoftDeletedTask() {
        UUID taskId = softDeletedTask.getId();
        when(taskRepository.findActiveByIdWithDetails(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                taskService.getTaskById(taskId)
        );
    }

    @Test
    @DisplayName("Task proofs download throws 404 when parent task is soft-deleted")
    void testTaskProofDownloadThrows404ForDeletedTask() {
        UUID proofId = UUID.randomUUID();
        TaskProof proof = TaskProof.builder()
                .task(softDeletedTask)
                .proofType(ProofType.DOCUMENT)
                .fileUrl("uploads/test.pdf")
                .build();
        proof.setId(proofId);

        when(proofRepository.findById(proofId)).thenReturn(Optional.of(proof));

        assertThrows(ResourceNotFoundException.class, () ->
                taskProofService.downloadProofFile(proofId)
        );
    }

    @Test
    @DisplayName("Task requests getRequestById throws 404 when parent task is soft-deleted")
    void testTaskRequestGetByIdThrows404ForDeletedTask() {
        UUID requestId = UUID.randomUUID();
        TaskRequest request = TaskRequest.builder()
                .task(softDeletedTask)
                .requestType(TaskRequestType.TASK_CLOSURE)
                .status(TaskRequestStatus.PENDING)
                .build();
        request.setId(requestId);

        when(requestRepository.findByIdWithDetails(requestId)).thenReturn(Optional.of(request));

        assertThrows(ResourceNotFoundException.class, () ->
                taskRequestService.getRequestById(requestId)
        );
    }

    @Test
    @DisplayName("DepartmentMapper filters out inactive subdepartments from DepartmentDto")
    void testDepartmentMapperFiltersInactiveSubDepartments() {
        SubDepartment inactiveSubDept = SubDepartment.builder()
                .department(activeDept)
                .name("Inactive SubDept")
                .isActive(false)
                .build();
        inactiveSubDept.setId(UUID.randomUUID());

        activeDept.setSubDepartments(List.of(activeSubDeptUnderActiveDept, inactiveSubDept));

        DepartmentDto dto = departmentMapper.toDto(activeDept);

        assertNotNull(dto);
        assertEquals(1, dto.getSubDepartments().size());
        assertEquals("Software Engineering", dto.getSubDepartments().get(0).getName());
    }

    @Test
    @DisplayName("TaskTemplateMapper filters out inactive applicable departments")
    void testTaskTemplateMapperFiltersInactiveDepartments() {
        TaskTemplateDto dto = taskTemplateMapper.toDto(activeTemplate);

        assertNotNull(dto);
        assertEquals(1, dto.getApplicableDepartments().size());
        assertEquals("Computer Science", dto.getApplicableDepartments().get(0).getName());
    }
}
