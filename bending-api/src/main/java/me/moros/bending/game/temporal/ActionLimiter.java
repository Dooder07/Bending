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

package me.moros.bending.game.temporal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.event.ActionLimitEvent;
import me.moros.bending.event.EventBus;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.Updatable.UpdateResult;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.model.user.BendingBar;
import me.moros.bending.model.user.User;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ActionLimiter extends TemporaryBase {
  public static final TemporalManager<UUID, ActionLimiter> MANAGER = new TemporalManager<>("Action Limiter") {
    public void tick() {
      super.tick();
      BARS.entrySet().removeIf(e -> e.getValue().update() == UpdateResult.REMOVE);
    }
  };

  private static final Map<UUID, BendingBar> BARS = new HashMap<>();

  private final UUID uuid;
  private final LivingEntity entity;
  private final boolean hadAI;
  private final Set<ActionType> limitedActions;

  private boolean reverted = false;

  private ActionLimiter(LivingEntity entity, Collection<ActionType> limitedActions, long duration) {
    super();
    this.uuid = entity.getUniqueId();
    this.entity = entity;
    this.limitedActions = Collections.unmodifiableSet(EnumSet.copyOf(limitedActions));
    hadAI = entity.hasAI();
    if (entity instanceof Player player) {
      if (duration > 100) {
        BossBar bar = BossBar.bossBar(Component.text("Restricted"), 1, Color.YELLOW, Overlay.PROGRESS);
        BARS.putIfAbsent(uuid, BendingBar.of(bar, player, duration));
      }
    } else {
      entity.setAI(false);
    }
    MANAGER.addEntry(uuid, this, Temporary.toTicks(duration));
  }


  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    MANAGER.removeEntry(uuid);
    BendingBar bar = BARS.remove(uuid);
    if (bar != null) {
      bar.onRemove();
    }
    entity.setAI(hadAI);
    return true;
  }

  public static boolean isLimited(Entity entity) {
    return isLimited(entity, null);
  }

  public static boolean isLimited(Entity entity, @Nullable ActionType method) {
    ActionLimiter limiter = MANAGER.get(entity.getUniqueId()).orElse(null);
    if (limiter == null) {
      return false;
    }
    return method == null || limiter.limitedActions.contains(method);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Set<ActionType> limitedActions = EnumSet.allOf(ActionType.class);
    private long duration = 5000;

    private Builder() {
    }

    public Builder limit(Collection<ActionType> methods) {
      limitedActions = EnumSet.copyOf(methods);
      return this;
    }

    public Builder limit(ActionType method, ActionType @Nullable ... methods) {
      Collection<ActionType> c = new ArrayList<>();
      c.add(method);
      if (methods != null) {
        c.addAll(List.of(methods));
      }
      return limit(c);
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<ActionLimiter> build(User source, LivingEntity target) {
      Objects.requireNonNull(source);
      Objects.requireNonNull(target);
      if (isLimited(target)) {
        return Optional.empty();
      }
      ActionLimitEvent event = EventBus.INSTANCE.postActionLimitEvent(source, target, duration);
      if (event.isCancelled() || event.duration() <= 0) {
        return Optional.empty();
      }
      return Optional.of(new ActionLimiter(target, limitedActions, event.duration()));
    }
  }
}