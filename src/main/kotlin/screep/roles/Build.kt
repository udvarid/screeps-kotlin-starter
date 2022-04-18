package screep.roles

import screep.memory.working
import screeps.api.*

fun Creep.build(assignedRoom: Room = this.room) {
    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.working = true
        say("🚧 build")
    }

    if (memory.working) {
        val targets = assignedRoom.find(FIND_MY_CONSTRUCTION_SITES)
        if (targets.isNotEmpty()) {
            if (build(targets[0]) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos)
            }
        }
    } else {
        val sources = room.find(FIND_SOURCES)
            .filter { it.energy > 0 }
            .toTypedArray()
        val source = this.pos.findClosestByPath(sources)
        if (source != null) {
            if (harvest(source) == ERR_NOT_IN_RANGE) {
                moveTo(source.pos)
            }
        }
    }
}