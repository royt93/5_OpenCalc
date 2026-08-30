package com.mckimquyen.opencal.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// F-DATA-8: ponytail — không thêm field version/migration framework. Pattern hiện tại (field mới
// = nullable hoặc có default, JSON cũ thiếu field thì Gson tự điền default JVM, xem ví dụ
// `isPinned` bên dưới) đã đủ cho quy mô model 4 field này. Thêm version tracking là hạ tầng
// speculative cho một schema đơn giản chưa từng cần rollback/branch migration thật sự.
data class History(
    @SerializedName("calculation") val calculation: String?,
    @SerializedName("result") val result: String?,
    @SerializedName("time") val time: String?,
    // N-DATA-4: entry cũ (JSON không có field này) qua Gson sẽ nhận default JVM của Boolean = false,
    // tức "chưa ghim" — không cần migration riêng.
    @SerializedName("isPinned") val isPinned: Boolean = false,
) : Serializable
