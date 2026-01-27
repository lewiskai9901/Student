-- Add building_no and room_no fields to space table
-- building_no: Building number (e.g., 1, A, 甲) - for BUILDING type spaces
-- room_no: Room number (e.g., 101, 302) - for ROOM type spaces

ALTER TABLE space
ADD COLUMN building_no VARCHAR(32) COMMENT '楼号（如 1, A, 甲）' AFTER building_type,
ADD COLUMN room_no VARCHAR(32) COMMENT '房间号（如 101, 302）' AFTER building_no;

-- Create indexes for efficient querying
CREATE INDEX idx_space_building_no ON space(building_no);
CREATE INDEX idx_space_room_no ON space(room_no);

-- Update existing building records to set building_no based on space_name pattern (optional)
-- This can be manually adjusted based on actual data
-- UPDATE space SET building_no = REGEXP_SUBSTR(space_name, '[0-9A-Za-z甲乙丙丁]+') WHERE space_type = 'BUILDING' AND building_no IS NULL;
