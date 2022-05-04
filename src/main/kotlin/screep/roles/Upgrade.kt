package screep.roles

import screep.memory.state
import screep.memory.working
import screeps.api.*

fun Creep.upgrade() {

    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
        memory.state = CreepState.HARVESTING
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity(RESOURCE_ENERGY)) {
        memory.working = true
        say("🚧 upgrading")
        memory.state = CreepState.UPGRADING
    }

    if (memory.working) {
        val controller = this.room.controller
        if (controller != null && upgradeController(controller) == ERR_NOT_IN_RANGE) {
            moveTo(controller.pos, options { reusePath = 10 })
        }
    } else {
        val source = findFreeAndActiveSource(room)
        goHarvest(source)
    }
}