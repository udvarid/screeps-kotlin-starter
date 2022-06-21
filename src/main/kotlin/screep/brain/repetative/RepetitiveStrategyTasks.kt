
package screep.brain.repetative

import screep.building.BuildingConstructor
import screep.constant.constructionRelatedLimit
import screep.constant.enemyDetectorLimit
import screep.constant.storeEnergySnapshotLimit
import screep.constant.terminalRelatedLimit
import screep.context.RoomContext
import screep.memory.*
import screep.roles.structureNotToRepair
import screeps.api.*
import screeps.api.structures.StructureTerminal
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete

class RepetitiveStrategyTasks {

    companion object Tasks {
        fun doTasks(roomContexts: List<RoomContext>) {
            memoryClearing(Game.creeps)
            detectingEnemies(roomContexts)
            doConstructionRelatedJobs(roomContexts)
            makeStoreEnergySnapshot(roomContexts)
            //doTerminalJobs(roomContexts)
        }

    }
}

private fun doTerminalJobs(roomContexts: List<RoomContext>) {
    if (global.Memory.doTerminalJob > 0) {
        global.Memory.doTerminalJob--
        return
    }
    global.Memory.inspectStoreEnergy = terminalRelatedLimit

    // leszedni memóriába az aktuális árakat (egy tömbbe) ill. a std. dev-et (getHistory-val)


    for (roomContext in roomContexts) {
        val terminal = roomContext.myStructures
            .filter { it.structureType == STRUCTURE_TERMINAL}
            .map { it.unsafeCast<StructureTerminal>() }
            .firstOrNull()
        terminal?.let {
            //terminálban szereplő nyersanyagonként végigmenni
            //energia esetén
                //ha nincs aktív eladásunk,
                    //ha a store több, mint a limit, akkor piaci ár felett betenni
                    //ha a store kisebb, mint az alsó limit, akkor legolcsóbbat megvenni
                        //ha van más terminálunk, aki árul, akkor függetlenül az ártól, tőle megvenni?
                //ha van aktív eladásunk és már X ideje nem tudjuk eladni, akkor zárjuk
            console.log("we have a terminal in room ", roomContext.room.name)
//            val orders = Game.market.getAllOrders()
//                .asSequence()
//                .filter { it.resourceType == RESOURCE_ENERGY }
//                .filter { it.type == ORDER_SELL }
//                .toList()
//            console.log("we have some orders.. ", orders.size)
//            for (order in orders) {
//                console.log(order.amount, order.price)
//            }
            val history = Game.market.getHistory(RESOURCE_ENERGY)
            for (element in history) {
                console.log(element.date, element.avgPrice, element.volume, element.stddevPrice)
            }

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
