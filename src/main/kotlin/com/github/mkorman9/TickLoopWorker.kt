package com.github.mkorman9

import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class TickLoopWorker(
    private val log: Logger,
    private val ticksCounter: TicksCounter
) {
    fun onInit() {
        log.info("Worker started")
    }

    fun onTick() {
        log.info("Tick")
        ticksCounter.tick()
    }

    fun onDestroy() {
        log.info("Worker stopped")
    }
}
