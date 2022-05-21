
package screep.brain.repetative

import screep.building.BuildingConstructor
import screep.constant.constructionRelatedLimit
import screep.constant.enemyDetectorLimit
import screep.context.RoomContext
import screep.memory.hasDamagedBuilding
import screep.memory.inspectCounterOfBuilding
import screep.memory.inspectCounterOfEnemyDetecting
import screep.memory.underAttack
import screep.roles.structureNotToRepair
import screeps.api.*
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete

class RepetitiveStrategyTasks {

    companion object Tasks {
        fun doTasks(roomContexts: List<RoomContext>) {
            memoryClearing(Game.creeps)
            detectingEnemies(roomContexts)
            doConstructionRelatedJobs(roomContexts)
        }
    }
}

private fun doConstructionRelatedJobs(roomContexts: List<RoomContext>) {

    if (global.Memory.inspectCounterOfBuilding > 0) {
        global.Memory.inspectCounterOfBuilding--
        return
    }
    global.Memory.inspectCounterOfBuilding = constructionRelatedLimit

    for (roomContext in roomContexts) {
        BuildingConstructor.doConstruct(roomContext)
        with(roomContext.room) {
            memory.hasDamagedBuilding = roomContext.damagedBuildings
                .filterNot { structureNotToRepair.contains(it.structureType) }
                .isNotEmpty()
        }
    }
}

private fun detectingEnemies(roomContexts: List<RoomContext>) {

    if (global.Memory.inspectCounterOfEnemyDetecting > 0) {
        global.Memory.inspectCounterOfEnemyDetecting--
        return
    }
    global.Memory.inspectCounterOfEnemyDetecting = enemyDetectorLimit

    for (roomContext in roomContexts) {
        val enemyScreeps = roomContext.enemyCreeps.count()
        roomContext.room.memory.underAttack = enemyScreeps > 0
        if (roomContext.room.memory.underAttack) {
            console.log("Room ", roomContext.room.name, " is under attack!")
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
