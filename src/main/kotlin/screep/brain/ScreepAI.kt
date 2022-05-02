package screep.brain

import screep.brain.repetative.RepetitiveTasks
import screep.building.BuildingConstructor
import screep.memory.numberOfCreeps
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.unsafe.jsObject

fun gameLoop() {
    RepetitiveTasks.doTasks()

    val mainSpawns = Game.spawns.values
        .groupBy { it.room.name }
        .mapNotNull { it.value.firstOrNull() }

    for (mainSpawn in mainSpawns) {
        // just an example of how to use room memory
        mainSpawn.room.memory.numberOfCreeps = mainSpawn.room.find(FIND_CREEPS).count()

        spawnCreeps(Game.creeps.values, mainSpawn)

        BuildingConstructor.doConstruct(mainSpawn)

        for ((_, creep) in Game.creeps) {
            when (creep.memory.role) {
                Role.HARVESTER -> creep.harvest()
                Role.BUILDER -> creep.build()
                Role.REPAIRER -> creep.repair()
                Role.UPGRADER -> creep.upgrade(mainSpawn.room.controller!!)
                else -> creep.assignRole()
            }
        }
    }

}

private fun spawnCreeps(
        creeps: Array<Creep>,
        spawn: StructureSpawn
) {

    val body = arrayOf<BodyPartConstant>(WORK, CARRY, MOVE)

    if (spawn.room.energyAvailable < body.sumOf { BODYPART_COST[it]!! }) {
        return
    }

    val role: Role = when {
        creeps.count { it.memory.role == Role.HARVESTER } < 2 -> Role.HARVESTER

        creeps.none { it.memory.role == Role.UPGRADER } -> Role.UPGRADER

        creeps.count { it.memory.role == Role.BUILDER } < 2 &&
                spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isNotEmpty()-> Role.BUILDER

        creeps.count { it.memory.role == Role.REPAIRER } < 2 &&
                spawn.room.find(FIND_MY_STRUCTURES).any { it.hits < it.hitsMax } -> Role.REPAIRER

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

