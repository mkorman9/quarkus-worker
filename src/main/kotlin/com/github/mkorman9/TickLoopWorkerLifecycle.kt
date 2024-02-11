package com.github.mkorman9

import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.jboss.logging.Logger
import java.time.Duration
import kotlin.math.abs

private const val TICK_INTERVAL: Long = 1000
private const val SHUTDOWN_TIMEOUT: Long = 2500

@ApplicationScoped
class TickLoopWorkerLifecycle(
    private val log: Logger,
    private val tickLoopWorker: TickLoopWorker
) : Runnable {
    private lateinit var thread: Thread
    private var isRunning = true

    fun onStartup(@Observes startupEvent: StartupEvent) {
        thread = Thread.ofVirtual()
            .name("tick-loop-thread")
            .start(this)
    }

    fun onShutdown(@Observes shutdownEvent: ShutdownEvent) {
        isRunning = false
        thread.join(Duration.ofMillis(SHUTDOWN_TIMEOUT))
    }

    override fun run() {
        while (isRunning) {
            val tickStartTime = System.currentTimeMillis()

            try {
                tickLoopWorker.onTick()
            } catch (e: Exception) {
                log.error("Exception in tick handler", e)
            }

            val elapsedTickTime = System.currentTimeMillis() - tickStartTime
            val timeUntilNextTick = TICK_INTERVAL - elapsedTickTime

            if (timeUntilNextTick >= 0) {
                try {
                    Thread.sleep(timeUntilNextTick)
                } catch (e: InterruptedException) {
                    log.error("Tick thread has been interrupted unexpectedly")
                    return
                }
            } else {
                log.info("Tick is lagging behind ${abs(timeUntilNextTick)} ms")
            }
        }
    }
}
