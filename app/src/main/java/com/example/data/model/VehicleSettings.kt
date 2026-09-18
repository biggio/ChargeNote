package com.example.data.model

data class VehicleSettings(
    val vehicleName: String = "Luxgen N7 亮點版7人",
    val usableBatteryKwh: Double = 57.7,
    val officialRangeKm: Double = 300.0,
    val ratingStandard: String = "NEDC", // "NEDC", "WLTP", "EPA", "CLTC"
    val distanceUnit: String = "km",
    val currencyUnit: String = "TWD"
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccentTheme(val displayName: String, val primaryHex: Long, val secondaryHex: Long) {
    GEOMETRIC_TEAL("幾何平衡 墨綠青 (Geometric Teal)", 0xFF006A6A, 0xFF008282),
    TECH_BLUE("科技電藍 (Tech Blue)", 0xFF00629E, 0xFF38BDF8),
    ECO_MINT("節能翠綠 (Eco Mint)", 0xFF006D3B, 0xFF10B981),
    AMBER_SUNSET("曜金琥珀 (Amber Sunset)", 0xFF944B00, 0xFFF59E0B),
    NEON_VIOLET("極光魅紫 (Neon Violet)", 0xFF6B4FA2, 0xFF8B5CF6),
    GRAPHITE_DARK("洗鍊炭黑 (Graphite Dark)", 0xFF40484C, 0xFF64748B)
}

data class AppThemeConfig(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentTheme: AccentTheme = AccentTheme.GEOMETRIC_TEAL
)
