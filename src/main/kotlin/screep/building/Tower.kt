package screep.building

import screep.memory.hasDamagedBuilding
import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.StructureTower

fun StructureTower.doYourJobTower() {
    val structureNotToRepair = listOf(STRUCTURE_ROAD, STRUCTURE_WALL, STRUCTURE_RAMPART)
    if (room.memory.underAttack) {
        val enemy = pos.findClosestByRange(FIND_HOSTILE_CREEPS)
        if (enemy != null) {
            attack(enemy)
        } else {
            room.getDamagedCreeps()
                .maxByOrNull { it.hitsMax - it.hits }
                ?.let { heal(it) }
        }
    } else if (ableToRepair() && room.memory.hasDamagedBuilding) {
        room.getDamagedBuildings()
            .filterNot { structureNotToRepair.contains(it.structureType)}
            .maxByOrNull { it.hitsMax - it.hits }
            ?.let { repair(it)  }
    } else if (ableToRepair()) {
        val rampartToBuild = room.getMyRamparts().firstOrNull { it.hits < rampartLimit() }
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
            1, 2, 3, 4 -> 100_000
            5 -> 200_000
            6 -> 300_000
            7 -> 500_000
            8 -> 1_000_000
            else -> 0
        }
    }  ?: 0
}
