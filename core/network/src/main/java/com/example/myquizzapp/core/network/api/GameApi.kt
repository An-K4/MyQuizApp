package com.example.myquizzapp.core.network.api

import com.example.myquizzapp.core.common.result.Result
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET

interface GameApi {
    @GET("games/game-modes")   // → GET https://api.myquizz.dpdns.org/v1/games/game-modes
    suspend fun getGameModes(): Result<JsonElement>  // smoke test; model đẹp làm ở N17
}