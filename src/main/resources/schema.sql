-- =============================================================
-- MERCHANT PORTAL — H2 SCHEMA
-- Compatible with H2 in-memory for dev.
-- Oracle 19c migration: swap BIGINT→NUMBER(19), VARCHAR→VARCHAR2,
-- BOOLEAN→NUMBER(1), remove AUTO_INCREMENT, add sequences + triggers.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- BILLER / MERCHANT  (future: one per institution/utility/govt)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS biller (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(50)   NOT NULL UNIQUE,          -- e.g. DHAKA_MODEL_CLG
    name                VARCHAR(200)  NOT NULL,
    short_name          VARCHAR(80),
    biller_type         VARCHAR(30)   NOT NULL DEFAULT 'EDUCATIONAL',
                                    -- EDUCATIONAL | UTILITY | GOVT | MERCHANT
    logo_url            VARCHAR(500),
    address             VARCHAR(500),
    contact_email       VARCHAR(150),
    contact_phone       VARCHAR(30),
    website             VARCHAR(200),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- INSTITUTION  (educational biller detail)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS institution (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id           BIGINT        NOT NULL REFERENCES biller(id),
    institution_type    VARCHAR(50)   NOT NULL,   -- SCHOOL | COLLEGE | UNIVERSITY | SCHOOL_COLLEGE | COLLEGE_UNIVERSITY | ALL
    eiin_no             VARCHAR(30),              -- Education Institute Identification Number
    board               VARCHAR(80),              -- Dhaka | Chittagong | etc.
    established_year    INT,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ─────────────────────────────────────────────────────────────
-- INSTITUTION SECTION  (school / college / university wing)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS institution_section (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    institution_id      BIGINT        NOT NULL REFERENCES institution(id),
    section_type        VARCHAR(20)   NOT NULL,   -- SCHOOL | COLLEGE | UNIVERSITY
    label               VARCHAR(100),             -- e.g. "School Wing", "Day College"
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ─────────────────────────────────────────────────────────────
-- BANK ACCOUNT  (preset per biller — dropdown source)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bank_account (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id           BIGINT        NOT NULL REFERENCES biller(id),
    account_code        VARCHAR(50)   NOT NULL,   -- internal code e.g. ACC_A
    bank_name           VARCHAR(150)  NOT NULL,
    branch_name         VARCHAR(150),
    account_no          VARCHAR(50)   NOT NULL,
    account_name        VARCHAR(200),
    routing_no          VARCHAR(30),
    account_type        VARCHAR(30),              -- CURRENT | SAVINGS | STD_CHARTERED
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order          INT           NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (biller_id, account_code)
);

-- ─────────────────────────────────────────────────────────────
-- PREDEFINED FEE ITEM  (fee catalogue per biller — default rows in fee table)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fee_item_catalogue (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id           BIGINT        NOT NULL REFERENCES biller(id),
    fee_code            VARCHAR(50)   NOT NULL,   -- e.g. TUITION_FEE, EXAM_FEE
    fee_name            VARCHAR(150)  NOT NULL,
    account_head        VARCHAR(150),             -- accounting head label
    default_account_id  BIGINT        REFERENCES bank_account(id),
    applicable_section  VARCHAR(20),              -- SCHOOL | COLLEGE | UNIVERSITY | ALL
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order          INT           NOT NULL DEFAULT 0,
    UNIQUE (biller_id, fee_code)
);

-- ─────────────────────────────────────────────────────────────
-- BILL  (master bill definition)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill (
    id                      BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id               BIGINT        NOT NULL REFERENCES biller(id),
    bill_code               VARCHAR(80)   NOT NULL UNIQUE,      -- auto-generated
    bill_title              VARCHAR(200)  NOT NULL,
    bill_type               VARCHAR(30)   NOT NULL,
                            -- MONTHLY | ONE_TIME | SEMESTER | EXAM | ADMISSION | FORM_FILLUP | CUSTOM
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
                            -- DRAFT | PUBLISHED | INACTIVE | ARCHIVED
    version                 INT           NOT NULL DEFAULT 1,
    parent_bill_id          BIGINT        REFERENCES bill(id),  -- for versioning after payments
    bill_start_date         DATE          NOT NULL,
    bill_end_date           DATE          NOT NULL,
    academic_year           VARCHAR(20),
    bill_description        CLOB,

    -- Classification
    institution_section_id  BIGINT        REFERENCES institution_section(id),
    classification_json     CLOB,
    -- Stores: {section, schoolClass, shift, section, group, version,
    --          collegeYear, faculty, department, program, yearSemester, session}

    -- University payment mode
    uni_pay_mode            VARCHAR(20),  -- ONE_TIME | SEMESTER | MONTHLY

    -- Payment / form config
    allow_partial_payment   BOOLEAN       NOT NULL DEFAULT FALSE,
    allow_month_selection   BOOLEAN       NOT NULL DEFAULT TRUE,
                            -- student can choose which months to pay
    student_id_type         VARCHAR(30)   DEFAULT 'STUDENT_ID',
                            -- STUDENT_ID | ROLL_NO | MOBILE | REGISTRATION_NO | EMAIL
    is_open_admission       BOOLEAN       NOT NULL DEFAULT FALSE,
                            -- TRUE = any student can pay (e.g. admission fee, no pre-upload needed)
    require_form_fill       BOOLEAN       NOT NULL DEFAULT FALSE,
                            -- TRUE = student fills dynamic form before payment

    created_by              BIGINT,               -- user id
    published_at            TIMESTAMP,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- BILL MONTH  (which months are in this bill — not limited to 12)
-- e.g. Jul-26 to Jul-27 with any gap: store each active month row
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_month (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    bill_id         BIGINT      NOT NULL REFERENCES bill(id) ON DELETE CASCADE,
    month_label     VARCHAR(30) NOT NULL,   -- e.g. JAN | FEB | JUL_2026 | SEMESTER_1
    month_year      INT,                    -- e.g. 2026  (null for semesters)
    month_seq       INT         NOT NULL,   -- display/sort order
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (bill_id, month_label, month_year)
);

-- ─────────────────────────────────────────────────────────────
-- BILL FEE  (fee items per bill — each row is one fee)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_fee (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    bill_id             BIGINT          NOT NULL REFERENCES bill(id) ON DELETE CASCADE,
    catalogue_item_id   BIGINT          REFERENCES fee_item_catalogue(id),
                        -- NULL = user-defined fee (not from catalogue)
    fee_code            VARCHAR(50)     NOT NULL,
    fee_name            VARCHAR(150)    NOT NULL,
    account_head        VARCHAR(150),
    bank_account_id     BIGINT          NOT NULL REFERENCES bank_account(id),
    sort_order          INT             NOT NULL DEFAULT 0,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ─────────────────────────────────────────────────────────────
-- BILL FEE AMOUNT  (amount per fee per month)
-- For ONE_TIME bills: one row per fee with month_id NULL
-- For MONTHLY bills: one row per fee per month
-- For SEMESTER bills: month_label = 'SEMESTER_1', 'SEMESTER_2', etc.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_fee_amount (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    bill_fee_id     BIGINT          NOT NULL REFERENCES bill_fee(id) ON DELETE CASCADE,
    bill_month_id   BIGINT          REFERENCES bill_month(id) ON DELETE CASCADE,
                    -- NULL for ONE_TIME bills
    month_label     VARCHAR(30),    -- key from amounts map (e.g. JAN_2026)
    amount          DECIMAL(12,2)   NOT NULL DEFAULT 0,
    is_waivable     BOOLEAN         NOT NULL DEFAULT FALSE,
    UNIQUE (bill_fee_id, bill_month_id)
);

-- ─────────────────────────────────────────────────────────────
-- BILL LPF  (late payment fine config)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_lpf (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    bill_id         BIGINT          NOT NULL REFERENCES bill(id) ON DELETE CASCADE,
    is_enabled      BOOLEAN         NOT NULL DEFAULT FALSE,
    lpf_start_date  DATE,
    lpf_end_date    DATE,           -- NULL = indefinite
    fine_type       VARCHAR(30),    -- FIXED | PERCENT | DAILY_FIXED | DAILY_PCT
    fine_amount     DECIMAL(10,2),
    fine_scope      VARCHAR(20),    -- ENTIRE | PER_FEE
    max_cap         DECIMAL(10,2),  -- NULL = no cap
    grace_days      INT             DEFAULT 0,
    recurrence      VARCHAR(20)     DEFAULT 'ONCE', -- ONCE | DAILY | MONTHLY
    waiver_role     VARCHAR(30),    -- ADMIN | PRINCIPAL | BOTH | NULL
    UNIQUE (bill_id)
);

-- ─────────────────────────────────────────────────────────────
-- BILL LPF FEE MAP  (which fees have LPF when scope = PER_FEE)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_lpf_fee (
    id              BIGINT  AUTO_INCREMENT PRIMARY KEY,
    bill_lpf_id     BIGINT  NOT NULL REFERENCES bill_lpf(id) ON DELETE CASCADE,
    bill_fee_id     BIGINT  NOT NULL REFERENCES bill_fee(id) ON DELETE CASCADE,
    UNIQUE (bill_lpf_id, bill_fee_id)
);

-- ─────────────────────────────────────────────────────────────
-- BILL DYNAMIC FORM  (form schema shown to student at payment)
-- Stored as JSON schema: [{fieldCode, fieldType, label, required, options:[]}]
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_form_config (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    bill_id         BIGINT      NOT NULL REFERENCES bill(id) ON DELETE CASCADE,
    form_schema     CLOB        NOT NULL,   -- JSON array of field definitions
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bill_id)
);

-- ─────────────────────────────────────────────────────────────
-- STUDENT  (biller-scoped student record)
-- One student can exist across multiple billers
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id           BIGINT        NOT NULL REFERENCES biller(id),
    student_uid         VARCHAR(80)   NOT NULL,     -- unique within biller: roll, student_id, mobile etc.
    uid_type            VARCHAR(30)   NOT NULL DEFAULT 'STUDENT_ID',
    full_name           VARCHAR(200)  NOT NULL,
    mobile              VARCHAR(20),
    email               VARCHAR(150),
    dob                 DATE,
    gender              VARCHAR(10),
    address             CLOB,
    extra_data          CLOB,         -- JSON: any additional fields from CSV upload
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (biller_id, student_uid, uid_type)
);

-- ─────────────────────────────────────────────────────────────
-- BILL STUDENT  (students enrolled/assigned to a bill)
-- Can be pre-loaded via CSV or added manually
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bill_student (
    id                      BIGINT        AUTO_INCREMENT PRIMARY KEY,
    bill_id                 BIGINT        NOT NULL REFERENCES bill(id),
    student_id              BIGINT        REFERENCES student(id),
                            -- NULL if open-admission (walk-in); student record created at payment time
    classification_json     CLOB,
    -- e.g. {group:"SCIENCE", class:"9", section:"A"} — student's specific category
    -- needed because a bill may cover all groups but each student belongs to one
    enrolment_source        VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
                            -- MANUAL | CSV_UPLOAD | SELF_REGISTRATION
    upload_batch_id         BIGINT,       -- references student_upload_batch if from CSV
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    added_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bill_id, student_id)
);

-- ─────────────────────────────────────────────────────────────
-- STUDENT UPLOAD BATCH  (track CSV/Excel uploads per bill)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_upload_batch (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    bill_id         BIGINT        NOT NULL REFERENCES bill(id),
    file_name       VARCHAR(300)  NOT NULL,
    file_type       VARCHAR(10),              -- CSV | XLSX
    total_rows      INT,
    success_rows    INT,
    failed_rows     INT,
    error_log       CLOB,                     -- JSON array of row errors
    status          VARCHAR(20)   NOT NULL DEFAULT 'PROCESSING',
                    -- PROCESSING | COMPLETED | FAILED | PARTIAL
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- PAYMENT TRANSACTION  (universal — works for edu, utility, govt, merchant)
-- This table is the core future-proof payment record
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_transaction (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,

    -- Links to bill and biller
    biller_id               BIGINT          NOT NULL REFERENCES biller(id),
    bill_id                 BIGINT          NOT NULL REFERENCES bill(id),
    bill_student_id         BIGINT          REFERENCES bill_student(id),
                            -- NULL for open/walk-in payments

    -- Payer info (stored directly for non-student billers too)
    payer_name              VARCHAR(200),
    payer_mobile            VARCHAR(20),
    payer_email             VARCHAR(150),
    payer_reference         VARCHAR(100),   -- student ID, roll, mobile, or any reference
    payer_extra_data        CLOB,           -- JSON: dynamic form answers

    -- Payment breakdown
    total_amount            DECIMAL(12,2)   NOT NULL,
    paid_amount             DECIMAL(12,2),
    discount_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0,
    fine_amount             DECIMAL(12,2)   NOT NULL DEFAULT 0,
    net_payable             DECIMAL(12,2)   NOT NULL,
    currency                VARCHAR(5)      NOT NULL DEFAULT 'BDT',

    -- Which months were paid (JSON array of bill_month ids)
    paid_months_json        CLOB,           -- e.g. [1,2,3] bill_month ids

    -- Payment method / gateway
    payment_channel         VARCHAR(30),    -- CASH | BKASH | NAGAD | ROCKET | BANK | CARD | ONLINE
    payment_gateway         VARCHAR(50),
    gateway_txn_id          VARCHAR(200),   -- gateway reference
    gateway_response        CLOB,           -- full gateway JSON response

    -- Bank account(s) credited (JSON: [{accountId, amount}])
    credited_accounts_json  CLOB,

    -- Status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                            -- PENDING | COMPLETED | FAILED | CANCELLED | REFUNDED | PARTIAL
    payment_date            TIMESTAMP,
    challan_no              VARCHAR(100)    UNIQUE,     -- printable challan reference
    remarks                 VARCHAR(500),

    -- Audit
    created_by              BIGINT,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- PAYMENT TRANSACTION FEE LINE  (per-fee breakdown within a payment)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_transaction_fee (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    transaction_id      BIGINT          NOT NULL REFERENCES payment_transaction(id) ON DELETE CASCADE,
    bill_fee_id         BIGINT          NOT NULL REFERENCES bill_fee(id),
    bill_month_id       BIGINT          REFERENCES bill_month(id),
    fee_name            VARCHAR(150)    NOT NULL,
    base_amount         DECIMAL(12,2)   NOT NULL,
    fine_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0,
    net_amount          DECIMAL(12,2)   NOT NULL,
    bank_account_id     BIGINT          REFERENCES bank_account(id),
    account_head        VARCHAR(150)
);

-- ─────────────────────────────────────────────────────────────
-- PORTAL USER  (admin / operator / teacher)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS portal_user (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    biller_id       BIGINT        NOT NULL REFERENCES biller(id),
    username        VARCHAR(80)   NOT NULL UNIQUE,
    password_hash   VARCHAR(200)  NOT NULL,
    full_name       VARCHAR(200)  NOT NULL,
    email           VARCHAR(150)  UNIQUE,
    mobile          VARCHAR(20),
    role            VARCHAR(30)   NOT NULL DEFAULT 'OPERATOR',
                    -- SUPER_ADMIN | ADMIN | PRINCIPAL | OPERATOR | TEACHER
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- INDEXES  (H2 supports standard CREATE INDEX)
-- ─────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_bill_biller      ON bill(biller_id, status);
CREATE INDEX IF NOT EXISTS idx_bill_code        ON bill(bill_code);
CREATE INDEX IF NOT EXISTS idx_bill_fee_bill    ON bill_fee(bill_id);
CREATE INDEX IF NOT EXISTS idx_bill_month_bill  ON bill_month(bill_id);
CREATE INDEX IF NOT EXISTS idx_txn_bill         ON payment_transaction(bill_id, status);
CREATE INDEX IF NOT EXISTS idx_txn_payer        ON payment_transaction(payer_reference, biller_id);
CREATE INDEX IF NOT EXISTS idx_txn_challan      ON payment_transaction(challan_no);
CREATE INDEX IF NOT EXISTS idx_student_uid      ON student(biller_id, student_uid);
CREATE INDEX IF NOT EXISTS idx_bill_student     ON bill_student(bill_id, student_id);