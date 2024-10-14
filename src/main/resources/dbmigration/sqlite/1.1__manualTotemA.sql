-- Manual migration for SQLite
-- Workaround for unsupported ALTER TABLE in SQLite

-- Step 1: Rename the existing totemguard_alerts table
ALTER TABLE totemguard_alerts RENAME TO old_totemguard_alerts;

-- Step 2: Create the new totemguard_alerts table with the constraint
CREATE TABLE totemguard_alerts (
                                   id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                                   check_name             INTEGER NOT NULL,
                                   totemguard_player_uuid VARCHAR(40) NOT NULL
                                       REFERENCES totemguard_player
                                           ON UPDATE RESTRICT
                                           ON DELETE RESTRICT,
                                   when_created           TIMESTAMP NOT NULL,
                                   CONSTRAINT ck_totemguard_alerts_check_name
                                       CHECK (check_name IN (0, 1, 2, 3, 4, 5, 6, 7, 8))
);

-- Step 3: Copy data from the old table
INSERT INTO totemguard_alerts (id, check_name, totemguard_player_uuid, when_created)
SELECT id, check_name, totemguard_player_uuid, when_created FROM old_totemguard_alerts;

-- Step 4: Drop the old table
DROP TABLE old_totemguard_alerts;

-- Step 1: Rename the existing totemguard_punishments table
ALTER TABLE totemguard_punishments RENAME TO old_totemguard_punishments;

-- Step 2: Create the new totemguard_punishments table with the constraint
CREATE TABLE totemguard_punishments (
                                        id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                                        check_name             INTEGER NOT NULL,
                                        totemguard_player_uuid VARCHAR(40) NOT NULL
                                            REFERENCES totemguard_player
                                                ON UPDATE RESTRICT
                                                ON DELETE RESTRICT,
                                        when_created           TIMESTAMP NOT NULL,
                                        CONSTRAINT ck_totemguard_punishments_check_name
                                            CHECK (check_name IN (0, 1, 2, 3, 4, 5, 6, 7, 8))
);

-- Step 3: Copy data from the old table
INSERT INTO totemguard_punishments (id, check_name, totemguard_player_uuid, when_created)
SELECT id, check_name, totemguard_player_uuid, when_created FROM old_totemguard_punishments;

-- Step 4: Drop the old table
DROP TABLE old_totemguard_punishments;

