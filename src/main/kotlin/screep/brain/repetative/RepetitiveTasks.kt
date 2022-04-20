package screep.brain.repetative

import screeps.api.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete

class RepetitiveTasks {

    companion object Tasks {
        fun doTasks() {
            memoryClearing(Game.creeps)
        }
    }

}

private fun memoryClearing(creeps: Record<String, Creep>) {
    if (Game.creeps.isEmpty()) return  // this is needed because Memory.creeps is undefined

    for ((creepName, _) in Memory.creeps) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}
