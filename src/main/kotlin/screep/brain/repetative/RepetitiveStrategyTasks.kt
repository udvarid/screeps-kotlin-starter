
package screep.brain.repetative

import screep.building.BuildingConstructor
import screep.constant.buildingPlanningLimit
import screep.constant.enemyDetectorLimit
import screep.memory.inspectCounterOfBuilding
import screep.memory.inspectCounterOfEnemyDetecting
import screep.memory.underAttack
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete

class RepetitiveStrategyTasks {

    companion object Tasks {
        fun doTasks(spawns: List<StructureSpawn>) {
            memoryClearing(Game.creeps)
            detectingEnemies(spawns)
            planBuilding(spawns)
        }
    }
}

private fun planBuilding(spawns: List<StructureSpawn>) {

    if (global.Memory.inspectCounterOfBuilding > 0) {
        global.Memory.inspectCounterOfBuilding--
        return
    }
    global.Memory.inspectCounterOfBuilding = buildingPlanningLimit

    for (spawn in spawns) {
        BuildingConstructor.doConstruct(spawn)
    }
}

private fun detectingEnemies(spawns: List<StructureSpawn>) {

    if (global.Memory.inspectCounterOfEnemyDetecting > 0) {
        global.Memory.inspectCounterOfEnemyDetecting--
        return
    }
    global.Memory.inspectCounterOfEnemyDetecting = enemyDetectorLimit

    for (spawn in spawns) {
        val enemyScreeps = spawn.room.find(FIND_HOSTILE_CREEPS).count()
        spawn.room.memory.underAttack = enemyScreeps > 0
        if (spawn.room.memory.underAttack) {
            console.log("Room ", spawn.room.name, " is under attack!")
        }
    }
}


private fun memoryClearing(creeps: Record<String, Creep>) {
    if (creeps.isEmpty()) return

    for (creepName in Memory.creeps.keys) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}
