package screep.roles

import screep.memory.role
import screep.memory.working
import screeps.api.*

fun Creep.build(assignedRoom: Room = this.room) {
    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity(RESOURCE_ENERGY)) {
        memory.working = true
        say("🚧 build")
    }

    if (memory.working) {
        val targets = assignedRoom.find(FIND_MY_CONSTRUCTION_SITES)
        if (targets.isNotEmpty()) {
            if (build(targets[0]) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos)
            }
        } else {
            memory.role = Role.UPGRADER
        }
    } else {
        val source = findFreeAndActiveSource(room)
        goHarvest(source)
    }
}