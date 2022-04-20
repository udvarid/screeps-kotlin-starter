package screep.roles

import screep.memory.working
import screeps.api.*
import screeps.api.structures.StructureController

fun Creep.upgrade(controller: StructureController) {

    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity(RESOURCE_ENERGY)) {
        memory.working = true
        say("🚧 upgrading")
    }

    if (memory.working) {
        if (upgradeController(controller) == ERR_NOT_IN_RANGE) {
            moveTo(controller.pos)
        }
    } else {
        val source = findFreeAndActiveSource(room)
        goHarvest(source)
    }
}