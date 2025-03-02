package screep.roles

import screep.building.storageWithEnergy
import screep.context.RoomContext
import screep.memory.role
import screep.memory.state
import screep.memory.working
import screeps.api.*

fun Creep.buildMe(roomContext: RoomContext?) {
    if (memory.working && store[RESOURCE_ENERGY] == 0) {
        memory.working = false
        say("🔄 harvest")
        memory.state = CreepState.HARVESTING
    }
    if (!memory.working && store[RESOURCE_ENERGY] == store.getCapacity(RESOURCE_ENERGY)) {
        memory.working = true
        say("🚧 build")
        memory.state = CreepState.BUILDING
    }

    if (memory.working) {
        val target = roomContext!!.myConstructionSites.firstOrNull()
        if (target != null) {
            if (build(target) == ERR_NOT_IN_RANGE) {
                moveTo(target.pos, options { reusePath = 10 })
            }
        } else {
            memory.role = Role.UPGRADER
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

