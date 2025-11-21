/*
    PopulationDensity Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.PopulationDensity;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import java.util.HashSet;
import java.util.UUID;

/**
 * Created by RoboMWM on 12/22/2016.
 * All the events and logic to perform this stuff is all encapsulated here,
 * in case users want to disable the entirety of this thing
 */
public class DropShipTeleporter implements Listener
{
    PopulationDensity instance;

    public DropShipTeleporter(PopulationDensity populationDensity)
    {
        this.instance = populationDensity;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityToggleGlide(EntityToggleGlideEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER) return;

        if (isFallDamageImmune((Player)event.getEntity()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event)
    {
        Entity entity = event.getEntity();

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!isFallDamageImmune(entity)) return;

        // Prevent the fall damage
        event.setCancelled(true);
        removeFallDamageImmunity(entity);

        if (entity.getType() != EntityType.PLAYER) return;

        Player player = (Player) entity;

        // Skip effect if they have permission to teleport anywhere
        if (player.hasPermission("populationdensity.teleportanywhere")) return;

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                player.getLocation(),
                1,
                0.0, 0.0, 0.0,
                0.0
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                0.3f,
                1.0f
        );
    }

    HashSet<UUID> fallImmunityList = new HashSet<>();

    void makeEntityFallDamageImmune(LivingEntity entity)
    {
        if (entity.getType() == EntityType.PLAYER)
        {
            Player player = (Player)entity;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
            player.setGliding(false);
        }
        fallImmunityList.add(entity.getUniqueId());
    }

    boolean isFallDamageImmune(Entity entity)
    {
        return fallImmunityList.contains(entity.getUniqueId());
    }

    void removeFallDamageImmunity(Entity entity)
    {
        fallImmunityList.remove(entity.getUniqueId());
    }
}
