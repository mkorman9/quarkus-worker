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
import kotlin.math.abs

private const val TICK_INTERVAL: Long = 1000
private const val SHUTDOWN_TIMEOUT: Long = 2500

@ApplicationScoped
class TickLoopWorkerVerticle(
    private val log: Logger,
    private val vertxInstance: Vertx,
    private val tickLoopWorker: TickLoopWorker
) : AbstractVerticle() {
    private var isRunning = true
    private val shutdownQueue = SynchronousQueue<Boolean>()

    fun onStartup(@Observes startupEvent: StartupEvent) {
        vertxInstance.deployVerticle(
            this,
            DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER)
        )
    }

    fun onShutdown(@Observes shutdownEvent: ShutdownEvent) {
        isRunning = false
        shutdownQueue.poll(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    override fun start() {
        while (isRunning) {
            val beforeTickTime = System.currentTimeMillis()

            try {
                tickLoopWorker.onTick()
            } catch (e: Exception) {
                log.error("Exception in tick handler", e)
            }

            val tickTime = System.currentTimeMillis() - beforeTickTime
            val lagTime = TICK_INTERVAL - tickTime

            if (lagTime >= 0) {
                Thread.sleep(lagTime)
            } else {
                log.info("Tick is lagging behind ${abs(lagTime)} ms")
            }
        }

        shutdownQueue.offer(true)
    }
}
