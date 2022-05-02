package screep.brain.repetative

import screep.memory.inspectCounter
import screep.memory.underAttack
import screeps.api.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete

class RepetitiveTasks {

    companion object Tasks {
        fun doTasks() {
            memoryClearing(Game.creeps)
            detectingEnemies()
        }
    }

}

private fun detectingEnemies() {

    if (global.Memory.inspectCounter > 0) {
        global.Memory.inspectCounter--
        return
    }
    global.Memory.inspectCounter = 25

    val mainSpawns = Game.spawns.values
        .groupBy { it.room.name }
        .mapNotNull { it.value.firstOrNull() }

    for (spawn in mainSpawns) {
        val enemyScreeps = spawn.room.find(FIND_HOSTILE_CREEPS).count()
        spawn.room.memory.underAttack = enemyScreeps > 0
        if (spawn.room.memory.underAttack) {
            console.log("Room ", spawn.room.name, " is under attack!")
        }
    }
}


private fun memoryClearing(creeps: Record<String, Creep>) {
    if (creeps.isEmpty()) return

    for (creepName in Memory.creeps.keys) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}
