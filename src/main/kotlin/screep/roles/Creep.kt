package screep.roles

import screeps.api.*
import screep.memory.role

enum class Role {
    UNASSIGNED,
    HARVESTER,
    BUILDER,
    UPGRADER,
    REPAIRER
}

enum class CreepState {
    UNKNOWN,
    IDLE,
    TRANSFERRING_ENERGY,
    HARVESTING,
    UPGRADING,
    REPAIRING,
    BUILDING
}


fun Creep.assignRole() {
    if (canBeHarvester()) {
        memory.role = Role.HARVESTER
    } else {
        suicide()
    }
}

fun Creep.findFreeAndActiveSource(room: Room): Source? {
    val sources = room.find(FIND_SOURCES)
        .filter { it.energy > 0 }
        .toTypedArray()
    return pos.findClosestByPath(sources)
}

fun Creep.goHarvest(source: Source) {
    if (harvest(source) == ERR_NOT_IN_RANGE) {
        moveTo(source.pos, options { reusePath = 10 })
    }
}

fun Creep.goWithdraw(store: StoreOwner) {
    if (withdraw(store, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
        moveTo(store.pos, options { reusePath = 10 })
    }
}

val structuresRequiresEnergy : List<StructureConstant> =
    listOf(STRUCTURE_EXTENSION, STRUCTURE_SPAWN, STRUCTURE_TOWER, STRUCTURE_STORAGE)

private fun Creep.canBeHarvester(): Boolean = getActiveBodyparts(WORK) > 0 && getActiveBodyparts(CARRY) > 0
