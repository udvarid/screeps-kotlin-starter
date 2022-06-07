package screep.building

import screep.context.RoomContext
import screep.memory.underAttack
import screeps.api.*
import kotlin.math.abs

class BuildingConstructor {

    companion object Construct {
        private val buildingWithLimits: Map<BuildableStructureConstant, List<Pair<Int, Int>>> = fillUpBuildingLiWithLimits()

        fun doConstruct(roomContext: RoomContext) {
            if (roomContext.room.memory.underAttack || !isSpawnEligibleForConstructing(roomContext)) {
                return
            }
            val controllerLevel = roomContext.room.controller!!.level
            buildingWithLimits
                .forEach { entry ->
                    val buildingType = entry.key
                    val listOfPairs = entry.value.filter { it.first <= controllerLevel }.sortedByDescending { it.first }
                    val numberOfBuilding = if (listOfPairs.isNotEmpty()) listOfPairs[0].second else 0

                    if (roomContext.myStructures.count { it.structureType == buildingType } < numberOfBuilding) {
                        createConstructionSite(roomContext, buildingType)
                        return
                    }
                }
            val structureNeedsRampart = roomContext.myStructuresToCoverWithRampart
                .filterNot { it.hasRampart(roomContext.myRamparts) }
                .firstOrNull()
            structureNeedsRampart?.let {
                roomContext.room.createConstructionSite(it.pos, STRUCTURE_RAMPART)
            }
        }

    }
}

private fun createConstructionSite(roomContext: RoomContext, buildingType: BuildableStructureConstant) {
    getFreeBuildingSite(roomContext)?.let {
        roomContext.room.createConstructionSite(it.x, it.y, buildingType)
    }
}

private fun getFreeBuildingSite(roomContext: RoomContext): Coordinate? {
    val structures = roomContext.myStructures
    val terrain = roomContext.myTerrain
    val coordinateQueue = mutableListOf(Coordinate(roomContext.spawn!!.pos.x, roomContext.spawn.pos.y))
    var finalCoordinate: Coordinate? = null
    while (finalCoordinate == null && coordinateQueue.isNotEmpty()) {
        val coordinate = coordinateQueue.removeAt(0)
        coordinate.getNeighbours()
            .filter { it.inNormalRange()}
            .filterNot { it in coordinateQueue}
            .forEach { coordinateQueue.add(it) }
        val notOccupied = structures
            .map { it.pos }
            .none { Coordinate(it) == coordinate }
        val goodLocation = terrain[coordinate.x, coordinate.y] != TERRAIN_MASK_WALL &&
                terrain[coordinate.x, coordinate.y] != TERRAIN_MASK_LAVA
        val notBlocking = !isAtBlockingPlace(coordinate, roomContext)
        if (notOccupied && goodLocation && notBlocking) {
            finalCoordinate = coordinate
        }
    }

    return finalCoordinate
}

fun isAtBlockingPlace(coordinate: Coordinate, roomContext: RoomContext): Boolean =
            isNearToSource(coordinate, roomContext) ||
            isNearToMine(coordinate, roomContext) ||
            isNearToController(coordinate, roomContext) ||
            isNearToExit(coordinate, roomContext)

fun isNearToExit(coordinate: Coordinate, roomContext: RoomContext): Boolean {
    val exits = roomContext.myExits
    return exits
        .any {Coordinate(it.x, it.y) isNearTo coordinate}
}

fun isNearToController(coordinate: Coordinate, roomContext: RoomContext): Boolean {
    val controller = roomContext.room.controller
    return if (controller == null) {
        false
    } else {
        Coordinate(controller.pos) isNearTo coordinate
    }
}

fun isNearToMine(coordinate: Coordinate, roomContext: RoomContext): Boolean {
    val mines = roomContext.myMinerals
    return mines
        .any { Coordinate(it.pos) isNearTo coordinate}
}

fun isNearToSource(coordinate: Coordinate, roomContext: RoomContext): Boolean {
    val sources = roomContext.mySources
    return sources
        .any { Coordinate(it.pos) isNearTo coordinate}
}


private fun isSpawnEligibleForConstructing(roomContext: RoomContext): Boolean =
            roomContext.myConstructionSites.isEmpty() &&
            roomContext.room.controller != null &&
            roomContext.room.controller!!.level > 1

private fun fillUpBuildingLiWithLimits(): Map<BuildableStructureConstant, List<Pair<Int, Int>>> {
    val buildingLimits = mutableMapOf<BuildableStructureConstant, List<Pair<Int, Int>>>()
    buildingLimits[STRUCTURE_EXTENSION] = listOf(Pair(2,5), Pair(3,10), Pair(4, 20), Pair(5, 30))
    buildingLimits[STRUCTURE_TOWER] = listOf(Pair(3,1), Pair(5, 2))
    buildingLimits[STRUCTURE_STORAGE] = listOf(Pair(4,1))
    return buildingLimits
}

data class Coordinate(val x: Int, val y: Int) {
    constructor(pos: RoomPosition) : this(pos.x, pos.y)

    fun getNeighbours(): List<Coordinate> = listOf(
        Coordinate(x + 1, y + 1),
        Coordinate(x - 1, y - 1),
        Coordinate(x + 1, y - 1),
        Coordinate(x - 1, y + 1)
    )

    fun inNormalRange() : Boolean =
        x in 1..48 && y in 1..49

    infix fun isNearTo(coordinate: Coordinate): Boolean =
        abs(coordinate.x - x) <= 1 && abs(coordinate.y - y) <= 1
}
