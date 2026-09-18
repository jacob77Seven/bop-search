package com.jacob77.bopsearch.ui.mixes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacob77.bopsearch.data.MixEntity
import com.jacob77.bopsearch.domain.MixRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixesScreen(
    mixes: List<MixEntity>,
    message: String?,
    onClearMessage: () -> Unit,
    onSave: (name: String, rules: MixRules, existingId: Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPlay: (MixEntity) -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf(0L) }
    var name by remember { mutableStateOf("") }
    var genreAllow by remember { mutableStateOf("") }
    var genreRequire by remember { mutableStateOf("") }
    var genreExclude by remember { mutableStateOf("") }
    var ratingMin by remember { mutableFloatStateOf(0f) }
    var useEnergy by remember { mutableStateOf(false) }
    var energyMin by remember { mutableFloatStateOf(0f) }
    var energyMax by remember { mutableFloatStateOf(1f) }
    var likeBias by remember { mutableFloatStateOf(0.5f) }
    var newBias by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            onClearMessage()
        }
    }

    fun openCreate() {
        editingId = 0L
        name = ""
        genreAllow = ""
        genreRequire = ""
        genreExclude = ""
        ratingMin = 0f
        useEnergy = false
        energyMin = 0f
        energyMax = 1f
        likeBias = 0.5f
        newBias = 0.5f
        editorOpen = true
    }

    fun openEdit(mix: MixEntity) {
        editingId = mix.id
        name = mix.name
        genreAllow = mix.genreAllow
        genreRequire = mix.genreRequire
        genreExclude = mix.genreExclude
        ratingMin = (mix.ratingMin ?: 0.0).toFloat()
        useEnergy = mix.energyMin != null || mix.energyMax != null
        energyMin = (mix.energyMin ?: 0.0).toFloat()
        energyMax = (mix.energyMax ?: 1.0).toFloat()
        likeBias = mix.likeBias.toFloat()
        newBias = mix.newBias.toFloat()
        editorOpen = true
    }

    fun save() {
        if (name.isBlank()) return
        onSave(
            name.trim(),
            MixRules(
                genreAllow = splitCsv(genreAllow),
                genreRequire = splitCsv(genreRequire),
                genreExclude = splitCsv(genreExclude),
                energyMin = if (useEnergy) energyMin.toDouble() else null,
                energyMax = if (useEnergy) energyMax.toDouble() else null,
                ratingMin = ratingMin.toDouble().takeIf { it > 0f },
                likeBias = likeBias.toDouble(),
                newBias = newBias.toDouble(),
            ),
            editingId,
        )
        editorOpen = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mixes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { openCreate() }) {
                Icon(Icons.Default.Add, contentDescription = "New mix")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (editorOpen) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Column(
                        Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (editingId > 0) "Edit mix" else "New mix",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = genreAllow,
                            onValueChange = { genreAllow = it },
                            label = { Text("Genre allow (ids, comma)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = genreRequire,
                            onValueChange = { genreRequire = it },
                            label = { Text("Genre require (ids, comma)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = genreExclude,
                            onValueChange = { genreExclude = it },
                            label = { Text("Genre exclude (ids, comma)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Rating min: ${ratingMin.toInt()} (0–100; 0 = any)",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = ratingMin,
                            onValueChange = { ratingMin = it },
                            valueRange = 0f..100f,
                        )
                        TextButton(onClick = { useEnergy = !useEnergy }) {
                            Text(if (useEnergy) "Clear energy window" else "Add energy window (0–1)")
                        }
                        if (useEnergy) {
                            Text(
                                "Energy ${"%.2f".format(energyMin)} – ${"%.2f".format(energyMax)}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Slider(
                                value = energyMin,
                                onValueChange = { energyMin = it.coerceAtMost(energyMax) },
                                valueRange = 0f..1f,
                            )
                            Slider(
                                value = energyMax,
                                onValueChange = { energyMax = it.coerceAtLeast(energyMin) },
                                valueRange = 0f..1f,
                            )
                        }
                        Text(
                            "Like bias: ${"%.2f".format(likeBias)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = likeBias,
                            onValueChange = { likeBias = it },
                            valueRange = 0f..1f,
                        )
                        Text(
                            "New bias: ${"%.2f".format(newBias)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = newBias,
                            onValueChange = { newBias = it },
                            valueRange = 0f..1f,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { save() }) { Text("Save") }
                            TextButton(onClick = { editorOpen = false }) { Text("Cancel") }
                        }
                    }
                }
            }
            if (mixes.isEmpty() && !editorOpen) {
                Text(
                    "No mixes yet. Tap + to create a rule set (schema: like_bias / new_bias / rating_min / genre / energy), then Play.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(mixes, key = { it.id }) { mix ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(mix.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            mix.mixId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onPlay(mix) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play mix")
                                        }
                                        IconButton(onClick = { onDelete(mix.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }
                                }
                                Text(
                                    buildString {
                                        append("like ${"%.0f".format(mix.likeBias * 100)}%")
                                        append(" · new ${"%.0f".format(mix.newBias * 100)}%")
                                        mix.ratingMin?.let { append(" · rating≥${it.toInt()}") }
                                        if (mix.energyMin != null || mix.energyMax != null) {
                                            append(
                                                " · energy ${mix.energyMin ?: 0}–${mix.energyMax ?: 1}",
                                            )
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = { openEdit(mix) }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun splitCsv(s: String): List<String> =
    s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
