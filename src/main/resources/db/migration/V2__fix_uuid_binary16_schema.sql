-- ==============================================================================
-- DATABASE MIGRATION: V2__fix_uuid_binary16_schema.sql
-- Goal: Fix UUID Primary-Key & Foreign-Key Schema Mismatch (BINARY(36) -> BINARY(16))
-- Description:
--   Hibernate previously generated `BINARY(36)` columns due to `length = 36` on UUID fields.
--   MySQL padded these with 20 trailing zeros (0x00), breaking Hibernate's 16-byte
--   `findById(UUID)` binary equality matching.
--   This migration converts all UUID primary keys and foreign keys to standard BINARY(16).
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- STEP 0: PRE-MIGRATION DIAGNOSTIC & VERIFICATION QUERIES
-- Run these before applying changes to confirm data format:
-- ------------------------------------------------------------------------------
/*
-- 0.1 Check how departments.id is stored:
SELECT
    HEX(id) AS id_hex,
    LENGTH(id) AS stored_length,
    BIN_TO_UUID(SUBSTRING(id, 1, 16)) AS uuid_from_first_16_bytes,
    CONVERT(id USING utf8mb4) AS possible_text_value
FROM departments;

-- 0.2 Check sub_departments foreign keys:
SELECT
    HEX(department_id) AS dept_fk_hex,
    LENGTH(department_id) AS stored_length,
    BIN_TO_UUID(SUBSTRING(department_id, 1, 16)) AS uuid_from_first_16_bytes
FROM sub_departments;

-- Expected result:
--   stored_length = 36
--   uuid_from_first_16_bytes matches the expected UUID string
--   The last 40 hex characters are zeros (0000000000000000000000000000000000000000)
*/

-- ------------------------------------------------------------------------------
-- STEP 1: SAFETY CHECKS & SETTINGS
-- ------------------------------------------------------------------------------
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- STEP 2: CONVERT PRIMARY & FOREIGN KEY COLUMNS TO BINARY(16)
-- Truncating padded 36-byte binary to 16 bytes preserves the exact 16 UUID bytes.
-- ------------------------------------------------------------------------------

-- 2.1 DEPARTMENTS (Parent table)
-- If data is padded binary (first 16 bytes = UUID, remaining 20 bytes = 0x00):
ALTER TABLE departments
    MODIFY COLUMN id BINARY(16) NOT NULL;

-- 2.2 SUB_DEPARTMENTS
ALTER TABLE sub_departments
    MODIFY COLUMN id BINARY(16) NOT NULL,
    MODIFY COLUMN department_id BINARY(16) NOT NULL;

-- 2.3 USERS & USER RELATIONS
ALTER TABLE users
    MODIFY COLUMN id BINARY(16) NOT NULL;

-- Many-to-many: users <-> departments
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users_departments') THEN
    ALTER TABLE users_departments
        MODIFY COLUMN user_id BINARY(16) NOT NULL,
        MODIFY COLUMN department_id BINARY(16) NOT NULL;
END IF;

-- Many-to-many: users <-> sub_departments
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users_sub_departments') THEN
    ALTER TABLE users_sub_departments
        MODIFY COLUMN user_id BINARY(16) NOT NULL,
        MODIFY COLUMN sub_department_id BINARY(16) NOT NULL;
END IF;

-- 2.4 ROLES & PERMISSIONS
ALTER TABLE roles
    MODIFY COLUMN id BINARY(16) NOT NULL;

ALTER TABLE permissions
    MODIFY COLUMN id BINARY(16) NOT NULL;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'role_permissions') THEN
    ALTER TABLE role_permissions
        MODIFY COLUMN role_id BINARY(16) NOT NULL,
        MODIFY COLUMN permission_id BINARY(16) NOT NULL;
END IF;

-- 2.5 USER ROLE ASSIGNMENTS
ALTER TABLE user_role_assignments
    MODIFY COLUMN id BINARY(16) NOT NULL,
    MODIFY COLUMN user_id BINARY(16) NOT NULL,
    MODIFY COLUMN role_id BINARY(16) NOT NULL,
    MODIFY COLUMN department_id BINARY(16) DEFAULT NULL,
    MODIFY COLUMN sub_department_id BINARY(16) DEFAULT NULL;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_assignment_custom_departments') THEN
    ALTER TABLE user_assignment_custom_departments
        MODIFY COLUMN assignment_id BINARY(16) NOT NULL,
        MODIFY COLUMN department_id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_assignment_custom_sub_departments') THEN
    ALTER TABLE user_assignment_custom_sub_departments
        MODIFY COLUMN assignment_id BINARY(16) NOT NULL,
        MODIFY COLUMN sub_department_id BINARY(16) NOT NULL;
END IF;

-- 2.6 TASK TEMPLATES
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_templates') THEN
    ALTER TABLE task_templates
        MODIFY COLUMN id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_template_departments') THEN
    ALTER TABLE task_template_departments
        MODIFY COLUMN template_id BINARY(16) NOT NULL,
        MODIFY COLUMN department_id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_template_sub_departments') THEN
    ALTER TABLE task_template_sub_departments
        MODIFY COLUMN template_id BINARY(16) NOT NULL,
        MODIFY COLUMN sub_department_id BINARY(16) NOT NULL;
END IF;

-- 2.7 TASKS
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tasks') THEN
    ALTER TABLE tasks
        MODIFY COLUMN id BINARY(16) NOT NULL,
        MODIFY COLUMN creator_id BINARY(16) NOT NULL,
        MODIFY COLUMN started_by_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN closed_by_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN extended_by_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN last_rejected_by_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN template_id BINARY(16) DEFAULT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_departments') THEN
    ALTER TABLE task_departments
        MODIFY COLUMN task_id BINARY(16) NOT NULL,
        MODIFY COLUMN department_id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_sub_departments') THEN
    ALTER TABLE task_sub_departments
        MODIFY COLUMN task_id BINARY(16) NOT NULL,
        MODIFY COLUMN sub_department_id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_assignees') THEN
    ALTER TABLE task_assignees
        MODIFY COLUMN task_id BINARY(16) NOT NULL,
        MODIFY COLUMN user_id BINARY(16) NOT NULL;
END IF;

-- 2.8 TASK REQUESTS & PROOFS
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_requests') THEN
    ALTER TABLE task_requests
        MODIFY COLUMN id BINARY(16) NOT NULL,
        MODIFY COLUMN task_id BINARY(16) NOT NULL,
        MODIFY COLUMN reviewer_id BINARY(16) DEFAULT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_proof_requirements') THEN
    ALTER TABLE task_proof_requirements
        MODIFY COLUMN id BINARY(16) NOT NULL,
        MODIFY COLUMN template_id BINARY(16) NOT NULL;
END IF;

IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'task_proofs') THEN
    ALTER TABLE task_proofs
        MODIFY COLUMN id BINARY(16) NOT NULL,
        MODIFY COLUMN task_id BINARY(16) NOT NULL,
        MODIFY COLUMN task_request_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN proof_requirement_id BINARY(16) DEFAULT NULL,
        MODIFY COLUMN uploaded_by_id BINARY(16) NOT NULL;
END IF;

-- 2.9 AUDIT LOGS
IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'audit_logs') THEN
    ALTER TABLE audit_logs
        MODIFY COLUMN id BINARY(16) NOT NULL;
END IF;

-- ------------------------------------------------------------------------------
-- STEP 3: RESTORE FOREIGN KEY CHECKS
-- ------------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- ------------------------------------------------------------------------------
-- STEP 4: POST-MIGRATION VERIFICATION
-- Run these queries after applying the migration to verify consistency:
-- ------------------------------------------------------------------------------
/*
-- 4.1 Confirm BIN_TO_UUID works directly on the 16-byte id:
SELECT
    BIN_TO_UUID(id) AS id,
    name,
    code,
    LENGTH(id) AS byte_length
FROM departments;

-- 4.2 Verify sub_departments join integrity:
SELECT
    BIN_TO_UUID(s.id) AS sub_dept_id,
    s.name AS sub_dept_name,
    BIN_TO_UUID(d.id) AS dept_id,
    d.name AS dept_name
FROM sub_departments s
JOIN departments d ON s.department_id = d.id;
*/
