package com.github.mkorman9

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/ticks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(value = [])
class TicksResource(
    private val ticksCounter: TicksCounter
) {
    @GET
    fun getTicks() = TicksResponse(
        ticks = ticksCounter.getTicks()
    )
}

data class TicksResponse(val ticks: Int)
