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
            if (buildConsturctionSite(controllerLevel, roomContext)) {
                return
            } else if (buildRampart(roomContext)) {
                return
            } else if (buildLinks(roomContext)) {
                return
            }
            else (buildContainer(roomContext))
        }

        private fun buildLinks(roomContext: RoomContext): Boolean {
            if (!isRoomReadyToContainerAndLink(roomContext)) return false
            for (source in roomContext.mySources.map { it.pos }) {
                if (linkConstructionCreating(source, roomContext, 2)) return true
            }
            return linkConstructionCreating(roomContext.room.storage!!.pos, roomContext, 1)
        }

        private fun linkConstructionCreating(pos: RoomPosition, roomContext: RoomContext, range: Int): Boolean {
            val links = pos.findInRange(FIND_MY_STRUCTURES, range)
                .filter { it.structureType == STRUCTURE_LINK }
            if (links.isEmpty()) {
                putConstructionSite(roomContext, pos, range, STRUCTURE_LINK)
                return true
            }
            return false
        }

        private fun buildContainer(roomContext: RoomContext) {
            if (!isRoomReadyToContainerAndLink(roomContext)) return
            val sources = roomContext.mySources // itt majd a mineral source-ot is hozzátenni, és pos-á alakítani
            for (source in sources) {
                val containers = source.pos.findInRange(FIND_STRUCTURES, 2)
                    .filter { it.structureType == STRUCTURE_CONTAINER }
                if (containers.size < 1) { // constans legyen
                    putConstructionSite(roomContext, source.pos, containers.size + 1, STRUCTURE_CONTAINER) // konstans legyen
                }
            }
        }

        private fun isRoomReadyToContainerAndLink(roomContext: RoomContext): Boolean {
            if (roomContext.myStructures.firstOrNull { it.structureType == STRUCTURE_STORAGE } == null ||
                roomContext.room.controller?.level < 6) {
                return false
            }
            return true
        }

        private fun putConstructionSite(roomContext: RoomContext, pos: RoomPosition, range: Int,
                                        str: BuildableStructureConstant) {
            val structures = (roomContext.myStructures + roomContext.containers).map { Coordinate(it.pos) }
            val terrain = roomContext.myTerrain
            val position = Coordinate(pos).getFullNeighbours(range)
                .asSequence()
                .filter { it.inNormalRange()}
                .filterNot { structures.contains(it) }
                .filterNot { terrain[it.x, it.y] == TERRAIN_MASK_WALL }
                .filterNot { terrain[it.x, it.y] == TERRAIN_MASK_LAVA }
                .map { RoomPosition(it.x, it.y, roomContext.room.name) }
                .minByOrNull { roomContext.room.findPath(it, roomContext.room.storage!!.pos).size}
            position?.let { roomContext.room.createConstructionSite(it, str) }
        }

        private fun buildRampart(roomContext: RoomContext): Boolean {
            val structureNeedsRampart = roomContext.myStructuresToCoverWithRampart
                .filterNot { it.hasRampart(roomContext.myRamparts) }
                .firstOrNull()
            return if (structureNeedsRampart != null) {
                roomContext.room.createConstructionSite(structureNeedsRampart.pos, STRUCTURE_RAMPART)
                true
            } else false
        }

        private fun buildConsturctionSite(controllerLevel: Int, roomContext: RoomContext): Boolean {
            buildingWithLimits
                .forEach { entry ->
                    val buildingType = entry.key
                    val listOfPairs = entry.value.filter { it.first <= controllerLevel }.sortedByDescending { it.first }
                    val numberOfBuilding = if (listOfPairs.isNotEmpty()) listOfPairs[0].second else 0

                    if (roomContext.myStructures.count { it.structureType == buildingType } < numberOfBuilding) {
                        tryToCreateConstructionSite(roomContext, buildingType)
                        return true
                    }
                }
            return false
        }

    }
}

private fun tryToCreateConstructionSite(roomContext: RoomContext, buildingType: BuildableStructureConstant) {
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
        coordinate.getCornerNeighbours()
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
    buildingLimits[STRUCTURE_EXTENSION] = listOf(Pair(2,5), Pair(3,10), Pair(4, 20), Pair(5, 30), Pair(6, 40), Pair(7, 50), Pair(8, 60))
    buildingLimits[STRUCTURE_TOWER] = listOf(Pair(3,1), Pair(5, 2), Pair(7, 3), Pair(8, 6))
    buildingLimits[STRUCTURE_STORAGE] = listOf(Pair(4,1))
    buildingLimits[STRUCTURE_TERMINAL] = listOf(Pair(6,1))
    return buildingLimits
}

data class Coordinate(val x: Int, val y: Int) {
    constructor(pos: RoomPosition) : this(pos.x, pos.y)

    fun getCornerNeighbours(): List<Coordinate> = listOf(
        Coordinate(x + 1, y + 1),
        Coordinate(x - 1, y - 1),
        Coordinate(x + 1, y - 1),
        Coordinate(x - 1, y + 1)
    )

    fun getFullNeighbours(range: Int): List<Coordinate> {
        val coordinates = mutableListOf<Coordinate>()
        for (i in -range..range) {
            for (j in -range..range) {
                if (i != 0 || j != 0) {
                    coordinates.add(Coordinate(x + i, y + j))
                }
            }
        }
        return coordinates
    }


    fun inNormalRange() : Boolean =
        x in 1..48 && y in 1..49

    infix fun isNearTo(coordinate: Coordinate): Boolean =
        abs(coordinate.x - x) <= 1 && abs(coordinate.y - y) <= 1
}
