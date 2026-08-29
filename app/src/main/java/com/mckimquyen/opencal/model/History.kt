package com.mckimquyen.opencal.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class History(
    @SerializedName("calculation") val calculation: String?,
    @SerializedName("result") val result: String?,
    @SerializedName("time") val time: String?,
    // N-DATA-4: entry cũ (JSON không có field này) qua Gson sẽ nhận default JVM của Boolean = false,
    // tức "chưa ghim" — không cần migration riêng.
    @SerializedName("isPinned") val isPinned: Boolean = false,
) : Serializable
