\echo '        boards' :boards
INSERT INTO boards (id, name, created_at, updated_at)
SELECT g, 'board-' || g, _seed.epoch(), _seed.epoch()
FROM generate_series(1, :boards) g;
