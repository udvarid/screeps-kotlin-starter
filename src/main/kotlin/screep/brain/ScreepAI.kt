package screep.brain

import screep.brain.repetative.RepetitiveTasks
import screep.memory.numberOfCreeps
import screep.memory.role
import screep.roles.*
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.unsafe.jsObject

fun gameLoop() {
    val mainSpawn: StructureSpawn = Game.spawns.values.firstOrNull() ?: return

    //create general repetitive tasks (like memory sweeping)
    RepetitiveTasks.doTasks()

    // just an example of how to use room memory
    mainSpawn.room.memory.numberOfCreeps = mainSpawn.room.find(FIND_CREEPS).count()

    //make sure we have at least some creeps
    spawnCreeps(Game.creeps.values, mainSpawn)

    // build a few extensions so we can have 550 energy
    val controller = mainSpawn.room.controller

    /*
    1, csak akkor nézzük, ha nincs construction site, előbb azt meg kell építeni
    2, leszedjük a structure-öket
    3, a mainSpawn-tól kezdve kifelé haladva rugó/csiga alakban minden egyes pontnál (jobb/bal nem üres legyen)
        - plain vagy swamp
        - nincs ott építményünk
        - a spawn-tól oda lehessen találni, legyen út
        - ne legyen a közelben Source vagy Mine
     */

    val terrain = mainSpawn.room.getTerrain()
    when (terrain[mainSpawn.pos.x, mainSpawn.pos.y + 3]) {
        TERRAIN_MASK_WALL -> console.log("This is wall")
        TERRAIN_MASK_SWAMP -> console.log("This is swamp")
        TERRAIN_MASK_LAVA -> console.log("This is lava")
        else -> console.log("This is plain")
    }

    if (controller != null && controller.level >= 2) {
        when (controller.room.find(FIND_MY_STRUCTURES).count { it.structureType == STRUCTURE_EXTENSION }) {
            0 -> controller.room.createConstructionSite(29, 27, STRUCTURE_EXTENSION)
            1 -> controller.room.createConstructionSite(28, 27, STRUCTURE_EXTENSION)
            2 -> controller.room.createConstructionSite(27, 27, STRUCTURE_EXTENSION)
            3 -> controller.room.createConstructionSite(26, 27, STRUCTURE_EXTENSION)
            4 -> controller.room.createConstructionSite(25, 27, STRUCTURE_EXTENSION)
            5 -> controller.room.createConstructionSite(24, 27, STRUCTURE_EXTENSION)
            6 -> controller.room.createConstructionSite(23, 27, STRUCTURE_EXTENSION)
        }
    }

    for ((_, creep) in Game.creeps) {
        when (creep.memory.role) {
            Role.HARVESTER -> creep.harvest()
            Role.BUILDER -> creep.build()
            Role.UPGRADER -> creep.upgrade(mainSpawn.room.controller!!)
            else -> creep.assignRole()
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

        spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isNotEmpty() &&
                creeps.count { it.memory.role == Role.BUILDER } < 2 -> Role.BUILDER

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

