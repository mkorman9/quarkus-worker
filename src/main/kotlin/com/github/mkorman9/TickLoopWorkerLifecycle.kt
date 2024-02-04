package com.github.mkorman9

import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.time.Instant
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ApplicationScoped
class TickLoopWorkerLifecycle(
    private val vertx: Vertx,
    tickLoopWorker: TickLoopWorker
) {
    private val verticle = TickLoopWorkerVerticle(tickLoopWorker)

    fun onStartup(@Observes startupEvent: StartupEvent) {
        vertx.deployVerticle(
            verticle,
            DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER)
        )
    }

    fun onShutdown(@Observes shutdownEvent: ShutdownEvent) {
        verticle.destroy()
    }
}

class TickLoopWorkerVerticle(
    private val tickLoopWorker: TickLoopWorker
) : AbstractVerticle() {
    private val isRunning = AtomicBoolean(true)
    private val shutdownQueue = SynchronousQueue<Boolean>()

    override fun start() {
        tickLoopWorker.onInit()

        var lastTick = Instant.now()
        while (isRunning.get()) {
            Thread.sleep(TICK_INTERVAL)
            tickLoopWorker.onTick(lastTick)
            lastTick = Instant.now()
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
