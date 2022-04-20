package screep.building

import screeps.api.*
import screeps.api.structures.StructureSpawn


class BuildingConstructor {

    companion object Construct {
        private val buildingWithLimits: Map<BuildableStructureConstant, List<Pair<Int, Int>>> = fillUpBuildingLiWithmits()

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
    while (finalCoordinate == null && coordinateQueue.isNotEmpty()) {
        val coordinate = coordinateQueue.removeAt(0)
        coordinate.getNeighbours()
            .filter { it.x in 1..49 && it.y in 1..49 }
            .filter { !coordinateQueue.contains(it) }
            .forEach { coordinateQueue.add(it) }
        val notOccupied = structures
            .map { it.pos }
            .none { Coordinate(it.x, it.y) == coordinate }
        val goodLocation = terrain[coordinate.x, coordinate.y] != TERRAIN_MASK_WALL &&
                terrain[coordinate.x, coordinate.y] != TERRAIN_MASK_LAVA &&
                terrain[coordinate.x, coordinate.y] != TERRAIN_MASK_SWAMP
        val notNearToSourceOrController = true //TODO
        if (notOccupied && goodLocation && notNearToSourceOrController) {
            finalCoordinate = coordinate
        }
    }

    return finalCoordinate
}


private fun isSpawnEligibleForConstructing(spawn: StructureSpawn): Boolean =
            spawn.room.find(FIND_MY_CONSTRUCTION_SITES).isEmpty() &&
            spawn.room.controller != null &&
            spawn.room.controller!!.level > 1

private fun fillUpBuildingLiWithmits(): Map<BuildableStructureConstant, List<Pair<Int, Int>>> {
    val buildingLimits = mutableMapOf<BuildableStructureConstant, List<Pair<Int, Int>>>()
    buildingLimits[STRUCTURE_EXTENSION] = listOf(Pair(2,5))
    return buildingLimits
}

data class Coordinate(val x: Int, val y: Int) {
    fun getNeighbours(): List<Coordinate> = listOf(
        Coordinate(x + 1, y + 1),
        Coordinate(x - 1, y - 1),
        Coordinate(x + 1, y - 1),
        Coordinate(x - 1, y + 1)
    )
}
