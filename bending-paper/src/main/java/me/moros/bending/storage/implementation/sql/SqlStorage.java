/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.storage.implementation.sql;

import me.moros.bending.Bending;
import me.moros.bending.model.user.player.BenderData;
import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.storage.StorageType;
import me.moros.bending.storage.implementation.StorageImplementation;
import me.moros.bending.storage.sql.SqlQueries;
import me.moros.bending.storage.sql.SqlStreamReader;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SqlStorage implements StorageImplementation {
	private final StorageType type;

	public SqlStorage(StorageType type) {
		this.type = type;
	}

	@Override
	public StorageType getType() {
		return type;
	}

	@Override
	public void init() {
		Collection<String> statements;
		try (InputStream stream = Bending.getPlugin().getResource(type.getSchemaPath())) {
			statements = SqlStreamReader.parseQueries(stream);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		StorageFactory.getJdbi().useHandle(handle -> {
			Batch batch = handle.createBatch();
			for (String query : statements) {
				batch.add(query);
			}
			batch.execute();
		});
	}

	@Override
	public BendingProfile createProfile(UUID uuid) {
		return loadProfile(uuid).orElseGet(() ->
			StorageFactory.getJdbi().withHandle(handle -> {
				int id = (int) handle.createQuery(SqlQueries.PLAYER_INSERT.getQuery()).bind(0, uuid).mapToMap().one().get("player_id");
				return new BendingProfile(uuid, id, new BenderData());
			})
		);
	}

	@Override
	public Optional<BendingProfile> loadProfile(UUID uuid) {
		try {
			return StorageFactory.getJdbi().withHandle(handle -> {
				Map<String, Object> result = handle.createQuery(SqlQueries.PLAYER_SELECT_BY_UUID.getQuery()).bind(0, uuid).mapToMap().one();
				if (result == null) return Optional.empty();
				int id = (int) result.getOrDefault("player_id", 0);
				boolean board = (boolean) result.getOrDefault("board", true);
				BenderData data = new BenderData(getSlots(id), getElements(id), getPresets(id));
				return Optional.of(new BendingProfile(uuid, id, data, board));
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	@Override
	public boolean updateProfile(BendingProfile profile) {
		try {
			StorageFactory.getJdbi().useHandle(handle ->
				handle.createUpdate(SqlQueries.PLAYER_UPDATE_BOARD_FOR_ID.getQuery())
					.bind(0, profile.hasBoard()).bind(1, profile.getInternalId()).execute()
			);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean createElements(Set<Element> elements) {
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				PreparedBatch batch = handle.prepareBatch(SqlQueries.ELEMENTS_INSERT_NEW.getQuery());
				for (Element element : elements) {
					batch.bind(0, element.name()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean createAbilities(Set<AbilityDescription> abilities) {
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				PreparedBatch batch = handle.prepareBatch(SqlQueries.ABILITIES_INSERT_NEW.getQuery());
				for (AbilityDescription desc : abilities) {
					batch.bind(0, desc.getName()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveElements(BendingPlayer player) {
		int id = player.getProfile().getInternalId();
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				handle.createUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE_FOR_ID.getQuery()).bind(0, id).execute();
				PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_ELEMENTS_INSERT_FOR_NAME.getQuery());
				for (Element element : player.getElements()) {
					batch.bind(0, id).bind(1, element.name()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveSlot(BendingPlayer player, int slotIndex) {
		int id = player.getProfile().getInternalId();
		Optional<AbilityDescription> desc = player.getStandardSlotAbility(slotIndex);
		if (!desc.isPresent()) return false;
		int abilityId = getAbilityId(desc.get().getName());
		if (abilityId == 0) return false;
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				handle.createUpdate(SqlQueries.PLAYER_SLOTS_REMOVE_SPECIFIC.getQuery())
					.bind(0, id).bind(1, slotIndex).execute();
				handle.createUpdate(SqlQueries.PLAYER_SLOTS_INSERT_NEW.getQuery()).bind(0, id)
					.bind(1, slotIndex).bind(2, abilityId).execute();
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// Returns null if doesn't exist or when a problem occurs.
	@Override
	public Preset loadPreset(int playerId, String name) {
		int presetId = getPresetId(playerId, name);
		if (presetId == 0) return null;
		String[] abilities = new String[9];
		try {
			return StorageFactory.getJdbi().withHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PRESET_SLOTS_SELECT_BY_ID.getQuery()).bind(0, presetId);
				for (Map<String, Object> map : query.mapToMap()) {
					int slot = (int) map.get("slot");
					String abilityName = (String) map.get("ability_name");
					abilities[slot - 1] = abilityName;
				}
				return new Preset(presetId, name, abilities);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean savePreset(int playerId, Preset preset) {
		if (preset.getInternalId() > 0) return false; // Must be a new preset!
		if (!deletePreset(playerId, preset.getName())) return false; // needed for overwriting
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				int presetId = (int) handle.createQuery(SqlQueries.PRESET_INSERT_NEW.getQuery())
					.bind(0, playerId).bind(1, preset.getName())
					.mapToMap().one().get("preset_id");
				String[] abilities = preset.getAbilities();
				PreparedBatch batch = handle.prepareBatch(SqlQueries.PRESET_SLOTS_INSERT_NEW.getQuery());
				batch.execute();
				for (int i = 0; i < 9; i++) {
					String abilityName = abilities[i];
					if (abilityName == null) continue;
					int abilityId = getAbilityId(abilityName);
					if (abilityId == 0) continue;
					batch.bind(0, presetId).bind(1, i + 1).bind(2, abilityId).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deletePreset(int presetId) {
		if (presetId <= 0) return false; // It won't exist
		try {
			StorageFactory.getJdbi().useHandle(handle ->
				handle.createUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.getQuery()).bind(0, presetId).execute()
			);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// Helper methods
	private int getAbilityId(String name) {
		try {
			return StorageFactory.getJdbi().withHandle(handle ->
				(int) handle.createQuery(SqlQueries.ABILITIES_SELECT_ID_BY_NAME.getQuery()).bind(0, name).mapToMap().one().get("ability_id")
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	private String[] getSlots(int playerId) {
		String[] slots = new String[9];
		try {
			StorageFactory.getJdbi().useHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PLAYER_SLOTS_SELECT_FOR_ID.getQuery()).bind(0, playerId);
				for (Map<String, Object> map : query.mapToMap()) {
					int slot = (int) map.get("slot");
					String abilityName = (String) map.get("ability_name");
					slots[slot - 1] = abilityName;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return slots;
	}

	private Set<String> getElements(int playerId) {
		try {
			return StorageFactory.getJdbi().withHandle(handle ->
				handle.createQuery(SqlQueries.PLAYER_ELEMENTS_SELECT_FOR_ID.getQuery()).bind(0, playerId)
					.mapToMap().stream().map(r -> (String) r.get("element_name")).collect(Collectors.toSet())
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	private Set<String> getPresets(int playerId) {
		try {
			return StorageFactory.getJdbi().withHandle(handle ->
				handle.createQuery(SqlQueries.PRESET_NAMES_SELECT_BY_PLAYER_ID.getQuery()).bind(0, playerId)
					.mapToMap().stream().map(r -> (String) r.get("preset_name")).collect(Collectors.toSet())
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	private boolean deletePreset(int playerId, String presetName) {
		try {
			StorageFactory.getJdbi().withHandle(handle ->
				handle.createUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.getQuery()).bind(0, playerId).bind(1, presetName)
			);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// Gets preset id
	// Returns 0 if doesn't exist or when a problem occurs.
	private int getPresetId(int playerId, String presetName) {
		try {
			return StorageFactory.getJdbi().withHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PRESET_SELECT_ID_BY_ID_AND_NAME.getQuery())
					.bind(0, playerId).bind(1, presetName);
				return (int) query.mapToMap().one().get("preset_id");
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
