package screep.roles

import screep.building.getMyConstructionSites
import screep.constant.creepSuicideLimit
import screep.constant.energySurplusLimitForSecondUpgrader
import screep.context.RoomContext
import screep.memory.hasDamagedBuilding
import screeps.api.*
import screep.memory.role
import screep.memory.storeEnergySnapshot
import screep.memory.underAttack

enum class Role {
    UNASSIGNED,
    HARVESTER,
    BUILDER,
    UPGRADER,
    REPAIRER,
    HAULER
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
fun Creep.shouldDie(): Boolean = ticksToLive < creepSuicideLimit && store[RESOURCE_ENERGY] == 0

val structuresRequiresEnergy : List<StructureConstant> =
    listOf(STRUCTURE_EXTENSION, STRUCTURE_SPAWN, STRUCTURE_TOWER, STRUCTURE_STORAGE)

private fun Creep.canBeHarvester(): Boolean = getActiveBodyparts(WORK) > 0 && getActiveBodyparts(CARRY) > 0

fun getEnergyFillPriority(structureType: StructureConstant, room: Room): Int =
    when (structureType) {
        STRUCTURE_TOWER -> if (room.memory.underAttack) 4 else 2
        STRUCTURE_SPAWN, STRUCTURE_EXTENSION -> 3
        else -> 1
    }


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

fun logicForHauler(roomContext: RoomContext) : Boolean {
    val storageEnergy = roomContext.room.storage.unsafeCast<StoreOwner>().store[RESOURCE_ENERGY] ?: 0
    return storageEnergy > 0
}

fun logicForUpgrader(roomContext: RoomContext) : Boolean {
    val numberOfExistingCreeps = roomContext.myCreeps.count { it.memory.role == Role.UPGRADER }
    val storageEnergy by lazy { roomContext.room.storage.unsafeCast<StoreOwner>().store[RESOURCE_ENERGY] ?: 0 }
    return numberOfExistingCreeps == 0 ||
            storageEnergy >= roomContext.room.memory.storeEnergySnapshot + energySurplusLimitForSecondUpgrader
}


val creepPlans = listOf(
    CreepPlan(Role.HARVESTER, 2, arrayOf(WORK, CARRY, MOVE, MOVE), 6),
    CreepPlan(Role.UPGRADER, 1, arrayOf(WORK, CARRY, MOVE, MOVE), 6, ::logicForUpgrader),
    CreepPlan(Role.BUILDER, 2, arrayOf(WORK, CARRY, MOVE, MOVE), 6, ::logicForBuilder),
    CreepPlan(Role.REPAIRER, 1, arrayOf(WORK, CARRY, MOVE, MOVE), 6, ::logicForRepairer),
    CreepPlan(Role.HAULER, 1, arrayOf(CARRY, MOVE), 10, ::logicForHauler)
)
