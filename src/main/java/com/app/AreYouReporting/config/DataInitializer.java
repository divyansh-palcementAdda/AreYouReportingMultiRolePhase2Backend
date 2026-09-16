package com.app.AreYouReporting.config;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.service.interfaces.PermissionService;
import com.app.AreYouReporting.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermissionService permissionService;
    private final RoleService roleService;
    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.default-users:true}")
    private boolean seedDefaultUsers;

    @Value("${app.default.super-admin.username:superadmin}")
    private String superAdminUsername;

    @Value("${app.default.super-admin.password:SuperAdmin@123}")
    private String superAdminPassword;

    @Value("${app.default.super-admin.email:superadmin@rcef.com}")
    private String superAdminEmail;

    @Value("${app.default.admin.username:admin}")
    private String adminUsername;

    @Value("${app.default.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.default.admin.email:admin@rcef.com}")
    private String adminEmail;

    @Value("${app.default.sub-admin.username:subadmin}")
    private String subAdminUsername;

    @Value("${app.default.sub-admin.password:SubAdmin@123}")
    private String subAdminPassword;

    @Value("${app.default.sub-admin.email:subadmin@rcef.com}")
    private String subAdminEmail;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting DataInitializer: seeding permissions and roles...");
        permissionService.seedBaselinePermissions();
        roleService.seedBaselineRoles();

        seedDefaultDepartments();

        if (seedDefaultUsers) {
            seedUsers();
        }
        log.info("DataInitializer completed successfully.");
    }

    private void seedDefaultDepartments() {
        if (departmentRepository.count() == 0) {
            Department cse = Department.builder()
                    .name("Computer Science & Engineering")
                    .code("CSE")
                    .description("Department of Computer Science & Engineering")
                    .isActive(true)
                    .build();
            cse = departmentRepository.save(cse);

            SubDepartment ai = SubDepartment.builder()
                    .department(cse)
                    .name("Artificial Intelligence & ML")
                    .code("CSE-AIML")
                    .description("AI and Machine Learning Specialization")
                    .isActive(true)
                    .build();
            subDepartmentRepository.save(ai);

            SubDepartment ds = SubDepartment.builder()
                    .department(cse)
                    .name("Data Science")
                    .code("CSE-DS")
                    .description("Data Science Specialization")
                    .isActive(true)
                    .build();
            subDepartmentRepository.save(ds);

            Department mgmt = Department.builder()
                    .name("Management Studies")
                    .code("MGMT")
                    .description("Department of Management Studies")
                    .isActive(true)
                    .build();
            mgmt = departmentRepository.save(mgmt);

            SubDepartment finance = SubDepartment.builder()
                    .department(mgmt)
                    .name("Finance & Banking")
                    .code("MGMT-FIN")
                    .description("Finance Division")
                    .isActive(true)
                    .build();
            subDepartmentRepository.save(finance);

            log.info("Default departments and sub-departments seeded.");
        }
    }

    private void seedUsers() {
        Department cse = departmentRepository.findByCode("CSE").orElse(null);
        SubDepartment aiml = subDepartmentRepository.findByDepartmentIdAndCode(cse != null ? cse.getId() : null, "CSE-AIML").orElse(null);

        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN").orElse(null);
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        Role subAdminRole = roleRepository.findByName("SUB_ADMIN").orElse(null);
        Role hodRole = roleRepository.findByName("HOD").orElse(null);
        Role teacherRole = roleRepository.findByName("TEACHER").orElse(null);

        // 1. Super Admin
        if (!userRepository.existsByUsername(superAdminUsername)) {
            User superAdmin = User.builder()
                    .username(superAdminUsername)
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .fullName("Global Super Administrator")
                    .phoneNumber("+1-800-000001")
                    .isActive(true)
                    .build();
            superAdmin = userRepository.save(superAdmin);

            if (superAdminRole != null) {
                UserRoleAssignment saAssignment = UserRoleAssignment.builder()
                        .user(superAdmin)
                        .role(superAdminRole)
                        .dataScopeType(DataScopeType.GLOBAL)
                        .isActive(true)
                        .build();
                userRoleAssignmentRepository.save(saAssignment);
            }
            log.info("Super Admin user seeded: {}", superAdminUsername);
        }

        // 2. Admin
        if (!userRepository.existsByUsername(adminUsername)) {
            Set<Department> depts = new HashSet<>();
            if (cse != null) depts.add(cse);

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName("CSE Department Administrator")
                    .phoneNumber("+1-800-000002")
                    .departments(depts)
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);

            if (adminRole != null) {
                UserRoleAssignment adminAssignment = UserRoleAssignment.builder()
                        .user(admin)
                        .role(adminRole)
                        .department(cse)
                        .dataScopeType(DataScopeType.DEPARTMENT)
                        .isActive(true)
                        .build();
                userRoleAssignmentRepository.save(adminAssignment);
            }
            log.info("Admin user seeded: {}", adminUsername);
        }

        // 3. Sub-Admin
        if (!userRepository.existsByUsername(subAdminUsername)) {
            Set<Department> depts = new HashSet<>();
            if (cse != null) depts.add(cse);

            User subAdmin = User.builder()
                    .username(subAdminUsername)
                    .email(subAdminEmail)
                    .password(passwordEncoder.encode(subAdminPassword))
                    .fullName("CSE Sub-Administrator")
                    .phoneNumber("+1-800-000003")
                    .departments(depts)
                    .isActive(true)
                    .build();
            subAdmin = userRepository.save(subAdmin);

            if (subAdminRole != null) {
                UserRoleAssignment subAdminAssignment = UserRoleAssignment.builder()
                        .user(subAdmin)
                        .role(subAdminRole)
                        .department(cse)
                        .dataScopeType(DataScopeType.DEPARTMENT)
                        .isActive(true)
                        .build();
                userRoleAssignmentRepository.save(subAdminAssignment);
            }
            log.info("Sub-Admin user seeded: {}", subAdminUsername);
        }

        // 4. Default HOD / Multi-role Teacher Demo
        if (!userRepository.existsByUsername("dr.sharma")) {
            Set<Department> depts = new HashSet<>();
            Set<SubDepartment> subDepts = new HashSet<>();
            if (cse != null) depts.add(cse);
            if (aiml != null) subDepts.add(aiml);

            User prof = User.builder()
                    .username("dr.sharma")
                    .email("dr.sharma@rcef.com")
                    .password(passwordEncoder.encode("Sharma@123"))
                    .fullName("Dr. Rajesh Sharma")
                    .phoneNumber("+1-800-000004")
                    .departments(depts)
                    .subDepartments(subDepts)
                    .isActive(true)
                    .build();
            prof = userRepository.save(prof);

            if (hodRole != null) {
                UserRoleAssignment hodAssignment = UserRoleAssignment.builder()
                        .user(prof)
                        .role(hodRole)
                        .department(cse)
                        .subDepartment(aiml)
                        .dataScopeType(DataScopeType.SUB_DEPARTMENT)
                        .isActive(true)
                        .build();
                userRoleAssignmentRepository.save(hodAssignment);
            }

            if (teacherRole != null) {
                UserRoleAssignment teacherAssignment = UserRoleAssignment.builder()
                        .user(prof)
                        .role(teacherRole)
                        .department(cse)
                        .subDepartment(aiml)
                        .dataScopeType(DataScopeType.SELF)
                        .isActive(true)
                        .build();
                userRoleAssignmentRepository.save(teacherAssignment);
            }
            log.info("Multi-role HOD/Teacher user seeded: dr.sharma");
        }
    }
}
