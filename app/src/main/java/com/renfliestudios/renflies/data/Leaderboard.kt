package com.renfliestudios.renflies.data

/**
 * A fully local, mock leaderboard. The fictional entries are hardcoded; the
 * player's best score is merged in and the combined list is ranked.
 * No networking or backend is involved anywhere.
 */
object Leaderboard {

    data class Entry(val name: String, val score: Int, val isPlayer: Boolean = false)

    private val MOCK_ENTRIES = listOf(
        Entry("SkyMaster", 12450),
        Entry("WingLegend", 10870),
        Entry("FlapKing", 9320),
        Entry("BirdDestroyer", 8710),
        Entry("CloudRunner", 7640),
        Entry("FeatherStorm", 6120),
        Entry("BeakBlitz", 4980),
        Entry("GaleGlider", 3540),
        Entry("PipeDodger", 2210),
        Entry("TinyTalon", 1180)
    )

    /** The mock leaderboard including the local player's best score, ranked. */
    fun entries(playerName: String = "You", playerBestScore: Int): List<Entry> {
        val all = MOCK_ENTRIES + Entry(playerName, playerBestScore, isPlayer = true)
        return all.sortedByDescending { it.score }
    }
}
