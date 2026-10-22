package android.kma.myquizzapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Display values only. Authorization, rank and score stay with the caller. */
data class FinalResultPodiumEntry(val rank: Int, val name: String, val scoreText: String)
data class FinalResultMetric(val label: String, val value: String)

/** Stateless finish header. No game model, ViewModel, clock, platform action or navigation. */
@Composable
fun GameFinalResultHero(
    entries: List<FinalResultPodiumEntry>,
    modifier: Modifier = Modifier,
    title: String = "Kết quả chung cuộc",
    subtitle: String = "Bạn đã hoàn thành trận đấu",
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🏆", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clearAndSetSemantics { })
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(subtitle, color = Color(0xFFE6DDF5), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (entries.isNotEmpty()) {
            // Keep the podium at every width/font scale. Only real entries share the row.
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                // Reorder only a real, unambiguous 1/2/3 podium. Never create a missing rank.
                val ordered = if (entries.map { it.rank } == listOf(1, 2, 3))
                    listOf(entries[1], entries[0], entries[2]) else entries
                Row(
                    Modifier.widthIn(max = (entries.size * 120).dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    ordered.forEach { entry -> PodiumEntry(entry, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PodiumEntry(entry: FinalResultPodiumEntry, modifier: Modifier) {
    val tint = when (entry.rank) {
        1 -> Color(0xFFFFDE88)
        2 -> Color(0xFFD8DEEF)
        else -> Color(0xFFF0B69C)
    }
    Column(modifier.clearAndSetSemantics {
        contentDescription = "Hạng ${entry.rank}, ${entry.name}, ${entry.scoreText}"
    }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entry.rank == 1) Text("♛", color = Color(0xFFFFDE88), style = MaterialTheme.typography.headlineSmall)
        Surface(Modifier.size(if (entry.rank == 1) 58.dp else 48.dp), shape = CircleShape, color = Color(0xFFF3EEFF)) {
            Box(contentAlignment = Alignment.Center) {
                val initials = entry.name.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
                    .take(2).joinToString("") { it.take(1).uppercase() }
                Text(initials, color = Color(0xFF543294), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Text(entry.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(entry.scoreText, color = Color(0xFFE6DDF5), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(when (entry.rank) { 1 -> 116.dp; 2 -> 90.dp; else -> 72.dp })
            .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.72f))),
                RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)), contentAlignment = Alignment.Center) {
            Text("${entry.rank}", color = Color(0xFF352640), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Reusable metric card: the caller decides which facts exist and may be shown. */
@Composable
fun GameFinalResultSummaryCard(
    title: String,
    metrics: List<FinalResultMetric>,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp)) {
                    val textMeasurer = rememberTextMeasurer()
                    val valueStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    val labelStyle = MaterialTheme.typography.bodyMedium
                    val gap = 8.dp
                    val columnWidthPx = with(LocalDensity.current) {
                        ((maxWidth - gap * (metrics.size - 1).coerceAtLeast(0)) /
                            metrics.size.coerceAtLeast(1).toFloat()).toPx()
                    }.toInt().coerceAtLeast(0)
                    // Measure the same styles rendered below. Ordinary phones keep horizontal
                    // metrics; only actual text overflow (including accessibility scaling) stacks them.
                    val compact = metrics.any { metric ->
                        textMeasurer.measure(metric.value, valueStyle, softWrap = false, maxLines = 1)
                            .size.width > columnWidthPx ||
                            textMeasurer.measure(metric.label, labelStyle, softWrap = false, maxLines = 1)
                                .size.width > columnWidthPx
                    }
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            metrics.forEach { metric ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(metric.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(metric.value, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            metrics.forEach { metric ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(metric.value, style = valueStyle, textAlign = TextAlign.Center)
                                    Text(metric.label, style = labelStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Display-only fixtures for checking width/font-scale breakpoints without a ViewModel.
@Preview(name = "Final layout · 320dp", widthDp = 320, heightDp = 1000)
@Preview(name = "Final layout · 360dp · font 1.15", widthDp = 360, heightDp = 1000, fontScale = 1.15f)
@Preview(name = "Final layout · 390dp", widthDp = 390, heightDp = 1000)
@Preview(name = "Final layout · 320dp · font 1.5", widthDp = 320, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun FinalResultResponsivePreview() {
    FinalResultComponentsPreview(listOf(
        FinalResultPodiumEntry(1, "Minh Anh", "12.450 điểm"),
        FinalResultPodiumEntry(2, "Quang Huy", "11.200 điểm"),
        FinalResultPodiumEntry(3, "Tuấn Kiệt", "10.850 điểm")
    ))
}

@Preview(name = "Final layout · One player · 360dp", widthDp = 360, heightDp = 1000, fontScale = 1.15f)
@Composable
private fun FinalResultSinglePlayerPreview() {
    FinalResultComponentsPreview(listOf(FinalResultPodiumEntry(1, "Kiro number one", "2.938 điểm")))
}

@Composable
private fun FinalResultComponentsPreview(entries: List<FinalResultPodiumEntry>) {
    MyQuizAppTheme(darkTheme = false) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF34205F)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            GameFinalResultHero(entries)
            GameFinalResultSummaryCard("Thành tích của bạn (Kiro number one)", listOf(
                FinalResultMetric("Hạng", "#1"),
                FinalResultMetric("Điểm", "2.938"),
                FinalResultMetric("Câu đúng", "2")
            ))
        }
    }
}
