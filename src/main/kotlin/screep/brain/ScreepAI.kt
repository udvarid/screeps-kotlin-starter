package screep.brain

import screep.brain.repetative.RepetitiveOperationTasks
import screep.brain.repetative.RepetitiveStrategyTasks
import screep.building.getMainSpawns
import screeps.api.*

fun gameLoop() {
    val mainSpawns = Game.getMainSpawns()

    RepetitiveStrategyTasks.doTasks(mainSpawns)

    RepetitiveOperationTasks.doTasks(mainSpawns)
}

