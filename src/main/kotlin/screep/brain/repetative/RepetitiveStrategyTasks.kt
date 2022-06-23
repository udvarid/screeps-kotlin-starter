

package screep.brain.repetative

import screep.building.BuildingConstructor
import screep.constant.*
import screep.context.RoomContext
import screep.memory.*
import screep.roles.structureNotToRepair
import screeps.api.*
import screeps.api.structures.StructureStorage
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
            doTerminalJobs(roomContexts)
        }

    }
}

private fun doTerminalJobs(roomContexts: List<RoomContext>) {
    if (global.Memory.doTerminalJob > 0) {
        global.Memory.doTerminalJob--
        return
    }
    global.Memory.doTerminalJob = terminalRelatedLimit

    Game.market.getHistory(RESOURCE_ENERGY).last().let {
        global.Memory.prices = listOf(PriceHistory(it.resourceType, it.avgPrice, it.stddevPrice))
    }

    for (roomContext in roomContexts) {
        val myOrders by lazy { Game.market.orders.entries
            .filter { it.component2().active }
            .filter { it.component2().roomName == roomContext.room.name } }

        val terminal = roomContext.myStructures
            .filter { it.structureType == STRUCTURE_TERMINAL}
            .map { it.unsafeCast<StructureTerminal>() }
            .firstOrNull()

        terminal?.let {
            if (roomContext.room.storage.unsafeCast<StructureStorage>().store[RESOURCE_ENERGY] > storeToTerminalEnergyLimit) {
                myOrders.filter { it.component2().type == ORDER_BUY }
                    .filter { it.component2().resourceType == RESOURCE_ENERGY }
                    .forEach {Game.market.cancelOrder(it.component1())}
                val price = global.Memory.prices.firstOrNull { it.type == RESOURCE_ENERGY }
                if (myOrders.none { it.component2().type == ORDER_SELL && it.component2().resourceType == RESOURCE_ENERGY }) {
                    price?.let {
                        Game.market.createOrder(OrderCreationParams(ORDER_SELL,
                            RESOURCE_ENERGY,
                            (it.price - it.stdDevPrice).toDouble(),
                            marketOrderSize,
                            roomContext.room.name))
                    }
                } else {
                    price?.let {p ->
                        myOrders.filter { it.component2().type == ORDER_SELL }
                            .filter { it.component2().resourceType == RESOURCE_ENERGY }
                            .filter { it.component2().price * 0.95 > (p.price - p.stdDevPrice).toDouble() }
                            .forEach { Game.market.changeOrderPrice(it.component1(), (p.price - p.stdDevPrice).toDouble()) }
                    }
                }
            }
            if (roomContext.room.storage.unsafeCast<StructureStorage>().store[RESOURCE_ENERGY] < storeMinimumLevelToBuyEnergyLimit) {
                myOrders.filter { it.component2().type == ORDER_SELL }
                    .filter { it.component2().resourceType == RESOURCE_ENERGY }
                    .forEach {Game.market.cancelOrder(it.component1())}

                val price = global.Memory.prices.firstOrNull { it.type == RESOURCE_ENERGY }
                if (myOrders.none { it.component2().type == ORDER_BUY && it.component2().resourceType == RESOURCE_ENERGY}) {
                    price?.let {
                        Game.market.createOrder(OrderCreationParams(ORDER_BUY,
                            RESOURCE_ENERGY,
                            (it.price).toDouble(),
                            marketOrderSize,
                            roomContext.room.name))
                    }
                } else {
                    price?.let {p ->
                        myOrders.filter { it.component2().type == ORDER_BUY }
                            .filter { it.component2().resourceType == RESOURCE_ENERGY }
                            .filter { it.component2().price * 1.05 < (p.price).toDouble() }
                            .forEach { Game.market.changeOrderPrice(it.component1(), (p.price).toDouble()) }
                    }
                }
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
