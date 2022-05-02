package screep.building

import screeps.api.*
import screeps.api.structures.StructureSpawn
import kotlin.math.abs


class BuildingConstructor {

    companion object Construct {
        private val buildingWithLimits: Map<BuildableStructureConstant, List<Pair<Int, Int>>> = fillUpBuildingLiWithLimits()

        fun doConstruct(spawn: StructureSpawn) {
            if (!isSpawnEligibleForConstructing(spawn)) {
                return
            }
            val controllerLevel = spawn.room.controller!!.level
            buildingWithLimits
                .forEach { entry ->
                    val buildingType = entry.key
                    val listOfPairs = entry.value.filter { it.first <= controllerLevel }.sortedByDescending { it.first }
                    val numberOfBuilding = if (listOfPairs.isNotEmpty()) listOfPairs[0].second else 0

                    if (spawn.room.find(FIND_MY_STRUCTURES).count { it.structureType == buildingType } < numberOfBuilding) {
                        createConstruction(spawn, buildingType)
                        return
                    }
                }
        }

    }
}

private fun createConstruction(spawn: StructureSpawn, buildingType: BuildableStructureConstant) {
    val coordinate: Coordinate? = getFreeBuildingSite(spawn)
    if (coordinate!= null) {
        spawn.room.createConstructionSite(coordinate.x, coordinate.y, buildingType)
    }
}

private fun getFreeBuildingSite(spawn: StructureSpawn): Coordinate? {
    val structures = spawn.room.find(FIND_MY_STRUCTURES)
    val terrain = spawn.room.getTerrain()
    val coordinateQueue = mutableListOf(Coordinate(spawn.pos.x, spawn.pos.y))
    var finalCoordinate: Coordinate? = null
    while (finalCoordinate ==
        null && coordinateQueue.isNotEmpty()) {
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
        val notBlocking = !isAtBlockingPlace(coordinate, spawn.room)
        if (notOccupied && goodLocation && notBlocking) {
            finalCoordinate = coordinate
        }
    }

    return finalCoordinate
}

fun isAtBlockingPlace(coordinate: Coordinate, room: Room): Boolean =
            isNearToSource(coordinate, room) ||
            isNearToMine(coordinate, room) ||
            isNearToController(coordinate, room) ||
            isNearToExit(coordinate, room)

fun isNearToExit(coordinate: Coordinate, room: Room): Boolean {
    val exits = room.find(FIND_EXIT)
    return exits
        .any {Coordinate(it.x, it.y) isNearTo coordinate}
}

fun isNearToController(coordinate: Coordinate, room: Room): Boolean {
    val controller = room.controller
    return if (controller == null) {
        false
    } else {
        Coordinate(controller.pos) isNearTo coordinate
    }
}

fun isNearToMine(coordinate: Coordinate, room: Room): Boolean {
    val mines = room.find(FIND_MINERALS)
    return mines
        .any { Coordinate(it.pos) isNearTo coordinate}
}

fun isNearToSource(coordinate: Coordinate, room: Room): Boolean {
    val sources = room.find(FIND_SOURCES)
    return sources
        .any { Coordinate(it.pos) isNearTo coordinate}
}


private fun isSpawnEligibleForConstructing(spawn: StructureSpawn): Boolean =
            spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isEmpty() &&
            spawn.room.controller != null &&
            spawn.room.controller!!.level > 1

private fun fillUpBuildingLiWithLimits(): Map<BuildableStructureConstant, List<Pair<Int, Int>>> {
    val buildingLimits = mutableMapOf<BuildableStructureConstant, List<Pair<Int, Int>>>()
    buildingLimits[STRUCTURE_EXTENSION] = listOf(Pair(2,5))
    buildingLimits[STRUCTURE_EXTENSION] = listOf(Pair(3,10))
    buildingLimits[STRUCTURE_TOWER] = listOf(Pair(3,1))
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
