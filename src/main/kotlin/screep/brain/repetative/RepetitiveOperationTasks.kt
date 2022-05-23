
package screep.brain.repetative

import screep.building.doYourJobTower
import screep.context.RoomContext
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.jsObject
import kotlin.math.min

class RepetitiveOperationTasks {

    companion object Tasks {
        fun doTasks(roomContexts: List<RoomContext>) {
            operateTowers(roomContexts)
            for (roomContext in roomContexts) {
                if (!screepWasReorganised(roomContext))
                    spawnCreeps(roomContext)
            }
            giveWorkToCreeps(roomContexts)
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
        val roomContext = roomContexts.firstOrNull { it.room.name == creep.room.name }
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvestMe(roomContext)
            Role.BUILDER -> creep.buildMe(roomContext)
            Role.REPAIRER -> creep.repairMe(roomContext)
            Role.UPGRADER -> creep.upgradeMe(roomContext)
            else -> creep.assignRole()
        }
    }
}

private fun spawnCreeps(roomContext: RoomContext) {
    val creeps = roomContext.myCreeps

    var role: Role? = null
    for (creepPlan in creepPlans) {
        if (roomContext.room.energyAvailable < creepPlan.body.sumOf { BODYPART_COST[it]!! }) {
            continue
        }
        val numberOfRelatedCreeps = creeps.count { it.memory.role == creepPlan.role }
        val extraLogic = creepPlan.logic?.let { it(roomContext) } ?: true
        if (numberOfRelatedCreeps < creepPlan.number && extraLogic) {
            role = creepPlan.role
            break
        }
    }

    if (role == null) {
        return
    }

    val finalPlan = creepPlans.first {it.role == role}
    val minimumCost = finalPlan.body.sumOf { BODYPART_COST[it]!! }
    val multiplicator = min(roomContext.room.energyCapacityAvailable / minimumCost, finalPlan.max)
    val finalCost = minimumCost * multiplicator
    var body: Array<BodyPartConstant> = finalPlan.body.copyOf()
    if (finalPlan.role == Role.HARVESTER && creeps.count { it.memory.role == finalPlan.role } == 0 ) {
        val minMultiplicator = min(roomContext.room.energyAvailable / minimumCost, finalPlan.max)
        for (i in 1 until minMultiplicator) {
            body += finalPlan.body
        }
    } else if (roomContext.room.energyAvailable < finalCost) {
        return
    } else {
        for (i in 1 until multiplicator) {
            body += finalPlan.body
        }
    }

    val newName = "${role.name}_${Game.time}"
    val code = roomContext.spawn!!.spawnCreep(body, newName, options {
        memory = jsObject<CreepMemory> { this.role = this.role }
    })

    when (code) {
        OK -> console.log("spawning $newName with body $body")
        ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
        else -> console.log("unhandled error code $code")
    }
}