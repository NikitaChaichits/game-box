package com.boxgame

import com.orhanobut.hawk.Hawk

const val KEY_BEST_TIME = "best_time"

fun saveBestTime(time: Float) {
    Hawk.put(KEY_BEST_TIME, time)
}

fun getBestTime(): Float {
    return Hawk.get(KEY_BEST_TIME, 0f)
}
