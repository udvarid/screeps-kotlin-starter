package screep.roles

import screep.memory.role
import screep.memory.state
import screep.memory.working
import screeps.api.*

fun Creep.repair(assignedRoom: Room = this.room) {
    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
        memory.state = CreepState.HARVESTING
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity(RESOURCE_ENERGY)) {
        memory.working = true
        say("🚧 repair")
        memory.state = CreepState.REPAIRING
    }

    if (memory.working) {
        val targets = assignedRoom.find(FIND_MY_STRUCTURES)
            .filter { it.hits < it.hitsMax }
        if (targets.isNotEmpty()) {
            if (repair(targets[0]) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos, options { reusePath = 10 })
            }
        } else {
            memory.role = Role.UPGRADER
        }
    } else {
        val source = findFreeAndActiveSource(room)
        goHarvest(source)
    }
}