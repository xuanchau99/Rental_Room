-- A reusable catalog entry. Each contract stores its own agreed monthly price.
INSERT INTO services (name, calculation_type, price, active)
SELECT 'Wi-Fi', 'FIXED', 0, true
WHERE NOT EXISTS (
    SELECT 1 FROM services
    WHERE lower(name) = 'wi-fi' AND calculation_type = 'FIXED' AND active = true
);
