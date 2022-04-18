package screep.roles

import screeps.api.*

fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
    if (store[RESOURCE_ENERGY] < store.getCapacity()) {
        val sources = fromRoom.find(FIND_SOURCES)
            .filter { it.energy > 0 }
            .toTypedArray()
        val source = this.pos.findClosestByPath(sources)
        if (source != null) {
            if (harvest(source) == ERR_NOT_IN_RANGE) {
                moveTo(source.pos)
            }
        }
    } else {
        val targets = toRoom.find(FIND_MY_STRUCTURES)
            .filter { (it.structureType == STRUCTURE_EXTENSION || it.structureType == STRUCTURE_SPAWN) }
            .map { it.unsafeCast<StoreOwner>() }
            .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity() }

        if (targets.isNotEmpty()) {
            if (transfer(targets[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(targets[0].pos)
            }
        }
    }
}