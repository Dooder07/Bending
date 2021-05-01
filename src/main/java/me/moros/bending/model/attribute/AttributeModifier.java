/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

package me.moros.bending.model.attribute;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;

public class AttributeModifier {
  private final String attribute;
  private final ModifierOperation type;
  private final double value;

  public AttributeModifier(@NonNull String attribute, @NonNull ModifierOperation type, double value) {
    this.attribute = attribute;
    this.type = type;
    this.value = value;
  }

  public @NonNull String attribute() {
    return attribute;
  }

  public @NonNull ModifierOperation type() {
    return type;
  }

  public double value() {
    return value;
  }
}
