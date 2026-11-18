package app.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Claude-inspired dark palette: warm charcoal surfaces, cream text, clay-orange accent.
val ClayOrange = Color(0xFFD97757)
val Charcoal = Color(0xFF262624)
val CharcoalDeep = Color(0xFF1F1E1D)
val Card = Color(0xFF30302E)
val CardHigh = Color(0xFF3A3A37)
val Cream = Color(0xFFF5F4EE)
val Muted = Color(0xFFA9A69C)
val Line = Color(0xFF45443F)
val Sage = Color(0xFF8FBF8A)

private val Colors = darkColorScheme(
    primary = ClayOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4D3026),
    onPrimaryContainer = Color(0xFFF4CDBD),
    secondary = Color(0xFFC9C6BB),
    onSecondary = Charcoal,
    secondaryContainer = CardHigh,
    onSecondaryContainer = Cream,
    tertiary = Sage,
    onTertiary = Charcoal,
    tertiaryContainer = Color(0xFF2E3A2C),
    onTertiaryContainer = Color(0xFFCDE6C9),
    background = Charcoal,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = CardHigh,
    onSurfaceVariant = Muted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = CharcoalDeep,
    surfaceContainerLow = Color(0xFF2B2B29),
    surfaceContainer = Card,
    surfaceContainerHigh = CardHigh,
    surfaceContainerHighest = Color(0xFF42413E),
    surfaceBright = Color(0xFF42413E),
    surfaceDim = CharcoalDeep,
    outline = Line,
    outlineVariant = Color(0xFF3A3936),
    error = Color(0xFFE5806B),
    onError = Charcoal,
    errorContainer = Color(0xFF4F2A22),
    onErrorContainer = Color(0xFFF7C9BD),
    inverseSurface = Cream,
    inverseOnSurface = Charcoal,
    inversePrimary = Color(0xFF9C4A30),
    scrim = Color.Black,
)

private val Serif = FontFamily.Serif
private val base = Typography()

private val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = Serif),
    headlineLarge = base.headlineLarge.copy(fontFamily = Serif),
    headlineMedium = base.headlineMedium.copy(fontFamily = Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = Serif),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
