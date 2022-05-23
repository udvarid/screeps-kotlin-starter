package screep.roles

import screep.building.getMyConstructionSites
import screep.context.RoomContext
import screep.memory.hasDamagedBuilding
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

val structureNotToRepair = listOf(STRUCTURE_ROAD, STRUCTURE_WALL, STRUCTURE_RAMPART)


fun Creep.assignRole() {
    if (canBeHarvester()) {
        memory.role = Role.HARVESTER
    } else {
        suicide()
    }
}


fun Creep.findFreeAndActiveSource(sources: Array<Source>): Source? {
    return pos.findClosestByPath(sources
        .filter { it.energy > 0 }
        .toTypedArray())
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

data class CreepPlan(
    val role: Role,
    val number: Int,
    val body: Array<BodyPartConstant>,
    val max: Int,
    val logic: ((roomContext: RoomContext) -> Boolean)? = null
)

fun logicForRepairer(roomContext: RoomContext) : Boolean =
    roomContext.room.memory.hasDamagedBuilding &&
            roomContext.myTowers.isEmpty()

fun logicForBuilder(roomContext: RoomContext) : Boolean =
    roomContext.room.getMyConstructionSites().isNotEmpty()

val creepPlans = listOf(
    CreepPlan(Role.HARVESTER, 2, arrayOf(WORK, CARRY, MOVE, MOVE), 5),
    CreepPlan(Role.UPGRADER, 1, arrayOf(WORK, CARRY, MOVE, MOVE), 5),
    CreepPlan(Role.BUILDER, 2, arrayOf(WORK, CARRY, MOVE, MOVE), 5, ::logicForBuilder),
    CreepPlan(Role.REPAIRER, 1, arrayOf(WORK, CARRY, MOVE, MOVE), 5, ::logicForRepairer)
)
