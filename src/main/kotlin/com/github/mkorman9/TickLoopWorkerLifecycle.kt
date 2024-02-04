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

@ApplicationScoped
class TickLoopWorkerLifecycle(
    private val vertx: Vertx,
    private val tickLoopWorkerVerticle: TickLoopWorkerVerticle
) {
    fun onStartup(@Observes startupEvent: StartupEvent) {
        vertx.deployVerticle(
            tickLoopWorkerVerticle,
            DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER)
        )
    }

    fun onShutdown(@Observes shutdownEvent: ShutdownEvent) {
        tickLoopWorkerVerticle.destroy()
    }
}

@ApplicationScoped
class TickLoopWorkerVerticle(
    private val log: Logger,
    private val tickLoopWorker: TickLoopWorker
) : AbstractVerticle() {
    private val isRunning = AtomicBoolean(true)
    private val shutdownQueue = SynchronousQueue<Boolean>()

    override fun start() {
        tickLoopWorker.onInit()

        while (isRunning.get()) {
            val beforeTickTime = System.currentTimeMillis()
            tickLoopWorker.onTick()
            val tickTime = System.currentTimeMillis() - beforeTickTime
            val lagTime = TICK_INTERVAL - tickTime

            if (lagTime >= 0) {
                Thread.sleep(lagTime)
            } else {
                log.info("Tick is lagging behind ${abs(lagTime)} ms")
            }
        }

        tickLoopWorker.onDestroy()
        shutdownQueue.offer(true)
    }

    fun destroy() {
        isRunning.set(false)
        shutdownQueue.poll(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    companion object {
        private const val TICK_INTERVAL: Long = 1000
        private const val SHUTDOWN_TIMEOUT: Long = 2000
    }
}
