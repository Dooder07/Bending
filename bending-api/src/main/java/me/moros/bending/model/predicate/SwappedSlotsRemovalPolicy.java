/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.model.predicate;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;

/**
 * Policy to remove ability when the user has changed slots.
 */
public final class SwappedSlotsRemovalPolicy implements RemovalPolicy {
  private final AbilityDescription expected;

  private SwappedSlotsRemovalPolicy(AbilityDescription expected) {
    this.expected = expected;
  }

  @Override
  public boolean test(User user, AbilityDescription desc) {
    return user instanceof BendingPlayer && !expected.equals(user.selectedAbility());
  }

  /**
   * Creates a {@link RemovalPolicy} that expects a specific ability to be selected.
   * @param expected the expected ability
   * @return the constructed policy
   */
  public static RemovalPolicy of(AbilityDescription expected) {
    return new SwappedSlotsRemovalPolicy(expected);
  }
}
