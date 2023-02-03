-- Bending H2 Schema

CREATE TABLE IF NOT EXISTS bending_players (
    player_id       SERIAL PRIMARY KEY      NOT NULL,
    player_uuid     UUID                    NOT NULL,
    board           BOOLEAN                 NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS bending_abilities (
    ability_id      SERIAL PRIMARY KEY      NOT NULL,
    ability_name    VARCHAR(32)             NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS ability_name_index ON bending_abilities (ability_name);

CREATE TYPE element_enum AS ENUM ('air', 'water', 'earth', 'fire');
CREATE TABLE IF NOT EXISTS bending_players_elements (
    player_id       INTEGER                 NOT NULL,
    element         element_enum            NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    PRIMARY KEY(player_id, element)
);
CREATE INDEX IF NOT EXISTS elements_index ON bending_players_elements (player_id);

CREATE TABLE IF NOT EXISTS bending_players_slots (
    player_id       INTEGER                 NOT NULL,
    slot            INTEGER                 NOT NULL,
    ability_id      INTEGER                 NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    FOREIGN KEY(ability_id) REFERENCES bending_abilities(ability_id) ON DELETE CASCADE,
    PRIMARY KEY(player_id, slot)
);
CREATE INDEX IF NOT EXISTS players_slots_index ON bending_players_slots (player_id);

CREATE TABLE IF NOT EXISTS bending_presets (
    preset_id       SERIAL PRIMARY KEY      NOT NULL,
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
CREATE INDEX IF NOT EXISTS presets_slots_index ON bending_presets_slots (preset_id);
