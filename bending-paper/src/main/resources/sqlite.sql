-- Bending SQLite Schema

CREATE TABLE IF NOT EXISTS bending_players (
    player_id       INTEGER PRIMARY KEY     NOT NULL,
    player_uuid     BLOB                    NOT NULL,
    board           BOOL                    NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS bending_abilities (
    ability_id      INTEGER PRIMARY KEY     NOT NULL,
    ability_name    VARCHAR(32)             NOT NULL UNIQUE
);
CREATE INDEX ability_name_index ON bending_abilities (ability_name);

CREATE TABLE IF NOT EXISTS bending_players_elements (
    player_id       INTEGER                 NOT NULL,
    element         VARCHAR(16) CHECK( element IN ('air','water','earth','fire') ) NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    PRIMARY KEY(player_id, element)
);
CREATE INDEX elements_index ON bending_players_elements (player_id);

CREATE TABLE IF NOT EXISTS bending_players_slots (
    player_id       INTEGER                 NOT NULL,
    slot            INTEGER                 NOT NULL,
    ability_id      INTEGER                 NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    FOREIGN KEY(ability_id) REFERENCES bending_abilities(ability_id) ON DELETE CASCADE,
    PRIMARY KEY(player_id, slot)
);
CREATE INDEX players_slots_index ON bending_players_slots (player_id);

CREATE TABLE IF NOT EXISTS bending_presets (
    preset_id       INTEGER PRIMARY KEY     NOT NULL,
    player_id       INTEGER                 NOT NULL,
    preset_name     VARCHAR(16)             NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bending_presets_slots (
    preset_id       INTEGER                 NOT NULL,
    slot            INTEGER                 NOT NULL,
    ability_id      INTEGER                 NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES bending_presets(preset_id) ON DELETE CASCADE,
    FOREIGN KEY(ability_id) REFERENCES bending_abilities(ability_id) ON DELETE CASCADE,
    PRIMARY KEY(preset_id, slot)
);
CREATE INDEX presets_slots_index ON bending_presets_slots (preset_id);
