-- Development-only data. Rent remains unset until configured.
INSERT INTO properties (name) VALUES ('Dãy trọ 01');
INSERT INTO rooms (property_id, code, area_m2, has_mezzanine, has_air_conditioner, status)
SELECT p.id, 'P' || LPAD(n::text, 2, '0'), 20.00, TRUE, TRUE, 'VACANT'
FROM properties p CROSS JOIN generate_series(1, 9) AS n
WHERE p.name = 'Dãy trọ 01';
