package me.spartacus04.jext.listeners

import me.spartacus04.jext.discs.discplaying.JukeboxPlaybackRegistry
import me.spartacus04.jext.listeners.utils.JextListener
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

internal class JukeboxPlaybackSessionListener : JextListener() {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val to = event.to ?: return
        val from = event.from
        if (from.world === to.world && from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        JukeboxPlaybackRegistry.handlePlayerMovement(event.player)
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        JukeboxPlaybackRegistry.handlePlayerMovement(event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        JukeboxPlaybackRegistry.handlePlayerJoin(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        JukeboxPlaybackRegistry.handlePlayerQuit(event.player)
    }
}
