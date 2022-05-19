package screep.roles

import screep.building.getDamagedBuildings
import screep.building.storageWithEnergy
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
        val target = assignedRoom.getDamagedBuildings().firstOrNull()
        if (target != null) {
            if (repair(target) == ERR_NOT_IN_RANGE) {
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
            val source = findFreeAndActiveSource(room)
            source?.let { goHarvest(it) }
        }
    }
}