package screep.roles

import screeps.api.*

fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
    if (store[RESOURCE_ENERGY] < store.getCapacity()) {
        val source = findFreeAndActiveSource(fromRoom)
        goHarvest(source)
    } else {
        val targets = toRoom.find(FIND_MY_STRUCTURES)
            .filter { (it.structureType == STRUCTURE_EXTENSION ||
                    it.structureType == STRUCTURE_SPAWN ||
                    it.structureType == STRUCTURE_TOWER) }
            .map { it.unsafeCast<StoreOwner>() }
            .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }

        if (targets.isNotEmpty()) {
            if (transfer(targets[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos)
            }
        } else {
            val spawn = toRoom.find(FIND_MY_STRUCTURES)
                .firstOrNull { (it.structureType == STRUCTURE_SPAWN) }
            if (spawn != null) {
                if (!spawn.pos.inRangeTo(this.pos,3)) {
                    moveTo(spawn.pos)
                }
            }
        }
    }
}

