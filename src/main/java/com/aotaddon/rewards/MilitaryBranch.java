package com.aotaddon.rewards;

/**
 * TODO: this almost certainly already exists in some form in your military
 * rank system (Scouts / Garrison / MP / Warriors / Marley Military). Delete
 * this enum and point TitanKillRewardHandler at your real source instead —
 * this is just here so the reward-branching logic below compiles standalone.
 */
public enum MilitaryBranch {
    SCOUTS,
    GARRISON,
    MILITARY_POLICE,
    NONE // Warriors, Marley Military, or no branch assigned
}
