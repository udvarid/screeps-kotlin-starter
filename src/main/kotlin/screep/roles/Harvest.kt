package screep.roles

import screep.building.getMainSpawn
import screep.building.getMyStructures
import screep.memory.state
import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.Structure

fun Creep.harvest(fromRoom: Room = this.room, toRoom: Room = this.room) {
    if (store[RESOURCE_ENERGY] < store.getCapacity()) {
        val source = findFreeAndActiveSource(fromRoom)
        memory.state = CreepState.HARVESTING
        goHarvest(source)
    } else {
        val target = toRoom.getMyStructures()
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
            memory.state = CreepState.TRANSFERRING_ENERGY
            if (transfer(target, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(target.pos, options { reusePath = 10 })
            }
        } else {
            memory.state = CreepState.IDLE
            val spawn = toRoom.getMainSpawn()
            if (spawn != null) {
                if (!spawn.pos.inRangeTo(this.pos,3)) {
                    moveTo(spawn.pos, options { reusePath = 10 })
                }
            }
        }
    }
}

private fun getPriority(structureType: StructureConstant, room: Room): Int =
    if (room.memory.underAttack && structureType == STRUCTURE_TOWER) 2 else 1



