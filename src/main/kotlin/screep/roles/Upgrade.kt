package screep.roles

import screep.memory.working
import screeps.api.*
import screeps.api.structures.StructureController

fun Creep.upgrade(controller: StructureController) {

    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity()) {
        memory.working = true
        say("🚧 upgrading")
    }

    if (memory.working) {
        if (upgradeController(controller) == ERR_NOT_IN_RANGE) {
            moveTo(controller.pos)
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