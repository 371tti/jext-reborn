package me.spartacus04.jext.discs.discplaying

import me.spartacus04.jext.JextState.SCHEDULER
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

internal object JukeboxPlaybackRegistry {
    private data class SessionKey(
        val worldId: UUID,
        val blockX: Int,
        val blockY: Int,
        val blockZ: Int,
        val namespace: String
    )

    private data class Session(
        val location: Location,
        val namespace: String,
        val volume: Float,
        val pitch: Float,
        val rangeSquared: Double,
        val expectedEndNano: Long,
        val players: MutableSet<UUID>
    )

    private val sessions = ConcurrentHashMap<SessionKey, Session>()
    private val playerSessions = ConcurrentHashMap<UUID, MutableSet<SessionKey>>()

    fun startSession(
        location: Location,
        namespace: String,
        volume: Float,
        pitch: Float,
        range: Double,
        durationSeconds: Int,
        initialPlayers: Collection<Player>
    ) {
        val world = location.world ?: return
        val key = SessionKey(world.uid, location.blockX, location.blockY, location.blockZ, namespace)

        stopSession(location, namespace, stopAudio = false)

        val rangeSquared = if (range.isInfinite()) Double.POSITIVE_INFINITY else range * range
        val expectedEnd = if (durationSeconds > 0) {
            System.nanoTime() + durationSeconds.toLong() * 1_000_000_000L
        } else {
            Long.MAX_VALUE
        }

        val players = ConcurrentHashMap.newKeySet<UUID>().apply {
            initialPlayers.forEach { add(it.uniqueId) }
        }

        val session = Session(location.clone(), namespace, volume, pitch, rangeSquared, expectedEnd, players)
        sessions[key] = session

        initialPlayers.forEach { player ->
            playerSessions.compute(player.uniqueId) { _, keys ->
                val set = keys ?: ConcurrentHashMap.newKeySet()
                set.add(key)
                set
            }
        }

        if (durationSeconds > 0) {
            val delayTicks = max(1L, durationSeconds.toLong() * 20L + 40L)
            SCHEDULER.runTaskLater({
                val current = sessions[key]
                if (current != null && hasExpired(current)) {
                    removeSession(key, current, stopAudio = false)
                }
            }, delayTicks)
        }
    }

    fun stopSession(location: Location, namespace: String, stopAudio: Boolean) {
        val world = location.world ?: return
        val key = SessionKey(world.uid, location.blockX, location.blockY, location.blockZ, namespace)
        sessions[key]?.let { removeSession(key, it, stopAudio) }
    }

    fun stopSessions(location: Location, stopAudio: Boolean) {
        val world = location.world ?: return
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ

        sessions.entries.removeIf { (key, session) ->
            if (key.worldId == world.uid && key.blockX == blockX && key.blockY == blockY && key.blockZ == blockZ) {
                removeSession(key, session, stopAudio)
                true
            } else {
                false
            }
        }
    }

    fun handlePlayerMovement(player: Player) {
        pruneExpiredSessions()

        val uuid = player.uniqueId
        val worldId = player.world.uid
        val listenerLocation = player.location

        sessions.forEach { (key, session) ->
            if (key.worldId != worldId) {
                return@forEach
            }

            val inRange = session.rangeSquared.isInfinite() ||
                listenerLocation.distanceSquared(session.location) <= session.rangeSquared

            if (!inRange) {
                return@forEach
            }

            if (session.players.add(uuid)) {
                playerSessions.compute(uuid) { _, keys ->
                    val set = keys ?: ConcurrentHashMap.newKeySet()
                    set.add(key)
                    set
                }

                player.playSound(session.location, session.namespace, SoundCategory.RECORDS, session.volume, session.pitch)
            }
        }
    }

    fun handlePlayerQuit(player: Player) {
        val uuid = player.uniqueId
        val keys = playerSessions.remove(uuid) ?: return

        keys.forEach { key ->
            sessions[key]?.players?.remove(uuid)
        }
    }

    fun handlePlayerJoin(player: Player) {
        handlePlayerMovement(player)
    }

    private fun pruneExpiredSessions() {
        val now = System.nanoTime()
        sessions.entries.removeIf { (key, session) ->
            if (session.expectedEndNano != Long.MAX_VALUE && now >= session.expectedEndNano) {
                removeSession(key, session, stopAudio = false)
                true
            } else {
                false
            }
        }
    }

    private fun removeSession(key: SessionKey, session: Session, stopAudio: Boolean) {
        sessions.remove(key, session)

        session.players.forEach { uuid ->
            playerSessions.computeIfPresent(uuid) { _, keys ->
                keys.remove(key)
                if (keys.isEmpty()) null else keys
            }

            if (stopAudio) {
                Bukkit.getPlayer(uuid)?.stopSound(session.namespace, SoundCategory.RECORDS)
            }
        }
    }

    private fun hasExpired(session: Session): Boolean {
        return session.expectedEndNano != Long.MAX_VALUE && System.nanoTime() >= session.expectedEndNano
    }
}
