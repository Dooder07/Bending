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

package me.moros.bending.util.metadata;

import me.moros.bending.Bending;
import org.bukkit.metadata.FixedMetadataValue;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to provide and construct metadata for the {@link Bending} plugin.
 * @see FixedMetadataValue
 */
public final class Metadata {
  public static final String NO_INTERACT = "bending-no-interact";
  public static final String NO_PICKUP = "bending-no-pickup";
  public static final String GLOVE_KEY = "bending-earth-glove";
  public static final String METAL_CABLE = "bending-metal-cable";
  public static final String DESTRUCTIBLE = "bending-destructible";
  public static final String NO_MOVEMENT = "bending-no-movement";

  private Metadata() {
  }

  public static @NonNull FixedMetadataValue empty() {
    return of(null);
  }

  public static @NonNull FixedMetadataValue of(@Nullable Object obj) {
    return new FixedMetadataValue(Bending.plugin(), obj);
  }
}