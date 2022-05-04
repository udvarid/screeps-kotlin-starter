package screep.building

import screeps.api.*

fun Room.getMainSpawn() = find(FIND_MY_STRUCTURES)
    .firstOrNull { (it.structureType == STRUCTURE_SPAWN) }