package screep.building

import screep.constant.marketOrderSize
import screep.constant.storeMinimumLevelToBuyEnergyLimit
import screep.constant.storeToTerminalEnergyLimit
import screep.constant.terminalRelatedLimit
import screep.context.RoomContext
import screep.memory.PriceHistory
import screep.memory.doTerminalJob
import screep.memory.prices
import screeps.api.*
import screeps.api.structures.StructureStorage
import screeps.api.structures.StructureTerminal

class TerminalManager {
    companion object Construct {
        fun doTerminalJobs(roomContexts: List<RoomContext>) {
            if (global.Memory.doTerminalJob > 0) {
                global.Memory.doTerminalJob--
                return
            }
            global.Memory.doTerminalJob = terminalRelatedLimit

            Game.market.getHistory(RESOURCE_ENERGY).last().let {
                global.Memory.prices = listOf(PriceHistory(it.resourceType, it.avgPrice, it.stddevPrice))
            }

            val price by lazy { global.Memory.prices.firstOrNull { it.type == RESOURCE_ENERGY } }

            for (roomContext in roomContexts) {
                val myOrders by lazy { myOrdersInThisRoom(roomContext) }

                val terminal = roomContext.myTerminal
                    .map { it.unsafeCast<StructureTerminal>() }
                    .firstOrNull()

                terminal?.let {
                    if (hasSufficientEnergyToSell(roomContext)) {
                        cancelMyOrders(myOrders, ORDER_BUY)
                        if (hasNoSuchOrders(myOrders, ORDER_SELL)) {
                            createOrder(price, roomContext, ORDER_SELL)
                        } else {
                            refinePriceAtSellOrders(price, myOrders)
                        }
                    }
                    if (belowMinimumEnergyLimit(roomContext)) {
                        cancelMyOrders(myOrders, ORDER_SELL)

                        if (hasNoSuchOrders(myOrders, ORDER_BUY)) {
                            createOrder(price, roomContext, ORDER_BUY)
                        } else {
                            refinePriceAtBuyOrders(price, myOrders)
                        }
                    }
                }
            }
        }

        private fun hasNoSuchOrders(myOrders: List<JsPair<String, Market.Order>>, orderConstant: OrderConstant) =
            myOrders.none { it.component2().type == orderConstant && it.component2().resourceType == RESOURCE_ENERGY }

        private fun belowMinimumEnergyLimit(roomContext: RoomContext) =
            roomContext.room.storage.unsafeCast<StructureStorage>().store[RESOURCE_ENERGY] < storeMinimumLevelToBuyEnergyLimit

        private fun hasSufficientEnergyToSell(roomContext: RoomContext) =
            roomContext.room.storage.unsafeCast<StructureStorage>().store[RESOURCE_ENERGY] > storeToTerminalEnergyLimit

        private fun refinePriceAtSellOrders(
            price: PriceHistory?,
            myOrders: List<JsPair<String, Market.Order>>
        ) {
            price?.let { p ->
                myOrders.filter { it.component2().type == ORDER_SELL }
                    .filter { it.component2().resourceType == RESOURCE_ENERGY }
                    .filter { it.component2().price * 0.95 > (p.price - p.stdDevPrice).toDouble() }
                    .forEach { Game.market.changeOrderPrice(it.component1(), (p.price - p.stdDevPrice).toDouble()) }
            }
        }

        private fun refinePriceAtBuyOrders(
            price: PriceHistory?,
            myOrders: List<JsPair<String, Market.Order>>
        ) {
            price?.let { p ->
                myOrders.filter { it.component2().type == ORDER_BUY }
                    .filter { it.component2().resourceType == RESOURCE_ENERGY }
                    .filter { it.component2().price * 1.05 < (p.price).toDouble() }
                    .forEach { Game.market.changeOrderPrice(it.component1(), (p.price).toDouble()) }
            }
        }

        private fun createOrder(price: PriceHistory?, roomContext: RoomContext, orderConstant: OrderConstant) {
            price?.let {
                val priceOfOrder = if (orderConstant == ORDER_SELL) (it.price - it.stdDevPrice).toDouble() else (it.price).toDouble()
                Game.market.createOrder(
                    OrderCreationParams(
                        orderConstant,
                        RESOURCE_ENERGY,
                        priceOfOrder,
                        marketOrderSize,
                        roomContext.room.name
                    )
                )
            }
        }

        private fun cancelMyOrders(myOrders: List<JsPair<String, Market.Order>>, orderConstant: OrderConstant) {
            val buyEnergyOrders = myOrders.filter { it.component2().type == orderConstant }
                .filter { it.component2().resourceType == RESOURCE_ENERGY }
            buyEnergyOrders.forEach { Game.market.cancelOrder(it.component1()) }
        }

        private fun myOrdersInThisRoom(roomContext: RoomContext) = Game.market.orders.entries
            .filter { it.component2().active }
            .filter { it.component2().roomName == roomContext.room.name }
    }
}