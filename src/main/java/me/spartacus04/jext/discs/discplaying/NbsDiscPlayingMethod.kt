package me.spartacus04.jext.discs.discplaying

import com.xxmicloxx.NoteBlockAPI.model.Song
import com.xxmicloxx.NoteBlockAPI.songplayer.EntitySongPlayer
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer
import me.spartacus04.jext.JextState.CONFIG
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.math.roundToInt

/**
 * The class `NbsDiscPlayingMethod` is a implementation of the `DiscPlayingMethod` interface that plays discs from the nbs directory.
 */
class NbsDiscPlayingMethod(private val song: Song) : DiscPlayingMethod {

    override fun playLocation(location: Location, namespace: String, volume: Float, pitch: Float, durationSeconds: Int) {
        val songPlayer = PositionSongPlayer(song)
        songPlayer.targetLocation = location
        songPlayer.volume = volume.roundToInt().toByte()

        val range = CONFIG.JUKEBOX_RANGE
        songPlayer.distance = range

        val rangeSquared = if (range <= 0) Double.POSITIVE_INFINITY else (range * range).toDouble()

        location.world!!.players.forEach {
            if (location.distanceSquared(it.location) <= rangeSquared) {
                songPlayer.addPlayer(it)
            }
        }

        songPlayer.isPlaying = true
        songPlayer.autoDestroy = true
    }

    override fun playPlayer(player: Player, namespace: String, volume: Float, pitch: Float) {
        val songPlayer = EntitySongPlayer(song)
        songPlayer.entity = player
        songPlayer.volume = volume.roundToInt().toByte()

        songPlayer.distance = CONFIG.JUKEBOX_RANGE

        songPlayer.addPlayer(player)

        songPlayer.isPlaying = true
    }
}