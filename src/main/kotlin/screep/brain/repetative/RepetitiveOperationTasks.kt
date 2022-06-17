
package screep.brain.repetative

import screep.building.doYourJobTower
import screep.context.RoomContext
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.jsObject
import kotlin.math.min

class RepetitiveOperationTasks {

    companion object Tasks {
        fun doTasks(roomContexts: List<RoomContext>) {
            operateTowers(roomContexts)
            for (roomContext in roomContexts) {
                operateLinks(roomContext)
                if (!screepWasReorganised(roomContext)) {
                    spawnCreeps(roomContext)
                }
            }
            giveWorkToCreeps(roomContexts)
        }

    }
}
private fun operateLinks(roomContext: RoomContext) {
    val store = roomContext.room.storage
    val links = roomContext.myStructures
        .filter { it.structureType == STRUCTURE_LINK }
        .map { it.unsafeCast<StructureLink>() }
    if (store != null && links.size >= 2) {
        val centralLink = store.pos.findInRange(FIND_MY_STRUCTURES, 1)
            .map { it.unsafeCast<StructureLink>() }
            .firstOrNull { it.structureType == STRUCTURE_LINK }
        val sourceLinks = links.filterNot { it.id == centralLink?.id }
        if (sourceLinks.size == links.size - 1 && links.map { it.id }.contains(centralLink?.id)) {
            val isCentralLinkFull = centralLink!!.store.getCapacity(RESOURCE_ENERGY) ==
                    centralLink.store[RESOURCE_ENERGY]
            if (centralLink.cooldown == 0 && !isCentralLinkFull) {
                val freeCapacityAtCentral = centralLink.store[RESOURCE_ENERGY]?.let {
                    centralLink.store.getCapacity(RESOURCE_ENERGY)?.minus(it)
                }
                sourceLinks
                    .filter { it.cooldown == 0 }
                    .filter { it.store[RESOURCE_ENERGY] > 0 }
                    .firstOrNull { it.store[RESOURCE_ENERGY] <= freeCapacityAtCentral }?.transferEnergy(centralLink)
            }
        }
    }
}

private fun screepWasReorganised(roomContext: RoomContext) : Boolean {
    val creeps = roomContext.myCreeps
    val upgraders = creeps.filter { it.memory.role == Role.UPGRADER }
    val builders by lazy { creeps.filter { it.memory.role == Role.BUILDER } }
    if (upgraders.size > 1 && builders.size < 2 && roomContext.myConstructionSites.isNotEmpty()) {
        upgraders.maxByOrNull { it.ticksToLive }!!.memory.role = Role.BUILDER
        return true
    }
    return false
}

private fun operateTowers(roomContexts: List<RoomContext>) {
    for (roomContext in roomContexts) {
        roomContext
            .myTowers
            .map { it.unsafeCast<StructureTower>() }
            .forEach { it.doYourJobTower(roomContext)}
    }
}

private fun giveWorkToCreeps(roomContexts: List<RoomContext>) {
    for (creep in Game.creeps.values) {
        if (creep.shouldDie()) {
            creep.suicide()
            continue
        }
        val roomContext = roomContexts.firstOrNull { it.room.name == creep.room.name }
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvestMe(roomContext)
            Role.BUILDER -> creep.buildMe(roomContext)
            Role.REPAIRER -> creep.repairMe(roomContext)
            Role.UPGRADER -> creep.upgradeMe(roomContext)
            Role.HAULER -> creep.haulMe(roomContext)
            else -> creep.assignRole()
        }
    }
}

private fun spawnCreeps(roomContext: RoomContext) {
    val role: Role = getRole(roomContext, roomContext.myCreeps) ?: return
    val body = getBody(creepPlans.first {it.role == role}, roomContext) ?: return

    val newName = "${role.name}_${Game.time}"
    val code = roomContext.spawn!!.spawnCreep(body, newName, options {
        memory = jsObject<CreepMemory> { this.role = role }
    })

    when (code) {
        OK -> console.log("spawning $newName with body $body")
        ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
        else -> console.log("unhandled error code $code")
    }
}

private fun getBody(finalPlan: CreepPlan, roomContext: RoomContext): Array<BodyPartConstant>? {
    var body: Array<BodyPartConstant> = finalPlan.body.copyOf()
    val minimumCost = finalPlan.body.sumOf { BODYPART_COST[it]!! }
    val multiplicator = min(roomContext.room.energyCapacityAvailable / minimumCost, finalPlan.max)
    val finalCost = minimumCost * multiplicator
    if (finalPlan.role == Role.HARVESTER && roomContext.myCreeps.count { it.memory.role == finalPlan.role } == 0 ) {
        val minMultiplicator = min(roomContext.room.energyAvailable / minimumCost, finalPlan.max)
        for (i in 1 until minMultiplicator) {
            body += finalPlan.body
        }
    } else if (roomContext.room.energyAvailable < finalCost) {
        return null
    } else {
        for (i in 1 until multiplicator) {
            body += finalPlan.body
        }
    }
    return body
}

private fun getRole(roomContext: RoomContext, creeps: Array<Creep>): Role? {
    var role: Role? = null
    for (creepPlan in creepPlans) {
        if (roomContext.room.energyAvailable < creepPlan.body.sumOf { BODYPART_COST[it]!! }) {
            continue
        }
        val numberOfRelatedCreeps = creeps.count { it.memory.role == creepPlan.role }
        val extraLogic = creepPlan.logic?.let { it(roomContext) } ?: true
        val numberPlan = if (extraLogic) creepPlan.extraNumber else creepPlan.number
        if (numberOfRelatedCreeps < numberPlan && extraLogic) {
            role = creepPlan.role
            break
        }
    }
    return role
}