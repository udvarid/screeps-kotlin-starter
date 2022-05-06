
package screep.brain.repetative

import screep.building.doYourJobTower
import screep.building.getMyConstructionSites
import screep.building.getMyCreeps
import screep.building.getTowers
import screep.memory.hasDamagedBuilding
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.api.structures.StructureTower
import screeps.utils.lazyPerTick
import screeps.utils.unsafe.jsObject

class RepetitiveOperationTasks {

    companion object Tasks {
        fun doTasks(spawns: List<StructureSpawn>) {
            operateTowers(spawns)
            reOrganiseUpgradersWhenNecessary(spawns)
            spawnCreeps(spawns)
            giveWorkToCreeps()
        }

    }
}
private fun reOrganiseUpgradersWhenNecessary(spawns: List<StructureSpawn>) {
    for (spawn in spawns) {
        val creeps = spawn.room.getMyCreeps()
        val upgraders = creeps.filter { it.memory.role == Role.UPGRADER }
        val builders by lazy { creeps.filter { it.memory.role == Role.BUILDER } }
        if (upgraders.size > 1 && builders.size < 2 && spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isNotEmpty()) {
            upgraders.maxByOrNull { it.ticksToLive }!!.memory.role = Role.BUILDER
        }
    }
}

private fun operateTowers(spawns: List<StructureSpawn>) {
    for (spawn in spawns) {
        spawn.room
            .getTowers()
            .map { it.unsafeCast<StructureTower>() }
            .forEach { it.doYourJobTower()}
    }
}

private fun giveWorkToCreeps() {
    for (creep in Game.creeps.values) {
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvest()
            Role.BUILDER -> creep.build()
            Role.REPAIRER -> creep.repair()
            Role.UPGRADER -> creep.upgrade()
            else -> creep.assignRole()
        }
    }
}

private fun spawnCreeps(spawns: List<StructureSpawn>) {
    for (spawn in spawns) {

        val body = arrayOf<BodyPartConstant>(WORK, CARRY, MOVE)

        if (spawn.room.energyAvailable < body.sumOf { BODYPART_COST[it]!! }) {
            return
        }

        val creeps = spawn.room.getMyCreeps()

        val role: Role = when {
            creeps.count { it.memory.role == Role.HARVESTER } < 2 -> Role.HARVESTER

            creeps.none { it.memory.role == Role.UPGRADER } -> Role.UPGRADER

            creeps.count { it.memory.role == Role.BUILDER } < 2 &&
                    spawn.room.getMyConstructionSites().isNotEmpty()-> Role.BUILDER

            spawn.room.getTowers().isNotEmpty() &&
                    creeps.count { it.memory.role == Role.REPAIRER } < 1 &&
                    spawn.room.memory.hasDamagedBuilding -> Role.REPAIRER

            else -> return
        }

        val newName = "${role.name}_${Game.time}"
        val code = spawn.spawnCreep(body, newName, options {
            memory = jsObject<CreepMemory> { this.role = role }
        })

        when (code) {
            OK -> console.log("spawning $newName with body $body")
            ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
            else -> console.log("unhandled error code $code")
        }
    }
}