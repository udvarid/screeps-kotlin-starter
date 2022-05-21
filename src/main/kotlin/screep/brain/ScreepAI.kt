package screep.brain

import screep.brain.repetative.RepetitiveOperationTasks
import screep.brain.repetative.RepetitiveStrategyTasks
import screep.building.getMainSpawns
import screep.context.RoomContext
import screeps.api.*

fun gameLoop() {
    val roomContexts = Game.getMainSpawns()
        .map { RoomContext(it.room, it) }

    RepetitiveStrategyTasks.doTasks(roomContexts)

    RepetitiveOperationTasks.doTasks(roomContexts)
}

