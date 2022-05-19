package screep.roles

import screep.building.getMyConstructionSites
import screep.building.storageWithEnergy
import screep.memory.role
import screep.memory.state
import screep.memory.working
import screeps.api.*

fun Creep.build(assignedRoom: Room = this.room) {
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
        val targets = assignedRoom.getMyConstructionSites()
        if (targets.isNotEmpty()) {
            if (build(targets[0]) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos, options { reusePath = 10 })
            }
        } else {
            memory.role = Role.UPGRADER
        }
    } else {
        val store = room.storageWithEnergy()
        if (store != null) {
            goWithdraw(store)
        } else {
            val source = findFreeAndActiveSource(room)
            source?.let { goHarvest(it) }
        }
    }
}

