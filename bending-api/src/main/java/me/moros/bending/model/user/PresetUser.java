/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.user;

import java.util.Set;

import me.moros.bending.event.PresetCreateEvent;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PresetUser {
  /**
   * @return an immutable collection of this user's preset names
   */
  Set<Preset> presets();

  /**
   * Check if the user has the specified preset.
   * @param name the preset name to check
   * @return true the user has the specified preset, false otherwise
   */
  @Nullable Preset presetByName(String name);

  /**
   * Attempt to add the specified preset to the user. Calls a {@link PresetCreateEvent}.
   * @param preset the preset to add
   * @return the result
   */
  PresetCreateResult addPreset(Preset preset);

  /**
   * Attempt to remove the specified preset from the user.
   * @param preset the preset to remove
   * @return true if the preset was removed successfully, false otherwise
   */
  boolean removePreset(Preset preset);
}