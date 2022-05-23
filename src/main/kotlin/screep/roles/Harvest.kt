package screep.roles

import screep.context.RoomContext
import screep.memory.state
import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.Structure

fun Creep.harvestMe(roomContext: RoomContext?) {
    if (store[RESOURCE_ENERGY] < store.getCapacity() && memory.state == CreepState.HARVESTING ||
        store[RESOURCE_ENERGY] == 0) {
        val source = findFreeAndActiveSource(roomContext!!.mySources)
        memory.state = CreepState.HARVESTING
        source?.let { goHarvest(it) }
    } else {
        val target = roomContext!!.myStructures
            .filter { structuresRequiresEnergy.contains(it.structureType) }
            .map { it.unsafeCast<StoreOwner>() }
            .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }
            .map { it.unsafeCast<Structure>() }
            .map { Pair(getEnergyFillPriority(it.structureType, room), it) }
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
            val spawn = roomContext.spawn
            if (spawn != null) {
                if (!spawn.pos.inRangeTo(this.pos,3)) {
                    moveTo(spawn.pos, options { reusePath = 10 })
                }
            }
        }
    }
}

private fun getEnergyFillPriority(structureType: StructureConstant, room: Room): Int =
    when (structureType) {
        STRUCTURE_TOWER -> if (room.memory.underAttack) 3 else 2
        STRUCTURE_SPAWN, STRUCTURE_EXTENSION -> 2
        else -> 1
    }



