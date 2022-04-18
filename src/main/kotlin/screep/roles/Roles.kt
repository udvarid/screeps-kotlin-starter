package screep.roles

import screeps.api.*
import screep.memory.role

enum class Role {
    UNASSIGNED,
    HARVESTER,
    BUILDER,
    UPGRADER
}

fun Creep.assignRole() {
    memory.role = Role.HARVESTER
}
