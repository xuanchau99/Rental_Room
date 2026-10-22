-- V8__multi_tenant_saas.sql: Cấu trúc đa người thuê (Multi-tenant SaaS) cho các chủ trọ

-- 1. Thêm các cột phân cấp tài khoản trong user_accounts
ALTER TABLE user_accounts 
    ADD COLUMN owner_id BIGINT REFERENCES user_accounts(id),
    ADD COLUMN full_name VARCHAR(150),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN note VARCHAR(1000);

CREATE INDEX idx_user_accounts_owner_id ON user_accounts(owner_id);

-- Cập nhật tài khoản admin thành SUPER_ADMIN
UPDATE user_accounts 
SET role = 'SUPER_ADMIN', full_name = 'Quản Trị Viên Hệ Thống' 
WHERE username = 'admin';

-- Cập nhật tài khoản owner_chau thành OWNER (Chủ trọ mẫu)
UPDATE user_accounts 
SET role = 'OWNER', full_name = 'Chủ trọ Xuân Châu', phone = '0988889999' 
WHERE username = 'owner_chau';

-- Gán các tài khoản nhân viên (staff_*) thuộc quyền quản lý của chủ trọ owner_chau
UPDATE user_accounts 
SET owner_id = (SELECT id FROM user_accounts WHERE username = 'owner_chau') 
WHERE username LIKE 'staff_%';

-- 2. Thêm owner_id vào bảng properties (tòa nhà / khu trọ)
ALTER TABLE properties ADD COLUMN owner_id BIGINT REFERENCES user_accounts(id);
CREATE INDEX idx_properties_owner_id ON properties(owner_id);

-- Gán dữ liệu mẫu hiện tại cho chủ trọ owner_chau (nếu không có thì gán cho admin)
UPDATE properties 
SET owner_id = COALESCE(
    (SELECT id FROM user_accounts WHERE username = 'owner_chau'),
    (SELECT id FROM user_accounts WHERE username = 'admin')
);

-- 3. Thêm owner_id vào bảng tenants (khách thuê)
ALTER TABLE tenants ADD COLUMN owner_id BIGINT REFERENCES user_accounts(id);
CREATE INDEX idx_tenants_owner_id ON tenants(owner_id);

UPDATE tenants 
SET owner_id = COALESCE(
    (SELECT id FROM user_accounts WHERE username = 'owner_chau'),
    (SELECT id FROM user_accounts WHERE username = 'admin')
);

-- 4. Thêm owner_id vào bảng services (danh mục dịch vụ của từng chủ trọ)
ALTER TABLE services ADD COLUMN owner_id BIGINT REFERENCES user_accounts(id);
CREATE INDEX idx_services_owner_id ON services(owner_id);

UPDATE services 
SET owner_id = COALESCE(
    (SELECT id FROM user_accounts WHERE username = 'owner_chau'),
    (SELECT id FROM user_accounts WHERE username = 'admin')
);
