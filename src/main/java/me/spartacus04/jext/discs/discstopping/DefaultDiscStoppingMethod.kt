package me.spartacus04.jext.discs.discstopping

import com.github.retrooper.packetevents.PacketEvents
import me.spartacus04.jext.JextState.CONFIG
import me.spartacus04.jext.JextState.VERSION
import me.spartacus04.jext.discs.discplaying.JukeboxPlaybackRegistry
import me.spartacus04.jext.utils.WrapperPlayServerStopSoundCategory
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

/**
 * The class `DefaultDiscStoppingMethod` is an implementation of the `DiscStoppingMethod` interface that stops discs played from the resource pack.
 */
class DefaultDiscStoppingMethod : DiscStoppingMethod {
    override val requires = listOf<String>()

    private fun stopOldVersions(player: Player) {
        val category = com.github.retrooper.packetevents.protocol.sound.SoundCategory.RECORD
        val packet = WrapperPlayServerStopSoundCategory(category)

        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    override fun stop(player: Player) {
        if(VERSION < "1.19") {
            stopOldVersions(player)
        } else {
            player.stopSound(SoundCategory.RECORDS)
        }
    }

    override fun stop(player: Player, namespace: String) {
        player.stopSound(namespace, SoundCategory.RECORDS)
    }

    override fun stop(location: Location, namespace: String) {
    JukeboxPlaybackRegistry.stopSession(location, namespace, stopAudio = false)

    val world = location.world ?: return
        val range = CONFIG.JUKEBOX_RANGE
        val rangeSquared = if (range <= 0) Double.POSITIVE_INFINITY else (range * range).toDouble()

        for (player in world.players) {
            if (rangeSquared.isInfinite() || player.location.distanceSquared(location) <= rangeSquared) {
                stop(player, namespace)
            }
        }
    }

    override fun stop(location: Location) {
    JukeboxPlaybackRegistry.stopSessions(location, stopAudio = false)

    val world = location.world ?: return
        val configuredRange = CONFIG.JUKEBOX_RANGE
        val rangeSquared = if (configuredRange <= 0) Double.POSITIVE_INFINITY else (configuredRange * configuredRange).toDouble()

        for (player in world.players) {
            if (rangeSquared.isInfinite() || player.location.distanceSquared(location) <= rangeSquared) {
                if(VERSION < "1.19") {
                    stopOldVersions(player)
                } else {
                    player.stopSound(SoundCategory.RECORDS)
                }
            }
        }
    }

}