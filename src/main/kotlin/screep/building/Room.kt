package screep.building

import screeps.api.*

fun Room.getMyStructures() = find(FIND_MY_STRUCTURES)

fun Room.getContainers() = find(FIND_STRUCTURES)
    .filter { it.structureType == STRUCTURE_CONTAINER }
    .toTypedArray()

fun Room.getMyConstructionSites() = find(FIND_MY_CONSTRUCTION_SITES)

fun Room.getDamagedBuildings() = (getMyStructures() + getContainers())
    .filter { it.hits < it.hitsMax }

fun Room.storageWithEnergy(): StoreOwner? {
    return storage?.let {
        if (it.unsafeCast<StoreOwner>().store[RESOURCE_ENERGY] > 0) it else null
    }
}

val structureTypesToDefend = listOf(STRUCTURE_SPAWN, STRUCTURE_TOWER,
    STRUCTURE_STORAGE, STRUCTURE_TERMINAL, STRUCTURE_LINK)
