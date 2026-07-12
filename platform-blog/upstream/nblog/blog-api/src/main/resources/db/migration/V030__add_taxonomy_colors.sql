ALTER TABLE category ADD COLUMN color varchar(7) NOT NULL DEFAULT '#8B1E3F' AFTER category_name;

UPDATE tag SET color = CASE color
    WHEN 'orange' THEN '#9A3412' WHEN 'olive' THEN '#3F6212' WHEN 'green' THEN '#166534'
    WHEN 'teal' THEN '#115E59' WHEN 'blue' THEN '#1E3A8A' WHEN 'violet' THEN '#4C1D95'
    WHEN 'purple' THEN '#701A75' WHEN 'brown' THEN '#713F12' ELSE '#8B1E3F' END;

ALTER TABLE tag MODIFY color varchar(7) NOT NULL DEFAULT '#8B1E3F';
