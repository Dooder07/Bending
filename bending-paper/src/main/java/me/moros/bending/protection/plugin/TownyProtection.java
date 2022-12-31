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

package me.moros.bending.protection.plugin;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import me.moros.bending.model.protection.AbstractProtection;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.BukkitPlayer;
import me.moros.bending.platform.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

public final class TownyProtection extends AbstractProtection {
  private final TownyAPI api;

  public TownyProtection(Plugin plugin) {
    super(plugin.getName());
    api = TownyAPI.getInstance();
  }

  @Override
  public boolean canBuild(LivingEntity entity, Block block) {
    var loc = new Location(PlatformAdapter.toBukkitWorld(block.world()), block.blockX(), block.blockY(), block.blockZ());
    if (entity instanceof BukkitPlayer player) {
      return PlayerCacheUtil.getCachePermission(player.handle(), loc, Material.DIRT, TownyPermission.ActionType.BUILD);
    }
    TownBlock townBlock = api.getTownBlock(loc);
    return townBlock == null || !townBlock.hasTown();
  }
}
