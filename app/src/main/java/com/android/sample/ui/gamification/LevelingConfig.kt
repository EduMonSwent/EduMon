import kotlin.math.sqrt

object LevelingConfig {

    fun levelForPoints(points: Int): Int {
        if (points <= 0) return 1

        val K = 20.0   // difficulty scaling: larger K = harder leveling
        val level = sqrt(points / K)

        return level.toInt().coerceAtLeast(1)
    }


    fun pointsForLevel(level: Int): Int {
        if (level <= 1) return 0
        var p = 0
        while (levelForPoints(p) < level) {
            p++
        }
        return p
    }

}
