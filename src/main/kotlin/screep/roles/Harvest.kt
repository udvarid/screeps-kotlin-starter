package screep.roles

import screep.context.RoomContext
import screep.memory.state
import screeps.api.*
import screeps.api.structures.Structure

fun Creep.harvestMe(roomContext: RoomContext?) {
    if (store[RESOURCE_ENERGY] < store.getCapacity() && memory.state == CreepState.HARVESTING ||
        store[RESOURCE_ENERGY] == 0) {
        if (memory.state != CreepState.HARVESTING) {
            val containersNearbyWithEnergy = pos.findInRange(roomContext!!.containers, 3)
                .map { it.unsafeCast<StoreOwner>() }
                .filter { it.store[RESOURCE_ENERGY] > 0 }
            val linkNearToStore = room.storage?.let {
                it.pos.findInRange(FIND_MY_STRUCTURES, 1)
                    .firstOrNull { link -> link.structureType == STRUCTURE_LINK }
            }
            val linkNearbyWithSpace = pos.findInRange(roomContext.myStructures, 3)
                .filter { it.structureType == STRUCTURE_LINK }
                .map { it.unsafeCast<StoreOwner>() }
                .filter { it.store[RESOURCE_ENERGY] < it.store.getCapacity(RESOURCE_ENERGY) }
                .filter { it.id != linkNearToStore?.id}
            if (containersNearbyWithEnergy.isNotEmpty() && linkNearbyWithSpace.isNotEmpty()) {
                val container = containersNearbyWithEnergy.first()
                goWithdraw(container)
                return
            }
        }
        val source = findFreeAndActiveSource(roomContext!!.mySources)
        memory.state = CreepState.HARVESTING
        source?.let { goHarvest(it) }
    } else {
        val containersNearby = pos.findInRange(roomContext!!.containers, 2)
        val linksNearby = pos.findInRange(FIND_MY_STRUCTURES, 2).filter { it.structureType == STRUCTURE_LINK }
        val target = (roomContext.myStructures + containersNearby + linksNearby)
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


