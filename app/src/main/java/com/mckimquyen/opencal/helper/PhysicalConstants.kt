package com.mckimquyen.opencal.helper

/**
 * N-CALC-4: hằng số vật lý chèn vào input, thay bằng giá trị số ngay tại
 * [Expression.getCleanExpression] (cùng cơ chế string-substitution đã dùng cho "ans") — không
 * đụng tokenizer đệ quy xuống trong `Calculator.kt`.
 *
 * [token] vừa là text hiển thị trong input vừa là khoá tìm-thay. Phải chọn token KHÔNG PHẢI
 * substring của bất kỳ từ khoá có sẵn nào ("sin","cos","tan","ln","logten","xp","arcco","arcsi",
 * "arcta","ans","π","e") — nếu không, .replace() thô sẽ phá hỏng những từ khoá đó. Ký hiệu tốc độ
 * ánh sáng dùng chữ "c" nghiêng Unicode (U+1D450) thay vì "c" ASCII thường vì "c" ASCII là
 * substring của "cos"/"arcco".
 */
data class PhysicalConstant(
    val token: String,
    val label: String,
    val value: Double,
)

object PhysicalConstants {
    val ALL = listOf(
        PhysicalConstant("𝑐", "Speed of light (c) = 299792458 m/s", 299792458.0),
        PhysicalConstant("h", "Planck constant (h) = 6.62607015×10⁻³⁴ J·s", 6.62607015E-34),
        PhysicalConstant("k", "Boltzmann constant (k) = 1.380649×10⁻²³ J/K", 1.380649E-23),
        PhysicalConstant("G", "Gravitational constant (G) = 6.6743×10⁻¹¹ N·m²/kg²", 6.6743E-11),
        PhysicalConstant("NA", "Avogadro constant (Nₐ) = 6.02214076×10²³ mol⁻¹", 6.02214076E23),
    )
}
