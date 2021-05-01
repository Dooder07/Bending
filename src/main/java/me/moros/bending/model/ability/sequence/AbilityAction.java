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

package me.moros.bending.model.ability.sequence;

import java.util.Objects;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;

/**
 * Immutable and thread-safe pair representation of {@link AbilityDescription} and {@link ActivationMethod}
 */
public final class AbilityAction {
  private final AbilityDescription desc;
  private final ActivationMethod action;
  private final int hashcode;

  public AbilityAction(@NonNull AbilityDescription desc, @NonNull ActivationMethod action) {
    this.desc = desc;
    this.action = action;
    hashcode = Objects.hash(desc, action);
  }

  public @NonNull AbilityDescription abilityDescription() {
    return desc;
  }

  public @NonNull ActivationMethod action() {
    return action;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AbilityAction) {
      AbilityAction otherAction = ((AbilityAction) other);
      return action == otherAction.action && desc.equals(otherAction.desc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    return desc.name() + ": " + action.name();
  }
}
