package com.github.mkorman9

import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.jboss.logging.Logger
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

private const val TICK_INTERVAL: Long = 1000
private const val SHUTDOWN_TIMEOUT: Long = 2500

@ApplicationScoped
class TickLoopWorkerVerticle(
    private val log: Logger,
    private val vertxInstance: Vertx,
    private val tickLoopWorker: TickLoopWorker
) : AbstractVerticle() {
    private var isRunning = AtomicBoolean(true)
    private val shutdownQueue = SynchronousQueue<Boolean>()

    fun onStartup(@Observes startupEvent: StartupEvent) {
        vertxInstance.deployVerticle(
            this,
            DeploymentOptions()
                .setThreadingModel(ThreadingModel.VIRTUAL_THREAD)
        )
    }

    fun onShutdown(@Observes shutdownEvent: ShutdownEvent) {
        val wasRunning = isRunning.getAndSet(false)
        if (wasRunning) {
            shutdownQueue.poll(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    override fun start() {
        while (isRunning.get()) {
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
                    isRunning.set(false)
                    return
                }
            } else {
                log.info("Tick is lagging behind ${abs(timeUntilNextTick)} ms")
            }
        }

        shutdownQueue.offer(true)
    }
}
