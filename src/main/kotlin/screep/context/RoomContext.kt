package screep.context

import screep.building.getDamagedBuildings
import screep.building.getMyConstructionSites
import screep.building.getMyStructures
import screep.building.structureTypesToDefend
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.lazyPerTick

class RoomContext(val room: Room, val spawn: StructureSpawn? = null) {

    val enemyCreeps by lazyPerTick { room.find(FIND_HOSTILE_CREEPS) }
    val myCreeps by lazyPerTick { room.find(FIND_MY_CREEPS) }
    val damagedCreeps by lazyPerTick { myCreeps.filter { it.hits < it.hitsMax } }
    val damagedBuildings by lazyPerTick { room.getDamagedBuildings() }
    val myConstructionSites by lazyPerTick { room.getMyConstructionSites() }
    val myStructures by lazyPerTick { room.getMyStructures() }
    val myRamparts by lazyPerTick { myStructures.filter { it.structureType == STRUCTURE_RAMPART } }
    val myExits by lazyPerTick { room.find(FIND_EXIT) }
    val myMinerals by lazyPerTick { room.find(FIND_MINERALS) }
    val mySources by lazyPerTick { room.find(FIND_SOURCES) }
    val myTerrain by lazyPerTick { room.getTerrain() }
    val myTowers by lazyPerTick { myStructures.filter { it.structureType == STRUCTURE_TOWER } }
    val myStructuresToCoverWithRampart by lazyPerTick {
        myStructures.filter { structureTypesToDefend.contains(it.structureType) }
    }

}