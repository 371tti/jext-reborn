package me.spartacus04.jext.discs.discplaying

import me.spartacus04.jext.JextState.CONFIG
import me.spartacus04.jext.discs.discplaying.JukeboxPlaybackRegistry
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import kotlin.math.min

/**
 * The class `DefaultDiscPlayingMethod` is a implementation of the `DiscPlayingMethod` interface that plays discs from the resource pack.
 */
class DefaultDiscPlayingMethod : DiscPlayingMethod {
    override fun playLocation(
        location: Location,
        namespace: String,
        volume : Float,
        pitch : Float,
        durationSeconds: Int
    ) {
        val world = location.world ?: return

        val configuredRange = CONFIG.JUKEBOX_RANGE
        val maxRangeVolume = configuredRange.coerceAtLeast(1) / 16f
        val effectiveVolume = when {
            configuredRange <= 0 -> volume.coerceAtLeast(0f)
            volume <= 0f -> maxRangeVolume
            else -> min(volume, maxRangeVolume)
        }

        if (effectiveVolume <= 0f) {
            return
        }

        val maxReach = (effectiveVolume * 16.0).coerceAtLeast(1.0)
        val effectiveRange = when {
            configuredRange <= 0 -> Double.POSITIVE_INFINITY
            else -> min(configuredRange.toDouble(), maxReach)
        }

        val initialPlayers: Collection<Player> = if (effectiveRange.isInfinite()) {
            world.players
        } else {
            world.getNearbyPlayers(location, effectiveRange).toList()
        }

        initialPlayers.forEach {
            it.playSound(location, namespace, SoundCategory.RECORDS, effectiveVolume, pitch)
        }

        JukeboxPlaybackRegistry.startSession(
            location,
            namespace,
            effectiveVolume,
            pitch,
            effectiveRange,
            durationSeconds,
            initialPlayers
        )
    }

    override fun playPlayer(player: Player, namespace: String, volume : Float, pitch : Float) {
        player.playSound(player.location, namespace, SoundCategory.RECORDS, volume, pitch)
    }
}