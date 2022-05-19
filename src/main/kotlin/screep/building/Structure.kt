package screep.building

import screeps.api.structures.Structure

fun Structure.hasRampart(): Boolean {
    val ramparts = room.getMyRamparts()
    return ramparts
        .map { it.pos }
        .any { it.x == pos.x && it.y == pos.y}
}