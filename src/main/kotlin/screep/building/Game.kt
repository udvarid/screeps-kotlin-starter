package screep.building

import screeps.api.*

fun Game.getMainSpawns() = spawns.values
    .groupBy { it.room.name }
    .mapNotNull { it.value.firstOrNull() }

