package com.renfliestudios.renflies.data

import com.renfliestudios.renflies.game.PowerUpType

/**
 * The mock/local battle pass. Pure Kotlin so it is fully unit testable.
 *
 * XP is earned through gameplay:
 *  - +1 XP per obstacle passed (score)
 *  - +250 XP per defeated boss
 *  - +50 XP run completion bonus
 *
 * Level thresholds are cumulative XP required to REACH that level
 * (level 1 starts at 0 XP). Progress persists locally via [ProgressStore].
 *
 * Rewards come in three unlock types: SKINS, EMOTES and SINGLE-USE
 * CONSUMABLES (the latter balance the brutal Devilish difficulty).
 */
object BattlePass {

    const val BOSS_XP: Int = 250
    const val RUN_BONUS_XP: Int = 50

    enum class RewardType { SKIN, EMOTE, CONSUMABLE }

    data class Reward(
        val name: String,
        val type: RewardType,
        val iconType: RewardIcon,
        /** Set when this reward grants single-use consumable charges. */
        val consumable: PowerUpType? = null
    )

    enum class RewardIcon { BIRD, EMOTE, SHIELD, SPEED, BERSERKER, BADGE }

    data class LevelInfo(
        val level: Int,
        val xpRequired: Int,       // cumulative XP to reach this level
        val reward: Reward
    )

    val LEVELS: List<LevelInfo> = buildList {
        var xp = 0
        val rewards = listOf(
            Reward("Classic Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Wave Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Shield Charge", RewardType.CONSUMABLE, RewardIcon.SHIELD, PowerUpType.SHIELD),
            Reward("Crimson Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Flap Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Speed Charge", RewardType.CONSUMABLE, RewardIcon.SPEED, PowerUpType.SPEED_BOOST),
            Reward("Midnight Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Spin Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Berserker Charge", RewardType.CONSUMABLE, RewardIcon.BERSERKER, PowerUpType.BERSERKER),
            Reward("Golden Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Taunt Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Shield Charge", RewardType.CONSUMABLE, RewardIcon.SHIELD, PowerUpType.SHIELD),
            Reward("Neon Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Victory Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Speed Charge", RewardType.CONSUMABLE, RewardIcon.SPEED, PowerUpType.SPEED_BOOST),
            Reward("Phoenix Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Flex Emote", RewardType.EMOTE, RewardIcon.EMOTE),
            Reward("Berserker Charge", RewardType.CONSUMABLE, RewardIcon.BERSERKER, PowerUpType.BERSERKER),
            Reward("Platinum Bird Skin", RewardType.SKIN, RewardIcon.BIRD),
            Reward("Legend Badge Skin", RewardType.SKIN, RewardIcon.BADGE)
        )
        val steps = listOf(100, 150, 250, 300, 400, 500, 500, 600, 700, 900,
            1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800)
        add(LevelInfo(1, 0, rewards[0]))
        for (i in 1 until rewards.size) {
            xp += steps[i - 1]
            add(LevelInfo(i + 1, xp, rewards[i]))
        }
    }

    val MAX_LEVEL: Int get() = LEVELS.size

    /** XP awarded for finishing a run. */
    fun xpForRun(score: Int, bossesDefeated: Int): Int =
        score + bossesDefeated * BOSS_XP + RUN_BONUS_XP

    /** Cumulative XP needed to reach [level]. */
    fun xpForLevel(level: Int): Int =
        LEVELS.getOrElse(level - 1) { LEVELS.last() }.xpRequired

    /** The level a given amount of total XP corresponds to. */
    fun levelForXp(totalXp: Int): Int {
        var level = 1
        for (info in LEVELS) {
            if (totalXp >= info.xpRequired) level = info.level else break
        }
        return level
    }

    /** XP progress details for the UI. */
    data class Progress(
        val level: Int,
        val totalXp: Int,
        val xpIntoLevel: Int,
        val xpForNextLevel: Int,   // amount needed WITHIN this level (0 at max)
        val fraction: Float,
        val isMaxLevel: Boolean
    )

    fun progressFor(totalXp: Int): Progress {
        val level = levelForXp(totalXp)
        if (level >= MAX_LEVEL) {
            return Progress(level, totalXp, 0, 0, 1f, true)
        }
        val currentThreshold = xpForLevel(level)
        val nextThreshold = xpForLevel(level + 1)
        val into = totalXp - currentThreshold
        val needed = nextThreshold - currentThreshold
        return Progress(
            level = level,
            totalXp = totalXp,
            xpIntoLevel = into,
            xpForNextLevel = needed,
            fraction = into.toFloat() / needed,
            isMaxLevel = false
        )
    }
}
