package screep.roles

import screep.building.storageWithEnergy
import screep.context.RoomContext
import screep.memory.state
import screep.memory.working
import screeps.api.*

fun Creep.upgradeMe(roomContext: RoomContext?) {

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
        this.room.controller?.let {
            if (upgradeController(it) == ERR_NOT_IN_RANGE) {
                moveTo(it.pos, options { reusePath = 10 })
            }
        }
    } else {
        val store = room.storageWithEnergy()
        if (store != null) {
            goWithdraw(store)
        } else {
            val source = findFreeAndActiveSource(roomContext!!.mySources)
            source?.let { goHarvest(it) }
        }
    }
}