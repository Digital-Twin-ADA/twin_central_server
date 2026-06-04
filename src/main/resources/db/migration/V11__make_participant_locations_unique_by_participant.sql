DELETE FROM participant_locations
WHERE id NOT IN (
    SELECT MAX(id)
    FROM participant_locations
    GROUP BY participant_id
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_participant_locations_participant_id
ON participant_locations(participant_id);
