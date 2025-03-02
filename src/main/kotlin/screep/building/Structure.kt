package screep.building

import screeps.api.structures.Structure

fun Structure.hasRampart(ramparts : List<Structure>): Boolean {
    return ramparts
        .map { it.pos }
        .any { it.x == pos.x && it.y == pos.y}
}