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

package me.moros.bending.paper;

import java.io.InputStream;
import java.nio.file.Path;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.AbstractBending;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.hook.MiniPlaceholdersHook;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.ReflectionUtil;
import me.moros.bending.paper.hook.LuckPermsHook;
import me.moros.bending.paper.hook.PlaceholderAPIHook;
import me.moros.bending.paper.listener.BlockListener;
import me.moros.bending.paper.listener.ConnectionListener;
import me.moros.bending.paper.listener.UserListener;
import me.moros.bending.paper.listener.WorldListener;
import me.moros.bending.paper.platform.BukkitPermissionInitializer;
import me.moros.bending.paper.platform.BukkitPlatform;
import me.moros.bending.paper.protection.ProtectionInitializer;
import me.moros.tasker.bukkit.BukkitExecutor;
import me.moros.tasker.executor.CompositeExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.checkerframework.checker.nullness.qual.Nullable;

final class PaperBending extends AbstractBending<BendingBootstrap> {
  PaperBending(BendingBootstrap parent, Path dir, Logger logger) {
    super(parent, dir, logger);
  }

  void onPluginEnable() {
    new Metrics(parent, 8717);
    ReflectionUtil.injectStatic(Tasker.class, CompositeExecutor.of(new BukkitExecutor(parent)));
    ReflectionUtil.injectStatic(Platform.Holder.class, new BukkitPlatform(logger()));
    new ProtectionInitializer(this).init();
    load();
    new BukkitPermissionInitializer().init();

    var pluginManager = parent.getServer().getPluginManager();
    pluginManager.registerEvents(new BlockListener(game), parent);
    pluginManager.registerEvents(new UserListener(game), parent);
    pluginManager.registerEvents(new ConnectionListener(logger(), game), parent);
    pluginManager.registerEvents(new WorldListener(game), parent);

    try {
      PaperCommandManager<CommandSender> manager = PaperCommandManager.createNative(parent, CommandExecutionCoordinator.simpleCoordinator());
      manager.registerAsynchronousCompletions();
      Commander.create(manager, Player.class, this).init();
    } catch (Exception e) {
      logger().error(e.getMessage(), e);
      pluginManager.disablePlugin(parent);
      return;
    }

    parent.getServer().getServicesManager().register(Game.class, game, parent, ServicePriority.Normal);
    registerHooks(parent.getServer());
  }

  void onPluginDisable() {
    disable();
  }

  private void registerHooks(Server server) {
    if (server.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new PlaceholderAPIHook(this).register();
    }
    if (server.getPluginManager().isPluginEnabled("MiniPlaceholders")) {
      new MiniPlaceholdersHook().init();
    }
    if (server.getPluginManager().isPluginEnabled("LuckPerms")) {
      LuckPermsHook.register(server.getServicesManager());
    }
  }

  @Override
  public String author() {
    return parent.getPluginMeta().getAuthors().get(0);
  }

  @Override
  public String version() {
    return parent.getPluginMeta().getVersion();
  }

  @Override
  public @Nullable InputStream resource(String fileName) {
    return parent.getResource(fileName);
  }
}
