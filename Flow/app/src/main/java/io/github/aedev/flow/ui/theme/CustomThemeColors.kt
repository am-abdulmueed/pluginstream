package io.github.aedev.flow.ui.theme

enum class CustomColorRole {
    PRIMARY,
    ON_PRIMARY,
    SECONDARY,
    ON_SECONDARY,
    TERTIARY,
    ON_TERTIARY,
    BACKGROUND,
    ON_BACKGROUND,
    SURFACE,
    ON_SURFACE,
    SURFACE_VARIANT,
    ON_SURFACE_VARIANT,
    ERROR,
    ON_ERROR,
    OUTLINE,
    SCRIM
}

data class CustomThemeColors(
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val error: Long,
    val onError: Long,
    val outline: Long,
    val scrim: Long
) {
    fun colorOf(role: CustomColorRole): Long {
        return when (role) {
            CustomColorRole.PRIMARY -> primary
            CustomColorRole.ON_PRIMARY -> onPrimary
            CustomColorRole.SECONDARY -> secondary
            CustomColorRole.ON_SECONDARY -> onSecondary
            CustomColorRole.TERTIARY -> tertiary
            CustomColorRole.ON_TERTIARY -> onTertiary
            CustomColorRole.BACKGROUND -> background
            CustomColorRole.ON_BACKGROUND -> onBackground
            CustomColorRole.SURFACE -> surface
            CustomColorRole.ON_SURFACE -> onSurface
            CustomColorRole.SURFACE_VARIANT -> surfaceVariant
            CustomColorRole.ON_SURFACE_VARIANT -> onSurfaceVariant
            CustomColorRole.ERROR -> error
            CustomColorRole.ON_ERROR -> onError
            CustomColorRole.OUTLINE -> outline
            CustomColorRole.SCRIM -> scrim
        }
    }

    fun withColor(role: CustomColorRole, argb: Long): CustomThemeColors {
        return when (role) {
            CustomColorRole.PRIMARY -> copy(primary = argb)
            CustomColorRole.ON_PRIMARY -> copy(onPrimary = argb)
            CustomColorRole.SECONDARY -> copy(secondary = argb)
            CustomColorRole.ON_SECONDARY -> copy(onSecondary = argb)
            CustomColorRole.TERTIARY -> copy(tertiary = argb)
            CustomColorRole.ON_TERTIARY -> copy(onTertiary = argb)
            CustomColorRole.BACKGROUND -> copy(background = argb)
            CustomColorRole.ON_BACKGROUND -> copy(onBackground = argb)
            CustomColorRole.SURFACE -> copy(surface = argb)
            CustomColorRole.ON_SURFACE -> copy(onSurface = argb)
            CustomColorRole.SURFACE_VARIANT -> copy(surfaceVariant = argb)
            CustomColorRole.ON_SURFACE_VARIANT -> copy(onSurfaceVariant = argb)
            CustomColorRole.ERROR -> copy(error = argb)
            CustomColorRole.ON_ERROR -> copy(onError = argb)
            CustomColorRole.OUTLINE -> copy(outline = argb)
            CustomColorRole.SCRIM -> copy(scrim = argb)
        }
    }

    companion object {
        fun default(): CustomThemeColors {
            return CustomThemeColors(
                primary = 0xFF82B1FF,
                onPrimary = 0xFF0A1E3D,
                secondary = 0xFFB39DDB,
                onSecondary = 0xFF180B2D,
                tertiary = 0xFF80CBC4,
                onTertiary = 0xFF062421,
                background = 0xFF11131A,
                onBackground = 0xFFE6E8EF,
                surface = 0xFF1A1D26,
                onSurface = 0xFFE6E8EF,
                surfaceVariant = 0xFF262A35,
                onSurfaceVariant = 0xFFC2C8D6,
                error = 0xFFFF6B6B,
                onError = 0xFF2C0000,
                outline = 0xFF3C4355,
                scrim = 0xCC000000
            )
        }
    }
}