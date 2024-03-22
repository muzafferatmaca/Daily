package com.muzafferatmaca.daily.navigation

import com.muzafferatmaca.daily.util.Constants.WRITE_SCREEN_ARGUMENT_KEY

/**
 * Created by Muzaffer Atmaca on 13.03.2024 at 17:09
 */
sealed class Screen(val route: String) {
    object Authentication : Screen("authentication_screen")
    object Home : Screen("home_screen")
    object Write : Screen("write_screen?$WRITE_SCREEN_ARGUMENT_KEY={$WRITE_SCREEN_ARGUMENT_KEY}") {
        fun passDailyId(dailyId: String) = "write_screen?$WRITE_SCREEN_ARGUMENT_KEY=$dailyId"
    }


    //arg gönderme yöntemi extra
    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { args ->
                append("/$args")
            }
        }
    }
}