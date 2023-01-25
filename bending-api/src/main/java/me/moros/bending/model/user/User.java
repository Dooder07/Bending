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

package me.moros.bending.model.user;

import java.util.function.Predicate;

import me.moros.bending.model.GridIterator;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.data.DataContainer;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.protection.ProtectionCache;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.DelegateLivingEntity;
import me.moros.bending.temporal.TempBlock;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a user that can bend.
 */
public sealed interface User extends DelegateLivingEntity, ElementUser, AttributeUser permits BendingUser {
  String NAMESPACE = "bending.user";

  /**
   * Get the game object that this user belongs to.
   * @return the game
   */
  Game game();

  /**
   * Get the data store for this user.
   * @return the data store object
   */
  DataContainer store();

  /**
   * Check if this user is in spectator mode.
   * @return whether this user is a player in spectator mode
   */
  default boolean isSpectator() {
    return false;
  }

  /**
   * Check if the user has the specified ability on cooldown.
   * @param desc the ability to check
   * @return true if the ability is on cooldown for this user, false otherwise
   */
  boolean onCooldown(AbilityDescription desc);

  /**
   * Attempts to put the specified ability on cooldown for the given duration.
   * @param desc the ability to put on cooldown
   * @param duration the duration of the cooldown
   * @return true if cooldown was added successfully, false otherwise
   */
  boolean addCooldown(AbilityDescription desc, long duration);

  /**
   * Makes a preset out of this user's current slots.
   * @param name the name of the preset to be created
   * @return the constructed preset
   */
  Preset createPresetFromSlots(String name);

  /**
   * Bind a preset to slots.
   * @param preset the preset of abilities to bind
   * @return whether binding was successful
   */
  boolean bindPreset(Preset preset);

  /**
   * Assigns an ability to the specified slot.
   * @param slot the slot number in the range [1, 9] (inclusive)
   * @param desc the ability to bind
   */
  void bindAbility(int slot, @Nullable AbilityDescription desc);

  /**
   * Retrieve the ability assigned to the specified slot.
   * @param slot the slot number to check, slot must be in range [1, 9] (inclusive)
   * @return the ability bound to given slot if found, null otherwise
   */
  @Nullable AbilityDescription boundAbility(int slot);

  /**
   * Get the currently selected slot.
   * @return a slot index in the 1-9 range (inclusive)
   */
  int currentSlot();

  /**
   * Changes the currently selected slot.
   * <p>Note: This has no effect on players.
   * @param slot the slot number in the range [1, 9] (inclusive)
   */
  void currentSlot(int slot);

  /**
   * Get the currently selected ability for the user.
   * @return the ability in the currently selected slot for the user if found, null otherwise
   */
  @Nullable AbilityDescription selectedAbility();

  /**
   * Retrieves the ability name for the currently selected slot.
   * @return the ability's name or an empty string if no ability is bound to the currently selected slot
   */
  default String selectedAbilityName() {
    AbilityDescription selected = selectedAbility();
    return selected == null ? "" : selected.name();
  }

  /**
   * Clears the specified slot.
   * @param slot the slot number to clear, slot must be in range [1, 9] (inclusive)
   */
  default void clearSlot(int slot) {
    bindAbility(slot, null);
  }

  /**
   * Check whether this user can bend the specified ability.
   * @param desc the ability to check
   * @return true if the user can bend the given ability, false otherwise
   */
  boolean canBend(AbilityDescription desc);

  /**
   * Check whether this user can bend.
   * @return true if the user can bend, false otherwise
   */
  boolean canBend();

  /**
   * Toggle this user's bending.
   * @return true if the user can bend after the toggle, false otherwise
   */
  boolean toggleBending();

  /**
   * Gets the board for this user.
   * @return the board instance
   */
  Board board();

  /**
   * Check if the user has all required permissions for the specified ability.
   * @param desc the ability to check
   * @return true if the user has all permissions for the ability, false otherwise
   * @see #hasPermission(String)
   */
  default boolean hasPermission(AbilityDescription desc) {
    return desc.permissions().stream().allMatch(this::hasPermission);
  }

  /**
   * Check if the user has the specified permission.
   * If the user is a non-player, this will return true unless a virtual node is set.
   * @param permission the permission to check
   * @return true if the user has the given permission, false otherwise
   * @see #setPermission(String, TriState)
   */
  boolean hasPermission(String permission);

  /**
   * Set a virtual permission node (in memory) for a user.
   * <p>Note: This has no effect if the user is a player.
   * @param permission the permission node
   * @param state the permission state
   * @return the previous state of the permission node
   */
  TriState setPermission(String permission, TriState state);

  /**
   * Checks if the user can build at its current location.
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild() {
    return canBuild(world().blockAt(location()));
  }

  /**
   * Checks if the user can build at a location.
   * @param position the position to check in the user's current world
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild(Vector3d position) {
    return canBuild(world().blockAt(position));
  }

  /**
   * Checks if the user can build at a block location.
   * @param block the block to check
   * @return the result
   * @see ProtectionCache#canBuild(User, Block)
   */
  default boolean canBuild(Block block) {
    return ProtectionCache.INSTANCE.canBuild(this, block);
  }

  /**
   * Attempt to find a possible block source that matches the given predicate.
   * @param range the max range to check
   * @param predicate the predicate to check
   * @return the source block if one was found, null otherwise
   */
  default @Nullable Block find(double range, Predicate<Block> predicate) {
    GridIterator it = GridIterator.create(eyeLocation(), direction(), FastMath.clamp(1, 100, FastMath.ceil(range)));
    while (it.hasNext()) {
      Block block = world().blockAt(it.next());
      if (block.type().isAir()) {
        continue;
      }
      if (predicate.test(block) && TempBlock.isBendable(block) && canBuild(block)) {
        return block;
      }
      if (block.type().isCollidable()) {
        break;
      }
    }
    return null;
  }
}
