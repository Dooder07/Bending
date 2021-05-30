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

package me.moros.bending.model.user;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.atlas.caffeine.cache.Scheduler;
import me.moros.bending.Bending;
import me.moros.bending.events.BindChangeEvent.BindType;
import me.moros.bending.events.ElementChangeEvent.ElementAction;
import me.moros.bending.game.BenderRegistry;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.CooldownExpiry;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.util.Tasker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BendingUser implements User {
  private final LivingEntity entity;
  private final Set<Element> elements;
  private final AbilityDescription[] slots;
  private final Cache<AbilityDescription, Long> cooldowns;
  private final CompositeBendingConditional bendingConditional;


  protected BendingUser(@NonNull LivingEntity entity, @NonNull BenderData data) {
    this.entity = entity;
    cooldowns = Caffeine.newBuilder().expireAfter(new CooldownExpiry())
      .removalListener((key, value, cause) -> {
        if (key != null) {
          Tasker.sync(() -> Bending.eventBus().postCooldownRemoveEvent(this, key), 0);
        }
      })
      .scheduler(Scheduler.systemScheduler())
      .build();
    slots = new Preset(data.slots()).toBinds();
    elements = EnumSet.copyOf(data.elements().stream().map(Element::fromName).flatMap(Optional::stream).collect(Collectors.toList()));
    bendingConditional = BendingConditions.builder().build();
    validateSlots();
  }

  @Override
  public @NonNull LivingEntity entity() {
    return entity;
  }

  @Override
  public @NonNull Set<@NonNull Element> elements() {
    return Set.copyOf(elements);
  }

  @Override
  public boolean hasElement(@NonNull Element element) {
    return elements.contains(element);
  }

  @Override
  public boolean addElement(@NonNull Element element) {
    if (!hasElement(element) && Bending.eventBus().postElementChangeEvent(this, ElementAction.ADD)) {
      elements.add(element);
      return true;
    }
    return false;
  }

  @Override
  public boolean removeElement(@NonNull Element element) {
    if (hasElement(element) && Bending.eventBus().postElementChangeEvent(this, ElementAction.REMOVE)) {
      elements.remove(element);
      validateSlots();
      return true;
    }
    return false;
  }

  @Override
  public boolean chooseElement(@NonNull Element element) {
    if (Bending.eventBus().postElementChangeEvent(this, ElementAction.CHOOSE)) {
      elements.clear();
      elements.add(element);
      validateSlots();
      Bending.game().abilityManager(world()).createPassives(this);
      return true;
    }
    return false;
  }

  @Override
  public @NonNull Preset createPresetFromSlots(@NonNull String name) {
    String[] copy = new String[9];
    for (int slot = 0; slot < 9; slot++) {
      AbilityDescription desc = slots[slot];
      copy[slot] = desc == null ? null : desc.name();
    }
    return new Preset(0, name, copy);
  }

  @Override
  public int bindPreset(@NonNull Preset preset) {
    if (Bending.eventBus().postBindChangeEvent(this, BindType.MULTIPLE)) {
      System.arraycopy(preset.toBinds(), 0, slots, 0, 9);
      validateSlots();
      if (this instanceof BendingPlayer) {
        Bending.game().boardManager().updateBoard((Player) entity());
      }
      return preset.compare(createPresetFromSlots(""));
    }
    return 0;
  }

  @Override
  public Optional<AbilityDescription> boundAbility(int slot) {
    if (slot < 1 || slot > 9) {
      return Optional.empty();
    }
    return Optional.ofNullable(slots[slot - 1]);
  }

  @Override
  public void bindAbility(int slot, @Nullable AbilityDescription desc) {
    if (slot < 1 || slot > 9) {
      return;
    }
    if (Bending.eventBus().postBindChangeEvent(this, BindType.SINGLE)) {
      slots[slot - 1] = desc;
      if (this instanceof BendingPlayer) {
        Bending.game().boardManager().updateBoardSlot((Player) entity(), desc);
      }
    }
  }

  @Override
  public Optional<AbilityDescription> selectedAbility() {
    return Optional.empty(); // Non-player bending users don't have anything selected.
  }

  @Override
  public boolean onCooldown(@NonNull AbilityDescription desc) {
    return cooldowns.getIfPresent(desc) != null;
  }

  @Override
  public boolean addCooldown(@NonNull AbilityDescription desc, long duration) {
    if (duration > 0 && Bending.eventBus().postCooldownAddEvent(this, desc, duration)) {
      cooldowns.put(desc, duration);
      return true;
    }
    return false;
  }

  @Override
  public @NonNull CompositeBendingConditional bendingConditional() {
    return bendingConditional;
  }

  @Override
  public void validateSlots() {
    IntStream.rangeClosed(1, 9).forEach(i -> boundAbility(i).ifPresent(desc -> {
      if (!hasElement(desc.element()) || !hasPermission(desc) || !desc.canBind()) {
        slots[i] = null;
      }
    }));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BendingUser) {
      return entity().equals(((BendingUser) obj).entity());
    }
    return entity().equals(obj);
  }

  @Override
  public int hashCode() {
    return entity.hashCode();
  }

  /**
   * Use {@link BenderRegistry#createUser(LivingEntity, BenderData)}
   */
  public static Optional<BendingUser> createUser(@NonNull LivingEntity entity, @NonNull BenderData data) {
    if (entity instanceof Player) {
      return Optional.empty();
    }
    if (Bending.game().benderRegistry().isRegistered(entity.getUniqueId())) {
      return Bending.game().benderRegistry().user(entity);
    }
    return Optional.of(new BendingUser(entity, data));
  }
}
