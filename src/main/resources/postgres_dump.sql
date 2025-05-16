-- PostgreSQL version of the MySQL dump

-- Table: "user"
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone BIGINT UNIQUE NOT NULL,
    role TEXT CHECK (role IN ('Customer', 'Employee', 'Manager')) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status TEXT CHECK (status IN ('Suspended', 'Active', 'Inactive')) NOT NULL,
    created_at BIGINT,
    modified_at BIGINT,
    performed_by BIGINT,
    password_version SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_performed_by FOREIGN KEY (performed_by) REFERENCES "user" (id)
);

-- Table: branch
CREATE TABLE IF NOT EXISTS branch (
    id BIGSERIAL PRIMARY KEY,
    ifsc_code VARCHAR(11) UNIQUE NOT NULL,
    contact_number BIGINT UNIQUE,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at BIGINT,
    modified_at BIGINT,
    performed_by BIGINT
);


-- Table: account
CREATE TABLE IF NOT EXISTS account (
    account_id BIGSERIAL PRIMARY KEY,
    account_number BIGINT UNIQUE NOT NULL,
    branch_id BIGINT,
    user_id BIGINT,
    account_type TEXT CHECK (account_type IN ('Savings', 'Current', 'Fixed_Deposit', 'Operational')) NOT NULL,
    status TEXT CHECK (status IN ('Suspended', 'Active', 'Inactive')) NOT NULL,
    balance NUMERIC(15,2) NOT NULL,
    min_balance NUMERIC(15,2) NOT NULL,
    created_at BIGINT,
    modified_at BIGINT,
    performed_by BIGINT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

-- Table: activity_log (renamed from activityLog to follow PG naming conventions)
CREATE TABLE IF NOT EXISTS activityLog (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(20) NOT NULL,
    log_type TEXT CHECK (log_type IN ('Insert', 'Update', 'Login', 'Logout', 'Delete')) NOT NULL,
    row_id BIGINT NOT NULL,
    user_id BIGINT,
    account_number BIGINT,
    log_message VARCHAR(150) NOT NULL,
    timestamp BIGINT NOT NULL,
    performed_by BIGINT,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_number FOREIGN KEY (account_number) REFERENCES account (account_number) ON DELETE SET NULL,
    CONSTRAINT fk_performed_by FOREIGN KEY (performed_by) REFERENCES "user" (id) ON DELETE SET NULL
);

-- Table: customer
CREATE TABLE IF NOT EXISTS customer (
    user_id BIGINT PRIMARY KEY,
    pan_number VARCHAR(25) UNIQUE NOT NULL,
    aadhar_number BIGINT UNIQUE NOT NULL,
    CONSTRAINT fk_customer_user_id FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

-- Table: customer_detail
CREATE TABLE IF NOT EXISTS customerDetail (
    user_id BIGINT PRIMARY KEY,
    dob VARCHAR(10) NOT NULL,
    father_name VARCHAR(100) NOT NULL,
    mother_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    marital_status VARCHAR(10) NOT NULL,
    CONSTRAINT fk_customer FOREIGN KEY (user_id) REFERENCES customer (user_id) ON DELETE CASCADE
);

-- Table: message
CREATE TABLE IF NOT EXISTS message (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    message_content TEXT NOT NULL,
    message_status VARCHAR(20) NOT NULL,
    created_at BIGINT,
    modified_at BIGINT,
    CONSTRAINT fk_sender FOREIGN KEY (sender_id) REFERENCES "user" (id)
);

-- Table: oauth_provider
CREATE TABLE IF NOT EXISTS oauth_provider (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(100) UNIQUE NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_in INT,
    created_at BIGINT,
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);

-- Table: staff
CREATE TABLE IF NOT EXISTS staff (
    user_id BIGINT PRIMARY KEY,
    branch_id BIGINT,
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES branch (id)
);

-- Table: transaction
CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT,
    account_number BIGINT NOT NULL,
    transaction_account_number BIGINT,
    transaction_type TEXT CHECK (transaction_type IN ('Credit', 'Debit', 'Deposit', 'Withdraw')) NOT NULL,
    status TEXT CHECK (status IN ('Completed', 'Pending', 'Cancelled', 'Active', 'Expired')) NOT NULL,
    remarks VARCHAR(255),
    amount NUMERIC(15,2) NOT NULL,
    closing_balance NUMERIC(15,2) NOT NULL,
    transaction_time BIGINT,
    performed_by BIGINT,
    bank_name VARCHAR(255) DEFAULT 'Horizon',
    transaction_ifsc VARCHAR(20) NOT NULL,
    CONSTRAINT fk_transaction_customer FOREIGN KEY (customer_id) REFERENCES "user" (id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_number) REFERENCES account (account_number)
);

-- Table: user_session
CREATE TABLE IF NOT EXISTS user_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    provider_id BIGINT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE
);
