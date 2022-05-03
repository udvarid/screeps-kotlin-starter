package screep.roles

import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.Structure

fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
    if (store[RESOURCE_ENERGY] < store.getCapacity()) {
        val source = findFreeAndActiveSource(fromRoom)
        goHarvest(source)
    } else {
        val target = toRoom.find(FIND_MY_STRUCTURES)
            .filter { structuresRequiresEnergy.contains(it.structureType) }
            .map { it.unsafeCast<StoreOwner>() }
            .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }
            .map { it.unsafeCast<Structure>() }
            .map { Pair(getPriority(it.structureType, room), it) }
            .sortedByDescending { it.first }
            .map { it.second }
            .map { it.unsafeCast<StoreOwner>() }
            .firstOrNull()

        if (target != null) {
            if (transfer(target, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(target.pos)
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

private fun getPriority(structureType: StructureConstant, room: Room): Int =
    if (room.memory.underAttack && structureType == STRUCTURE_TOWER) 2 else 1



