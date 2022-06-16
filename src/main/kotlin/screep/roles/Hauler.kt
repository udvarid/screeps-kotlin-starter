package screep.roles

import screep.building.storageWithEnergy
import screep.constant.creepSuicideLimit
import screep.context.RoomContext
import screep.memory.state
import screeps.api.*
import screeps.api.structures.Structure

fun Creep.haulMe(roomContext: RoomContext?) {
    val targets = roomContext!!.myStructures
        .filter { structuresRequiresEnergy.contains(it.structureType) }
        .filterNot { it.structureType == STRUCTURE_STORAGE }
        .map { it.unsafeCast<StoreOwner>() }
        .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }
        .map { it.unsafeCast<Structure>() }
        .map { Pair(getEnergyFillPriority(it.structureType, room), it) }
        .sortedByDescending { it.first }
        .map { it.second }
        .map { it.unsafeCast<StoreOwner>() }

    if (targets.isNotEmpty()) {
        if (store[RESOURCE_ENERGY] == 0) {
            val storage = room.storage
            if (storage != null) {
                val link = storage.pos.findInRange(FIND_MY_STRUCTURES, 1)
                        .filter { it.structureType == STRUCTURE_LINK }
                        .map { it.unsafeCast<StoreOwner>() }
                        .firstOrNull { it.store[RESOURCE_ENERGY] > 0 }
                val energySource = link ?: room.storageWithEnergy()
                energySource?.let {
                    memory.state = CreepState.HARVESTING
                    goWithdraw(it) }
            }
        } else {
            val target = targets.first()
            memory.state = CreepState.TRANSFERRING_ENERGY
            if (transfer(target, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(target.pos, options { reusePath = 10 })
            }
        }
    } else {
        if (store[RESOURCE_ENERGY] > 0 && ticksToLive < creepSuicideLimit) {
            memory.state = CreepState.TRANSFERRING_ENERGY
            val storage = roomContext.room.storage!!
            if (transfer(storage, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                moveTo(storage.pos, options { reusePath = 10 })
            }
        } else {
            val storage = room.storage
            if (storage != null) {
                val link = storage.pos.findInRange(FIND_MY_STRUCTURES, 1)
                    .filter { it.structureType == STRUCTURE_LINK }
                    .map { it.unsafeCast<StoreOwner>() }
                    .firstOrNull { it.store[RESOURCE_ENERGY] > 0 }
                if (link != null) {
                    memory.state = CreepState.HARVESTING
                    goWithdraw(link)
                } else {
                    memory.state = CreepState.IDLE
                }
            }
        }
    }
}

