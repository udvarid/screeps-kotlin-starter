package screep.brain.repetative

import screep.building.BuildingConstructor
import screep.building.TerminalManager
import screep.constant.constructionRelatedLimit
import screep.constant.enemyDetectorLimit
import screep.constant.storeEnergySnapshotLimit
import screep.context.RoomContext
import screep.memory.*
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
            makeStoreEnergySnapshot(roomContexts)
            TerminalManager.doTerminalJobs(roomContexts)
        }

    }
}

private fun makeStoreEnergySnapshot(roomContexts: List<RoomContext>) {
    if (global.Memory.inspectStoreEnergy > 0) {
        global.Memory.inspectStoreEnergy--
        return
    }
    global.Memory.inspectStoreEnergy = storeEnergySnapshotLimit

    for (roomContext in roomContexts) {
        roomContext.room.memory.storeEnergySnapshot =
            roomContext.room.storage.unsafeCast<StoreOwner>().store[RESOURCE_ENERGY] ?: 0
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
                .filterNot { it.structureType == STRUCTURE_CONTAINER && it.hits > it.hitsMax * 0.9 }
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
            safeModeInvestigation(roomContext)
            console.log("Room ", roomContext.room.name, " is under attack!")
        }
    }
}

fun safeModeInvestigation(roomContext: RoomContext) {
    val noFunctioningTower = roomContext.myTowers
        .map { it.unsafeCast<StoreOwner>() }
        .all { it.store[RESOURCE_ENERGY] == 0 }
    val hasSafeModeToActivate =
        roomContext.room.controller?.let {it.safeMode == null && it.safeModeAvailable > 0 && it.safeModeCooldown == null} ?: false
    if (noFunctioningTower && hasSafeModeToActivate) {
        roomContext.room.controller?.activateSafeMode()
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
