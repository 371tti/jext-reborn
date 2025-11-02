package me.spartacus04.jext.utils

import me.spartacus04.jext.JextState.PLUGIN
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Proxy

internal object FoliaRegionScheduler {
    private var warned = false

    fun run(location: Location, delayTicks: Long, task: Runnable): Boolean {
        val server = Bukkit.getServer()
        val scheduler = try {
            val accessor = server.javaClass.methods.firstOrNull { it.name == "getRegionScheduler" && it.parameterCount == 0 }
                ?: return false
            accessor.invoke(server) ?: return false
        } catch (_: NoSuchMethodException) {
            return false
        } catch (_: NoSuchMethodError) {
            return false
        } catch (_: UnsupportedOperationException) {
            return false
        } catch (t: Throwable) {
            PLUGIN.logger.warning("Unable to access Folia RegionScheduler: ${t.message}")
            return false
        }

        val safeLocation = location.clone()
        val methods = scheduler.javaClass.methods.filter { it.name == "runDelayed" }

        for (method in methods) {
            val params = method.parameterTypes
            if (params.size < 4) continue

            val pluginIndex = params.indexOfFirst { !it.isPrimitive && it.isAssignableFrom(PLUGIN.javaClass) }
            if (pluginIndex == -1) continue

            val locationIndex = params.indexOfFirst { !it.isPrimitive && it.isAssignableFrom(Location::class.java) }
            if (locationIndex == -1) continue

            val delayIndex = params.indexOfFirst {
                it == java.lang.Long.TYPE || it == java.lang.Long::class.java ||
                    it == java.lang.Integer.TYPE || it == java.lang.Integer::class.java
            }
            if (delayIndex == -1) continue

            val taskIndex = params.indexOfFirst {
                Runnable::class.java.isAssignableFrom(it) ||
                    (it.isInterface && it.methods.any { m ->
                        m.parameterCount <= 1 && (m.name == "run" || m.name == "accept" || m.name == "invoke")
                    })
            }
            if (taskIndex == -1) continue

            val args = arrayOfNulls<Any>(params.size)
            args[pluginIndex] = PLUGIN
            args[locationIndex] = safeLocation
            args[delayIndex] = when (params[delayIndex]) {
                java.lang.Integer.TYPE, java.lang.Integer::class.java -> delayTicks.toInt()
                else -> delayTicks
            }

            args[taskIndex] = when {
                Runnable::class.java.isAssignableFrom(params[taskIndex]) -> Runnable { task.run() }
                else -> Proxy.newProxyInstance(
                    params[taskIndex].classLoader,
                    arrayOf(params[taskIndex])
                ) { proxy, invokedMethod, methodArgs ->
                    when {
                        invokedMethod.parameterCount <= 1 && (invokedMethod.name == "run" || invokedMethod.name == "accept" || invokedMethod.name == "invoke") -> {
                            try {
                                task.run()
                            } catch (t: Throwable) {
                                PLUGIN.logger.warning("Region task execution failed: ${t.message}")
                            }
                            null
                        }

                        invokedMethod.name == "hashCode" -> task.hashCode()
                        invokedMethod.name == "equals" -> (methodArgs?.firstOrNull() === proxy)
                        invokedMethod.name == "toString" -> "JextRegionSchedulerProxy"
                        else -> null
                    }
                }
            }

            return try {
                method.invoke(scheduler, *args)
                true
            } catch (_: UnsupportedOperationException) {
                false
            } catch (t: Throwable) {
                PLUGIN.logger.warning("Failed to schedule region task via ${method.name}: ${t.message}")
                false
            }
        }

        if (!warned) {
            warned = true
            PLUGIN.logger.warning("No compatible Folia RegionScheduler#runDelayed signature found; falling back to Bukkit scheduler")
        }

        return false
    }
}
