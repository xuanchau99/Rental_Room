-- Upgrade existing data without changing V1 or local V2.
ALTER TABLE rooms ADD COLUMN name VARCHAR(150), ADD COLUMN deposit_amount NUMERIC(14,2) CHECK (deposit_amount >= 0), ADD COLUMN description VARCHAR(1000);
ALTER TABLE tenants ADD COLUMN address VARCHAR(255), ADD COLUMN gender VARCHAR(30), ADD COLUMN vehicle_plate VARCHAR(50), ADD COLUMN note VARCHAR(1000);
ALTER TABLE contracts DROP CONSTRAINT contracts_status_check;
UPDATE contracts SET status = CASE status WHEN 'DRAFT' THEN 'PENDING' WHEN 'ENDED' THEN 'EXPIRED' WHEN 'CANCELLED' THEN 'TERMINATED' ELSE status END;
ALTER TABLE contracts ADD CONSTRAINT contracts_status_check CHECK (status IN ('PENDING','ACTIVE','EXPIRED','TERMINATED'));
ALTER TABLE contracts ADD COLUMN payment_due_day INTEGER NOT NULL DEFAULT 5 CHECK (payment_due_day BETWEEN 1 AND 31),
    ADD COLUMN note VARCHAR(1000), ADD COLUMN electricity_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (electricity_price >= 0),
    ADD COLUMN water_calculation_type VARCHAR(30) NOT NULL DEFAULT 'METER' CHECK (water_calculation_type IN ('METER','PER_PERSON')),
    ADD COLUMN water_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (water_price >= 0);
CREATE UNIQUE INDEX uq_contract_active_room ON contracts(room_id) WHERE status = 'ACTIVE';
ALTER TABLE electricity_meters ADD COLUMN billing_month INTEGER, ADD COLUMN billing_year INTEGER,
    ADD COLUMN previous_reading NUMERIC(14,3), ADD COLUMN current_reading NUMERIC(14,3),
    ADD COLUMN usage NUMERIC(14,3), ADD COLUMN unit_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    ADD COLUMN amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (amount >= 0);
UPDATE electricity_meters SET billing_month = EXTRACT(MONTH FROM reading_date), billing_year = EXTRACT(YEAR FROM reading_date),
    previous_reading = reading, current_reading = reading, usage = 0;
ALTER TABLE electricity_meters ALTER COLUMN billing_month SET NOT NULL, ALTER COLUMN billing_year SET NOT NULL,
    ALTER COLUMN previous_reading SET NOT NULL, ALTER COLUMN current_reading SET NOT NULL, ALTER COLUMN usage SET NOT NULL,
    ADD CHECK (billing_month BETWEEN 1 AND 12), ADD CHECK (billing_year BETWEEN 2000 AND 9999),
    ADD CHECK (previous_reading >= 0 AND current_reading >= previous_reading AND usage = current_reading - previous_reading),
    ADD CONSTRAINT uq_electricity_meters_period UNIQUE(room_id,billing_year,billing_month);
ALTER TABLE water_meters ADD COLUMN billing_month INTEGER, ADD COLUMN billing_year INTEGER,
    ADD COLUMN previous_reading NUMERIC(14,3), ADD COLUMN current_reading NUMERIC(14,3),
    ADD COLUMN usage NUMERIC(14,3), ADD COLUMN unit_price NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    ADD COLUMN amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (amount >= 0);
UPDATE water_meters SET billing_month = EXTRACT(MONTH FROM reading_date), billing_year = EXTRACT(YEAR FROM reading_date),
    previous_reading = reading, current_reading = reading, usage = 0;
ALTER TABLE water_meters ALTER COLUMN billing_month SET NOT NULL, ALTER COLUMN billing_year SET NOT NULL,
    ALTER COLUMN previous_reading SET NOT NULL, ALTER COLUMN current_reading SET NOT NULL, ALTER COLUMN usage SET NOT NULL,
    ADD CHECK (billing_month BETWEEN 1 AND 12), ADD CHECK (billing_year BETWEEN 2000 AND 9999),
    ADD CHECK (previous_reading >= 0 AND current_reading >= previous_reading AND usage = current_reading - previous_reading),
    ADD CONSTRAINT uq_water_meters_period UNIQUE(room_id,billing_year,billing_month);
ALTER TABLE electricity_meters ADD COLUMN image_url VARCHAR(1000);
ALTER TABLE water_meters ADD COLUMN calculation_type VARCHAR(30) NOT NULL DEFAULT 'METER' CHECK (calculation_type IN ('METER','PER_PERSON')),
    ADD COLUMN person_count INTEGER NOT NULL DEFAULT 0 CHECK (person_count >= 0);
ALTER TABLE invoices DROP CONSTRAINT invoices_status_check;
UPDATE invoices SET status = CASE status WHEN 'DRAFT' THEN 'UNPAID' WHEN 'ISSUED' THEN 'UNPAID' ELSE status END;
ALTER TABLE invoices ADD CONSTRAINT invoices_status_check CHECK (status IN ('UNPAID','PARTIALLY_PAID','PAID','OVERDUE','CANCELLED'));
ALTER TABLE invoices ADD COLUMN room_id BIGINT REFERENCES rooms(id), ADD COLUMN billing_month INTEGER,
    ADD COLUMN billing_year INTEGER, ADD COLUMN room_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (room_amount >= 0),
    ADD COLUMN electric_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (electric_amount >= 0),
    ADD COLUMN water_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (water_amount >= 0),
    ADD COLUMN service_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (service_amount >= 0),
    ADD COLUMN other_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (other_amount >= 0),
    ADD COLUMN discount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    ADD COLUMN paid_at TIMESTAMPTZ;
UPDATE invoices i SET room_id = c.room_id, billing_month = EXTRACT(MONTH FROM i.period_start),
    billing_year = EXTRACT(YEAR FROM i.period_start), room_amount = i.total_amount FROM contracts c WHERE c.id = i.contract_id;
ALTER TABLE invoices ALTER COLUMN room_id SET NOT NULL, ALTER COLUMN billing_month SET NOT NULL,
    ALTER COLUMN billing_year SET NOT NULL, ADD CHECK (billing_month BETWEEN 1 AND 12),
    ADD CHECK (billing_year BETWEEN 2000 AND 9999),
    ADD CONSTRAINT uq_invoice_room_period UNIQUE(room_id,billing_year,billing_month);
CREATE INDEX idx_invoices_period ON invoices(billing_year,billing_month);
ALTER TABLE payments ADD COLUMN note VARCHAR(1000);
ALTER TABLE payments ADD CHECK (amount > 0);
CREATE UNIQUE INDEX uq_payment_reference ON payments(invoice_id,reference) WHERE reference IS NOT NULL;

CREATE TABLE room_tenants (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    contract_id BIGINT NOT NULL REFERENCES contracts(id),
    role VARCHAR(30) NOT NULL,
    move_in_date DATE NOT NULL,
    move_out_date DATE,
    CHECK (move_out_date IS NULL OR move_out_date >= move_in_date)
);
CREATE INDEX idx_room_tenants_room_id ON room_tenants(room_id);
CREATE INDEX idx_room_tenants_tenant_id ON room_tenants(tenant_id);
CREATE INDEX idx_room_tenants_contract_id ON room_tenants(contract_id);
CREATE TABLE services (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(150) NOT NULL,
    calculation_type VARCHAR(30) NOT NULL,
    price NUMERIC(14,2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL
);
CREATE TABLE contract_services (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    contract_id BIGINT NOT NULL REFERENCES contracts(id),
    service_id BIGINT NOT NULL REFERENCES services(id),
    price NUMERIC(14,2) NOT NULL CHECK (price >= 0),
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity >= 0),
    UNIQUE(contract_id,service_id)
);
CREATE INDEX idx_contract_services_contract_id ON contract_services(contract_id);
CREATE INDEX idx_contract_services_service_id ON contract_services(service_id);
CREATE TABLE deposit_transactions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    contract_id BIGINT NOT NULL REFERENCES contracts(id),
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount >= 0),
    transaction_date DATE NOT NULL,
    note VARCHAR(1000),
    CHECK (amount > 0)
);
CREATE INDEX idx_deposit_transactions_contract_id ON deposit_transactions(contract_id);
CREATE TABLE user_accounts (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL
);
CREATE TABLE refresh_tokens (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE UNIQUE INDEX uq_tenant_current_stay ON room_tenants(tenant_id) WHERE move_out_date IS NULL;
CREATE UNIQUE INDEX uq_primary_stay ON room_tenants(contract_id) WHERE role = 'PRIMARY_TENANT' AND move_out_date IS NULL;
INSERT INTO room_tenants(room_id,tenant_id,contract_id,role,move_in_date)
SELECT room_id,tenant_id,id,'PRIMARY_TENANT',start_date FROM contracts WHERE status='ACTIVE';
