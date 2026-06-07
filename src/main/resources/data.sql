-- =============================================================
-- SEED DATA  (H2 dev only — idempotent via MERGE)
-- =============================================================

-- Biller
MERGE INTO biller (id, code, name, short_name, biller_type, is_active) KEY(id)
VALUES (1, 'DHAKA_MODEL_CLG', 'Dhaka Model College', 'DMC', 'EDUCATIONAL', TRUE);

-- Institution
MERGE INTO institution (id, biller_id, institution_type, eiin_no, board) KEY(id)
VALUES (1, 1, 'SCHOOL_COLLEGE', 'EIIN-108104', 'Dhaka');

-- Institution sections
MERGE INTO institution_section (id, institution_id, section_type, label, is_active) KEY(id) VALUES
(1, 1, 'SCHOOL',  'School Section',  TRUE),
(2, 1, 'COLLEGE', 'College Section', TRUE);

-- Bank accounts (preset — shown in dropdown)
MERGE INTO bank_account (id, biller_id, account_code, bank_name, branch_name, account_no, account_name, is_active, sort_order) KEY(id) VALUES
(1, 1, 'ACC_DBBL',   'Dutch Bangla Bank Ltd (DBBL)', 'Mirpur Branch',    '2019****1234', 'Dhaka Model College General A/C', TRUE, 1),
(2, 1, 'ACC_SONALI', 'Sonali Bank Ltd',               'Dhanmondi Branch', '0012****5678', 'Dhaka Model College Exam Fund',   TRUE, 2),
(3, 1, 'ACC_ISLAMI', 'Islami Bank Bangladesh Ltd',    'Mirpur Branch',    '1301****9900', 'Dhaka Model College Development Fund', TRUE, 3);

-- Fee item catalogue (default fee rows per section)
MERGE INTO fee_item_catalogue (id, biller_id, fee_code, fee_name, account_head, default_account_id, applicable_section, is_active, sort_order) KEY(id) VALUES
(1,  1, 'TUITION_FEE',    'Tuition Fee',           'Revenue – Tuition',      1, 'ALL',        TRUE, 1),
(2,  1, 'SESSION_FEE',    'Session Fee',            'Revenue – Session',      1, 'ALL',        TRUE, 2),
(3,  1, 'EXAM_FEE',       'Exam Fee',               'Revenue – Exam',         2, 'ALL',        TRUE, 3),
(4,  1, 'LAB_FEE',        'Lab Fee',                'Revenue – Lab',          1, 'COLLEGE',    TRUE, 4),
(5,  1, 'DEV_FUND',       'Development Fund',       'Fund – Development',     3, 'ALL',        TRUE, 5),
(6,  1, 'REG_FEE',        'Registration Fee',       'Revenue – Registration', 1, 'ALL',        TRUE, 6),
(7,  1, 'TRANSPORT_FEE',  'Transport Fee',          'Revenue – Transport',    1, 'SCHOOL',     TRUE, 7),
(8,  1, 'PRAYER_HALL',    'Prayer Hall Fee',        'Revenue – Misc',         1, 'ALL',        TRUE, 8),
(9,  1, 'MGMT_FEE',       'Management Fee',         'Revenue – Management',   1, 'ALL',        TRUE, 9),
(10, 1, 'PRACTICAL_FEE',  'Practical Center Fee',   'Revenue – Practical',    2, 'COLLEGE',    TRUE, 10);

-- Portal user (admin)
-- password = 'admin123' (BCrypt hash — replace with real hash in prod)
MERGE INTO portal_user (id, biller_id, username, password_hash, full_name, email, role, is_active) KEY(id)
VALUES (1, 1, 'admin', '$2a$10$xn3LI/AjqicFYZFruSwve.68134NJSXVmfeEMqkm6wPXPSN6.DI2e', 'System Admin', 'admin@dmc.edu.bd', 'ADMIN', TRUE);