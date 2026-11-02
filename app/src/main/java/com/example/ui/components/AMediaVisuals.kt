package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AMediaBlueTick
import com.example.ui.theme.AMediaCoral
import com.example.ui.theme.AMediaCyan
import com.example.ui.theme.AMediaEmerald
import com.example.ui.theme.AMediaGold
import com.example.ui.theme.AMediaViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VisualPresetOption(
  val key: String,
  val label: String,
  val subtitle: String,
)

val POST_VISUAL_PRESETS = listOf(
  VisualPresetOption("preset_cyber_city", "Shinjuku Neon Rain", "Blue Hour 35mm Street"),
  VisualPresetOption("preset_golden_coast", "Amalfi Cliffside", "Golden Hour Mediterranean"),
  VisualPresetOption("preset_aurora_synth", "Aurora Modular Studio", "Analog Synth & Neon Glow"),
  VisualPresetOption("preset_velvet_dune", "Sahara Twilight Dunes", "Minimalist Desert Horizon"),
  VisualPresetOption("preset_tokyo_bloom", "Kyoto Sakura Mist", "Editorial Film Portrait"),
)

val AVATAR_PRESETS = listOf(
  "avatar_aurora" to "Aurora Violet",
  "avatar_sunset" to "Coral Sunset",
  "avatar_neon" to "Cyber Cyan",
  "avatar_ocean" to "Deep Ocean",
  "avatar_emerald" to "Emerald Glow",
)

@Composable
fun BlueVerifiedBadge(
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
) {
  Icon(
    imageVector = Icons.Filled.Verified,
    contentDescription = "Verified Blue Tick Badge",
    tint = AMediaBlueTick,
    modifier = modifier.size(size),
  )
}

@Composable
fun UserAvatar(
  avatarKey: String,
  displayName: String,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  hasStoryRing: Boolean = false,
  isStoryViewed: Boolean = false,
) {
  val ringBrush = when {
    !hasStoryRing -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    isStoryViewed -> Brush.linearGradient(
      listOf(Color.Gray.copy(alpha = 0.45f), Color.Gray.copy(alpha = 0.45f))
    )
    else -> Brush.linearGradient(
      listOf(AMediaCoral, AMediaGold, AMediaViolet, AMediaCyan)
    )
  }

  val avatarBrush = when (avatarKey) {
    "avatar_sunset" -> Brush.linearGradient(listOf(Color(0xFFFF2E63), Color(0xFFFF8E53)))
    "avatar_neon" -> Brush.linearGradient(listOf(Color(0xFF08D9D6), Color(0xFF3A86FF)))
    "avatar_ocean" -> Brush.linearGradient(listOf(Color(0xFF0077B6), Color(0xFF00F5D4)))
    "avatar_emerald" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    else -> Brush.linearGradient(listOf(Color(0xFF7B2CBF), Color(0xFFFF2E63)))
  }

  val initials = displayName.trim()
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "AM" }

  Box(
    modifier = modifier
      .size(size)
      .then(
        if (hasStoryRing) {
          Modifier
            .border(width = 2.5.dp, brush = ringBrush, shape = CircleShape)
            .padding(3.dp)
        } else {
          Modifier
        }
      )
      .clip(CircleShape)
      .background(avatarBrush),
    contentAlignment = Alignment.Center,
  ) {
    if (avatarKey.startsWith("content://") || avatarKey.startsWith("file://") || avatarKey.startsWith("http")) {
      AsyncImage(
        model = Uri.parse(avatarKey),
        contentDescription = "$displayName profile photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(
        text = initials,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = (size.value * 0.36f).sp,
      )
    }
  }
}

@Composable
fun AMediaPostVisual(
  imageUrl: String,
  caption: String,
  modifier: Modifier = Modifier,
) {
  if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://") || imageUrl.startsWith("http")) {
    Box(modifier = modifier.background(Color(0xFF12131F))) {
      AsyncImage(
        model = Uri.parse(imageUrl),
        contentDescription = caption.ifBlank { "AMedia photo" },
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
    return
  }

  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      when (imageUrl) {
        "preset_golden_coast" -> {
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFFFF9E00), Color(0xFFFF5400), Color(0xFF0077B6), Color(0xFF03045E))
            )
          )
          // Glowing sun
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Color(0xFFFFEA00), Color(0xFFFF9E00).copy(alpha = 0f)),
              center = Offset(w * 0.72f, h * 0.32f),
              radius = w * 0.28f,
            ),
            radius = w * 0.28f,
            center = Offset(w * 0.72f, h * 0.32f),
          )
          drawCircle(
            color = Color(0xFFFFF3B0),
            radius = w * 0.11f,
            center = Offset(w * 0.72f, h * 0.32f),
          )
          // Coastal cliffs & sea reflections
          val cliffPath = Path().apply {
            moveTo(0f, h * 0.55f)
            quadraticTo(w * 0.28f, h * 0.48f, w * 0.48f, h * 0.68f)
            lineTo(0f, h * 0.75f)
            close()
          }
          drawPath(cliffPath, color = Color(0xFF2B193D))
          for (i in 0..5) {
            val y = h * (0.66f + i * 0.045f)
            drawLine(
              color = Color(0xFFFFEA00).copy(alpha = 0.45f - i * 0.06f),
              start = Offset(w * (0.55f - i * 0.03f), y),
              end = Offset(w * (0.85f + i * 0.02f), y),
              strokeWidth = 4.dp.toPx(),
            )
          }
        }

        "preset_aurora_synth" -> {
          drawRect(
            brush = Brush.linearGradient(
              colors = listOf(Color(0xFF060913), Color(0xFF0F1E36), Color(0xFF1B4332)),
              start = Offset.Zero,
              end = Offset(w, h),
            )
          )
          // Aurora Borealis waves
          for (i in 0..3) {
            val wavePath = Path().apply {
              moveTo(0f, h * (0.28f + i * 0.1f))
              cubicTo(
                w * 0.3f, h * (0.12f + i * 0.08f),
                w * 0.65f, h * (0.48f + i * 0.06f),
                w, h * (0.22f + i * 0.1f)
              )
            }
            drawPath(
              path = wavePath,
              brush = Brush.horizontalGradient(
                listOf(AMediaEmerald.copy(alpha = 0.55f), AMediaCyan.copy(alpha = 0.65f), AMediaViolet.copy(alpha = 0.5f))
              ),
              style = Stroke(width = (22 - i * 4).dp.toPx()),
            )
          }
          // Synth equalizer bars at bottom
          val barCount = 18
          val barWidth = w / (barCount * 1.8f)
          for (i in 0 until barCount) {
            val barH = h * (0.12f + ((i * 7) % 11) * 0.025f)
            val x = w * 0.08f + i * (barWidth * 1.65f)
            drawRoundRect(
              brush = Brush.verticalGradient(listOf(AMediaCyan, AMediaEmerald)),
              topLeft = Offset(x, h * 0.85f - barH),
              size = Size(barWidth, barH),
              cornerRadius = CornerRadius(6f, 6f),
            )
          }
        }

        "preset_velvet_dune" -> {
          drawRect(
            brush = Brush.verticalGradient(
              listOf(Color(0xFF240046), Color(0xFF5A189A), Color(0xFFFF6D00), Color(0xFF3C096C))
            )
          )
          drawCircle(
            color = Color(0xFFFF9E00).copy(alpha = 0.85f),
            radius = w * 0.16f,
            center = Offset(w * 0.35f, h * 0.34f),
          )
          val dune1 = Path().apply {
            moveTo(0f, h * 0.68f)
            quadraticTo(w * 0.35f, h * 0.52f, w, h * 0.74f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(dune1, color = Color(0xFF3C096C))
          val dune2 = Path().apply {
            moveTo(0f, h * 0.78f)
            quadraticTo(w * 0.65f, h * 0.62f, w, h * 0.82f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
          }
          drawPath(dune2, color = Color(0xFF10002B))
        }

        "preset_tokyo_bloom" -> {
          drawRect(
            brush = Brush.linearGradient(
              listOf(Color(0xFF2B1B3D), Color(0xFF6D2E46), Color(0xFFFF758F), Color(0xFFFFB3C1))
            )
          )
          for (i in 0..8) {
            val cx = w * ((i * 19 + 13) % 90) / 100f
            val cy = h * ((i * 23 + 17) % 85) / 100f
            drawCircle(
              color = Color.White.copy(alpha = 0.25f),
              radius = (14 + (i % 4) * 10).dp.toPx(),
              center = Offset(cx, cy),
            )
          }
        }

        else -> {
          // Default: preset_cyber_city (Shinjuku Neon Rain)
          drawRect(
            brush = Brush.verticalGradient(
              listOf(Color(0xFF090A14), Color(0xFF1A103C), Color(0xFF3B185F), Color(0xFF0F172A))
            )
          )
          // Neon halo moon
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(AMediaCoral.copy(alpha = 0.65f), Color.Transparent),
              center = Offset(w * 0.75f, h * 0.26f),
              radius = w * 0.32f,
            ),
            radius = w * 0.32f,
            center = Offset(w * 0.75f, h * 0.26f),
          )
          // Cityscape skyline silhouettes
          val heights = listOf(0.45f, 0.62f, 0.38f, 0.72f, 0.54f, 0.66f, 0.48f, 0.58f)
          val bw = w / heights.size
          heights.forEachIndexed { idx, ratio ->
            val bh = h * ratio
            val left = idx * bw + 4f
            drawRect(
              color = Color(0xFF0B0D19),
              topLeft = Offset(left, h - bh),
              size = Size(bw - 8f, bh),
            )
            // Neon vertical sign strips
            val neonColor = if (idx % 2 == 0) AMediaCyan else AMediaCoral
            drawRect(
              color = neonColor.copy(alpha = 0.75f),
              topLeft = Offset(left + bw * 0.35f, h - bh + 18f),
              size = Size(4.dp.toPx(), bh * 0.45f),
            )
          }
        }
      }
    }

    // Subtle editorial badge in top-left
    val presetTitle = POST_VISUAL_PRESETS.find { it.key == imageUrl }?.label ?: "AMedia Original"
    Surface(
      color = Color.Black.copy(alpha = 0.45f),
      shape = RoundedCornerShape(50),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(12.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(AMediaCoral)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = presetTitle,
          color = Color.White,
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
  }
}

fun formatRelativeTimestamp(timestampSeconds: Long?): String {
  if (timestampSeconds == null || timestampSeconds <= 0L) return "Just now"
  val nowSec = System.currentTimeMillis() / 1000L
  val diff = (nowSec - timestampSeconds).coerceAtLeast(0L)
  return when {
    diff < 60L -> "Just now"
    diff < 3600L -> "${diff / 60L}m ago"
    diff < 86400L -> "${diff / 3600L}h ago"
    diff < 604800L -> "${diff / 86400L}d ago"
    else -> {
      val sdf = SimpleDateFormat("MMM d", Locale.US)
      sdf.format(Date(timestampSeconds * 1000L))
    }
  }
}
