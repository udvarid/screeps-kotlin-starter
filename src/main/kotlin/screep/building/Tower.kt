package screep.building

import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.StructureTower

fun StructureTower.doYourJobTower() {
    if (room.memory.underAttack) {
        val enemy = pos.findClosestByRange(FIND_HOSTILE_CREEPS)

        if (enemy != null) {
            attack(enemy)
        } else {
            val wounded = room.find(FIND_MY_CREEPS)
                .filter { it.hits < it.hitsMax }
                .maxByOrNull { it.hitsMax - it.hits }
            if (wounded != null) {
                heal(wounded)
            }
        }
    } else {
        val damagedBuilding = room.find(FIND_MY_STRUCTURES)
            .filterNot { it.structureType == STRUCTURE_ROAD}
            .filterNot { it.structureType == STRUCTURE_WALL}
            .filterNot { it.structureType == STRUCTURE_RAMPART}
            .filter { it.hits < it.hitsMax }
            .maxByOrNull { it.hitsMax - it.hits }
        if (damagedBuilding != null) {
            repair(damagedBuilding)
        }
    }
    
}

