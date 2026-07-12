-- Existing tags receive deterministic deep colors from a broad numeric space.
UPDATE tag
SET color = CONCAT(
  '#',
  CASE MOD(CRC32(CONCAT(id, ':axis')), 3)
    WHEN 0 THEN LPAD(HEX(80 + MOD(CRC32(CONCAT(id, ':r')), 33)), 2, '0')
    ELSE LPAD(HEX(20 + MOD(CRC32(CONCAT(id, ':r')), 51)), 2, '0')
  END,
  CASE MOD(CRC32(CONCAT(id, ':axis')), 3)
    WHEN 1 THEN LPAD(HEX(80 + MOD(CRC32(CONCAT(id, ':g')), 33)), 2, '0')
    ELSE LPAD(HEX(20 + MOD(CRC32(CONCAT(id, ':g')), 51)), 2, '0')
  END,
  CASE MOD(CRC32(CONCAT(id, ':axis')), 3)
    WHEN 2 THEN LPAD(HEX(80 + MOD(CRC32(CONCAT(id, ':b')), 33)), 2, '0')
    ELSE LPAD(HEX(20 + MOD(CRC32(CONCAT(id, ':b')), 51)), 2, '0')
  END
);
