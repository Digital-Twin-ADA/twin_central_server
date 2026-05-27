CREATE TABLE IF NOT EXISTS spontaneous_events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    stage_id BIGINT REFERENCES stages(id),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_spontaneous_events_starts_at ON spontaneous_events(starts_at);
CREATE INDEX IF NOT EXISTS idx_spontaneous_events_stage_id ON spontaneous_events(stage_id);

-- Seeds for spontaneous_events
INSERT INTO spontaneous_events (id, title, description, stage_id, starts_at, ends_at, created_at, status) VALUES
  (1, 'Pop-up DJ', 'Surprise DJ set at Main Stage', 1, '2026-07-10T16:30:00+03:00', '2026-07-10T17:15:00+03:00', now(), 'SCHEDULED'),
  (2, 'Acoustic Corner', 'Unplugged acoustic set near Chill Zone', 2, '2026-07-10T15:45:00+03:00', '2026-07-10T16:15:00+03:00', now(), 'SCHEDULED'),
  (3, 'Meet & Greet', 'Artist meet and greet (no fixed stage)', NULL, '2026-07-11T12:00:00+03:00', '2026-07-11T13:00:00+03:00', now(), 'SCHEDULED')
ON CONFLICT (id) DO NOTHING;

-- Ensure sequence is set past the seeded ids
SELECT setval(pg_get_serial_sequence('spontaneous_events','id'),
              COALESCE((SELECT MAX(id) FROM spontaneous_events), 1));
