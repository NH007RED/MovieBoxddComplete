package com.moviebox.app.ui.user

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.moviebox.app.ui.components.MovieBoxLogo
import com.moviebox.app.ui.theme.CrimsonDark
import com.moviebox.app.ui.theme.CrimsonLight
import com.moviebox.app.ui.theme.CrimsonRed
import com.moviebox.app.ui.theme.TextMuted
import com.moviebox.app.ui.theme.TextSecondary
import com.moviebox.app.ui.theme.TextWhite
import kotlinx.coroutines.delay

private const val SPLASH_PROGRESS_MS = 2400
private const val SPLASH_HOLD_MS = 250L

private val splashStatusMessages = listOf(
    "Connecting to cinema library…",
    "Loading Indian & Global posters…",
    "Syncing movies & web series…",
    "Almost ready — enjoy the show!"
)

@Composable
fun SplashScreen(
    indianPosterUrls: List<String> = emptyList(),
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var startAnim by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.82f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(750),
        label = "splashAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splashInfinite")
    // Parallax drift: neighbouring poster columns move in opposite directions
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )
    // Soft breathing glow behind the brand card
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    // Moving highlight that sweeps along the progress bar
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val postersToDisplay = remember(indianPosterUrls) {
        if (indianPosterUrls.size >= 12) indianPosterUrls.take(12)
        else {
            val base = indianPosterUrls.ifEmpty {
                listOf(
                    "https://image.tmdb.org/t/p/w500/nEufeZlyAOLqO2brrs0yeF1lgXO.jpg",
                    "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                    "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                    "https://image.tmdb.org/t/p/w500/rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg"
                )
            }
            List(12) { idx -> base[idx % base.size] }
        }
    }

    LaunchedEffect(Unit) {
        startAnim = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SPLASH_PROGRESS_MS, easing = FastOutSlowInEasing)
        )
        delay(SPLASH_HOLD_MS)
        onFinished()
    }

    val bgColor = MaterialTheme.colorScheme.background
    val progressValue = progress.value
    val statusIndex = when {
        progressValue < 0.28f -> 0
        progressValue < 0.58f -> 1
        progressValue < 0.88f -> 2
        else -> 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // TMDB Indian Movies Poster Wall Collage in Background (staggered fade-in + parallax columns)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .alpha(0.45f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (rowIdx in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (colIdx in 0 until 3) {
                        val posterIdx = (rowIdx * 3 + colIdx) % postersToDisplay.size
                        val url = postersToDisplay[posterIdx]
                        val direction = if (colIdx % 2 == 0) 1f else -1f
                        SplashPosterTile(
                            url = url,
                            visible = startAnim,
                            order = rowIdx * 3 + colIdx,
                            driftDp = driftOffset * direction,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Cinematic Dark Vignette Gradient Overlay over Indian Movie Posters
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to bgColor.copy(alpha = 0.72f),
                        0.45f to bgColor.copy(alpha = 0.58f),
                        0.85f to bgColor.copy(alpha = 0.92f),
                        1.0f to bgColor
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CrimsonRed.copy(alpha = 0.24f * glowPulse + 0.06f),
                            Color.Transparent
                        ),
                        radius = 850f
                    )
                )
        )

        // Foreground Animated Brand Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .padding(24.dp)
        ) {
            Surface(
                color = Color(0xFF10131C).copy(alpha = 0.82f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                CrimsonRed.copy(alpha = 0.30f + 0.25f * glowPulse),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    MovieBoxLogo(
                        iconSize = 56.dp,
                        fontSizeSp = 34
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Indian & Global Cinema • Web Series",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Powered by Live TMDB Discovery",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(24.dp))

                    // Animated gradient progress bar with moving shimmer highlight
                    SplashProgressBar(
                        progress = progressValue,
                        shimmerX = shimmerX,
                        modifier = Modifier
                            .width(220.dp)
                            .height(6.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.width(220.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Crossfade(
                            targetState = statusIndex,
                            animationSpec = tween(350),
                            label = "splashStatus",
                            modifier = Modifier.weight(1f)
                        ) { idx ->
                            Text(
                                text = splashStatusMessages[idx],
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${(progressValue * 100).toInt().coerceIn(0, 100)}%",
                            color = TextWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Footer: three pulsing dots, sits above the transparent navigation bar
        SplashFooterDots(
            alphaValue = alpha,
            pulse = glowPulse
        )
    }
}

@Composable
private fun SplashPosterTile(
    url: String,
    visible: Boolean,
    order: Int,
    driftDp: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tileAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, delayMillis = order * 70),
        label = "tileAlpha$order"
    )
    val tileScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(durationMillis = 520, delayMillis = order * 70, easing = FastOutSlowInEasing),
        label = "tileScale$order"
    )
    Box(
        modifier = modifier
            .offset(y = driftDp.dp)
            .scale(tileScale)
            .alpha(tileAlpha)
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151924))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SplashProgressBar(
    progress: Float,
    shimmerX: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped.coerceIn(0.02f, 1f))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(CrimsonDark, CrimsonRed, CrimsonLight)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            (shimmerX - 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                            shimmerX.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.55f),
                            (shimmerX + 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
            )
        }
    }
}

@Composable
private fun BoxScope.SplashFooterDots(alphaValue: Float, pulse: Float) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 28.dp)
            .alpha(alphaValue),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 3) {
            val dotAlpha = when (i) {
                0 -> pulse
                1 -> 1.55f - pulse
                else -> pulse * 0.8f + 0.2f
            }.coerceIn(0.25f, 1f)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background((if (i == 1) CrimsonRed else TextMuted).copy(alpha = dotAlpha))
            )
        }
    }
}
