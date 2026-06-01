CREATE TABLE IF NOT EXISTS points_of_interest (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    zone_code VARCHAR(255),
    opening_hours VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_points_of_interest_type ON points_of_interest(type);
CREATE INDEX IF NOT EXISTS idx_points_of_interest_zone_code ON points_of_interest(zone_code);

INSERT INTO points_of_interest (id, name, type, description, latitude, longitude, zone_code, opening_hours) VALUES
(1, 'Vegan Garden', 'RESTAURANT', 'Plant-based meals and fresh bowls near Main Stage.', 45.7487, 21.2088, 'A1', '10:00-02:00'),
(2, 'Craft Beer Bar', 'BAR', 'Local craft beer and soft drinks.', 45.7491, 21.2090, 'B1', '12:00-03:00'),
(3, 'Festival Merch Shop', 'SHOP', 'Official festival merchandise and artist shirts.', 45.7486, 21.2094, 'C1', '11:00-01:00')
ON CONFLICT (id) DO NOTHING;
