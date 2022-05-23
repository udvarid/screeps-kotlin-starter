package screep.building

import screep.context.RoomContext
import screep.memory.hasDamagedBuilding
import screep.memory.underAttack
import screep.roles.structureNotToRepair
import screeps.api.*
import screeps.api.structures.StructureTower

fun StructureTower.doYourJobTower(roomContext: RoomContext) {
    if (room.memory.underAttack) {
        val enemy = pos.findClosestByRange(FIND_HOSTILE_CREEPS)
        if (enemy != null) {
            attack(enemy)
        } else {
            roomContext.damagedCreeps
                .maxByOrNull { it.hitsMax - it.hits }
                ?.let { heal(it) }
        }
    } else if (ableToRepair() && room.memory.hasDamagedBuilding) {
        roomContext.damagedBuildings
            .filterNot { structureNotToRepair.contains(it.structureType)}
            .maxByOrNull { it.hitsMax - it.hits }
            ?.let { repair(it)  }
    } else if (ableToRepair()) {
        val rampartToBuild = roomContext.myRamparts
            .filter { it.hits < rampartLimit() }
            .minByOrNull { it.hits }
        rampartToBuild?.let { repair(it) }
    }
}

fun StructureTower.ableToRepair() : Boolean {
    val tower = this.unsafeCast<StoreOwner>()
    return tower.store[RESOURCE_ENERGY] > tower.store.getCapacity(RESOURCE_ENERGY)?.div(2)
}

fun StructureTower.rampartLimit() : Int {
    val controller = room.controller
    return controller?.let {
        when (it.level) {
            1, 2, 3, 4 -> 500_000
            5 -> 750_000
            6 -> 1_000_000
            7 -> 1_500_000
            8 -> 3_000_000
            else -> 0
        }
    }  ?: 0
}
