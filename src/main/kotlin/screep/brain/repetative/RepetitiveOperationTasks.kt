
package screep.brain.repetative

import screep.building.doYourJobTower
import screep.context.RoomContext
import screep.memory.hasDamagedBuilding
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.jsObject

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
    val body = arrayOf<BodyPartConstant>(WORK, CARRY, MOVE)

    if (roomContext.room.energyAvailable < body.sumOf { BODYPART_COST[it]!! }) {
        return
    }

    val creeps = roomContext.myCreeps

    val role: Role = when {
        creeps.count { it.memory.role == Role.HARVESTER } < 2 -> Role.HARVESTER

        creeps.none { it.memory.role == Role.UPGRADER } -> Role.UPGRADER

        creeps.count { it.memory.role == Role.BUILDER } < 2 &&
                roomContext.myConstructionSites.isNotEmpty()-> Role.BUILDER

        roomContext.myTowers.isNotEmpty() &&
                creeps.count { it.memory.role == Role.REPAIRER } < 1 &&
                roomContext.room.memory.hasDamagedBuilding -> Role.REPAIRER

        else -> return
    }

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