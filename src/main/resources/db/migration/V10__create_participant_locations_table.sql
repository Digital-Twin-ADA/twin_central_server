CREATE TABLE IF NOT EXISTS participant_locations (
    id BIGSERIAL PRIMARY KEY,
    participant_id VARCHAR(255) NOT NULL,
    stage_id BIGINT REFERENCES stages(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    zone_code VARCHAR(255),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_participant_locations_recorded_at ON participant_locations(recorded_at);
CREATE INDEX IF NOT EXISTS idx_participant_locations_stage_id ON participant_locations(stage_id);
CREATE INDEX IF NOT EXISTS idx_participant_locations_zone_code ON participant_locations(zone_code);
