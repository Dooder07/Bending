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

package me.moros.bending.event;

import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.User;
import org.bukkit.event.Cancellable;

/**
 * Called when a user is attempting to create a {@link Preset}.
 */
public class PresetCreateEvent extends BendingEvent implements UserEvent, Cancellable {
  private final User user;
  private final Preset preset;

  private boolean cancelled = false;

  PresetCreateEvent(User user, Preset preset) {
    this.user = user;
    this.preset = preset;
  }

  @Override
  public User user() {
    return user;
  }

  /**
   * Provides the preset that is being created.
   * @return the preset
   */
  public Preset preset() {
    return preset;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }
}
