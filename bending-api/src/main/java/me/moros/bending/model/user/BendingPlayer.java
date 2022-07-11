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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import me.moros.bending.event.EventBus;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BendingPlayer extends BendingUser implements PresetUser {
  private final Set<Preset> presets;
  private final int internalId;

  private Board board = BoardImpl.DUMMY;

  private BendingPlayer(Game game, Player player, PlayerProfile profile) {
    super(game, player, profile.benderData());
    this.internalId = profile.id();
    presets = new HashSet<>(profile.benderData().presets());
    if (profile.board()) {
      board();
    }
  }

  public int profileId() {
    return internalId;
  }

  @Override
  public Player entity() {
    return (Player) super.entity();
  }

  @Override
  public int currentSlot() {
    return entity().getInventory().getHeldItemSlot() + 1;
  }

  @Override
  public void currentSlot(int slot) {
  }

  @Override
  public @Nullable AbilityDescription selectedAbility() {
    return boundAbility(currentSlot());
  }

  @Override
  public boolean hasPermission(String permission) {
    return entity().hasPermission(permission);
  }

  @Override
  public boolean valid() {
    return entity().isOnline();
  }

  @Override
  public boolean spectator() {
    return entity().getGameMode() == GameMode.SPECTATOR;
  }

  @Override
  public boolean sneaking() {
    return entity().isSneaking();
  }

  @Override
  public boolean sprinting() {
    return entity().isSprinting();
  }

  @Override
  public void sprinting(boolean sprinting) {
    if (sprinting() != sprinting) {
      entity().setSprinting(sprinting);
    }
  }

  @Override
  public boolean allowFlight() {
    return entity().getAllowFlight();
  }

  @Override
  public boolean flying() {
    return entity().isFlying();
  }

  @Override
  public void allowFlight(boolean allow) {
    entity().setAllowFlight(allow);
  }

  @Override
  public void flying(boolean flying) {
    entity().setFlying(flying);
  }

  @Override
  public PlayerInventory inventory() {
    return entity().getInventory();
  }

  @Override
  public Board board() {
    if (world().hasMetadata(Metadata.DISABLED) || entity().hasMetadata(Metadata.HIDDEN_BOARD)) {
      board.disableScoreboard();
      board = BoardImpl.DUMMY;
    } else if (!board.isEnabled()) {
      board = new BoardImpl(this);
    }
    return board;
  }

  // Presets
  @Override
  public Set<Preset> presets() {
    return Set.copyOf(presets);
  }

  @Override
  public @Nullable Preset presetByName(String name) {
    return presets.stream().filter(p -> p.name().equalsIgnoreCase(name)).findAny().orElse(null);
  }

  @Override
  public PresetCreateResult addPreset(Preset preset) {
    if (preset.id() > 0 || presets.contains(preset)) {
      return PresetCreateResult.EXISTS;
    }
    if (!EventBus.INSTANCE.postPresetCreateEvent(this, preset)) {
      return PresetCreateResult.CANCELLED;
    }
    presets.add(preset);
    return PresetCreateResult.SUCCESS;
  }

  @Override
  public boolean removePreset(Preset preset) {
    if (preset.id() <= 0 || !presets.contains(preset)) {
      return false;
    }
    return presets.remove(preset);
  }

  public PlayerProfile toProfile() {
    BenderData data = new BenderData(createPresetFromSlots("").abilities(), elements(), presets);
    boolean board = !entity().hasMetadata(Metadata.HIDDEN_BOARD);
    return new PlayerProfile(internalId, board, data);
  }

  public static Optional<User> createUser(Game game, Player player, PlayerProfile profile) {
    Objects.requireNonNull(game);
    if (Registries.BENDERS.containsKey(player.getUniqueId())) {
      return Optional.empty();
    }
    return Optional.of(new BendingPlayer(game, player, profile));
  }
}