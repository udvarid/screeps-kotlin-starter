package screep.building

import screeps.api.*

fun Room.getMyCreeps() = find(FIND_MY_CREEPS)

fun Room.getMyStructures() = find(FIND_MY_STRUCTURES)

fun Room.getMyConstructionSites() = find(FIND_MY_CONSTRUCTION_SITES)

fun Room.getMainSpawn() = getMyStructures()
    .firstOrNull { (it.structureType == STRUCTURE_SPAWN) }

fun Room.getTowers() = getMyStructures()
    .filter { it.structureType == STRUCTURE_TOWER }

fun Room.getDamagedBuildings() = getMyStructures()
    .filter { it.hits < it.hitsMax }

fun Room.getDamagedCreeps() = getMyCreeps()
    .filter { it.hits < it.hitsMax }

fun Room.storageWithEnergy(): StoreOwner? {
    val myStore = storage?.unsafeCast<Store>()
    return if (myStore != null && myStore.getUsedCapacity(RESOURCE_ENERGY) > 0) {
        myStore.unsafeCast<StoreOwner>()
    } else {
        null
    }
}

