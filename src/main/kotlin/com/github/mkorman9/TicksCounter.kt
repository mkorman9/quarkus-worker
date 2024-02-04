package com.github.mkorman9

import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
class TicksCounter {
    private val counter = AtomicInteger(0)

    fun tick() {
        counter.incrementAndGet()
    }

    fun getTicks(): Int {
        return counter.get()
    }
}
