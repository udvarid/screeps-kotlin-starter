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
    } else if (room.memory.hasDamagedBuilding) {
        room.getDamagedBuildings()
            .filterNot { structureNotToRepair.contains(it.structureType)}
            .maxByOrNull { it.hitsMax - it.hits }
            ?.let { repair(it)  }
    }


}

