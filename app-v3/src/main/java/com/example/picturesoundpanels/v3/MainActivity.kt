package com.example.picturesoundpanels.v3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.picturesoundpanels.v3.models.CardModel
import com.example.picturesoundpanels.v3.models.PanelModel
import com.example.picturesoundpanels.v3.ui.theme.AsdAppTheme
import com.example.picturesoundpanels.v3.viewmodels.PanelViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsdAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PanelViewModel = viewModel()) {
    val panels = viewModel.panels
    val selectedIndex by viewModel.selectedPanelIndex
    val editMode by viewModel.editMode
    val activePlaybackIndex by viewModel.activePlaybackCardIndex

    var showPanelDialog by remember { mutableStateOf<Int?>(null) }
    var showCardDialog by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Sidebar
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.25f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "Panels",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(panels) { index, panel ->
                    PanelItem(
                        icon = panel.icon,
                        name = panel.name,
                        isSelected = index == selectedIndex,
                        onClick = { viewModel.selectPanel(index) }
                    )
                }
            }

            if (editMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPanelDialog = -1 },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+ Add")
                    }
                    Button(
                        onClick = { viewModel.removePanel(selectedIndex) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("- Rem")
                    }
                }
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.75f)
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            val currentPanel = panels.getOrNull(selectedIndex)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speak Help • ${currentPanel?.icon ?: ""} ${currentPanel?.name ?: ""}",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Button(
                    onClick = { viewModel.toggleEditMode() },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (editMode) "Done" else "Edit")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                currentPanel?.let { panel ->
                    items(4) { cardIndex ->
                        CardItem(
                            card = panel.cards[cardIndex],
                            editMode = editMode,
                            isActive = activePlaybackIndex == cardIndex,
                            onClick = {
                                if (editMode) {
                                    showCardDialog = cardIndex
                                } else {
                                    viewModel.playCardAudio(cardIndex)
                                }
                            },
                            onImageUpdate = { scale, x, y ->
                                viewModel.updateCard(selectedIndex, cardIndex) {
                                    it.imageScale = scale
                                    it.imageOffsetX = x
                                    it.imageOffsetY = y
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    showPanelDialog?.let { index ->
        PanelEditorDialog(
            initialIcon = if (index >= 0) panels[index].icon else "⭐",
            initialName = if (index >= 0) panels[index].name else "",
            onDismiss = { showPanelDialog = null },
            onSave = { icon, name ->
                if (index >= 0) viewModel.updatePanel(index, icon, name)
                else viewModel.addPanel(icon, name)
                showPanelDialog = null
            }
        )
    }

    showCardDialog?.let { cardIndex ->
        val currentPanel = panels.getOrNull(selectedIndex)
        currentPanel?.let { panel ->
            val card = panel.cards[cardIndex]
            CardEditorDialog(
                card = card,
                onDismiss = { showCardDialog = null },
                onSave = { label ->
                    viewModel.updateCard(selectedIndex, cardIndex) { it.label = label }
                    showCardDialog = null
                },
                onImagePick = { uri ->
                    viewModel.updateCard(selectedIndex, cardIndex) { 
                        it.imageUri = uri.toString()
                        it.resetImagePosition()
                    }
                },
                viewModel = viewModel,
                cardIndex = cardIndex
            )
        }
    }
}

@Composable
fun PanelItem(icon: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CardItem(
    card: CardModel, 
    editMode: Boolean, 
    isActive: Boolean, 
    onClick: () -> Unit,
    onImageUpdate: (Float, Float, Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playback")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 0.85f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFFFFDF9)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFAF7F1)),
                contentAlignment = Alignment.Center
            ) {
                if (card.hasImage()) {
                    AsyncImage(
                        model = card.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = card.imageScale
                                scaleY = card.imageScale
                                translationX = card.imageOffsetX
                                translationY = card.imageOffsetY
                            }
                            .then(if (editMode) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onImageUpdate(card.imageScale, card.imageOffsetX + dragAmount.x, card.imageOffsetY + dragAmount.y)
                                    }
                                }
                            } else Modifier)
                    )
                } else {
                    Text(
                        text = if (editMode) "+\nAdd picture" else "Empty",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (card.label.isEmpty() && editMode) "Tap to edit" else card.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(if (editMode) (if (card.hasAudio()) "Play/Rec" else "Record") else "Play")
                }
            }
        }
    }
}

@Composable
fun PanelEditorDialog(initialIcon: String, initialName: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var icon by remember { mutableStateOf(initialIcon) }
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Panel Editor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = icon, onValueChange = { icon = it }, label = { Text("Icon (e.g. 🍎)") })
                TextField(value = name, onValueChange = { name = it }, label = { Text("Panel Name") })
            }
        },
        confirmButton = { Button(onClick = { onSave(icon, name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CardEditorDialog(
    card: CardModel, 
    onDismiss: () -> Unit, 
    onSave: (String) -> Unit, 
    onImagePick: (Uri) -> Unit,
    viewModel: PanelViewModel,
    cardIndex: Int
) {
    var label by remember { mutableStateOf(card.label) }
    val context = LocalContext.current
    
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onImagePick(it)
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val path = viewModel.startRecording(cardIndex)
            if (path != null) {
                isRecording = true
                viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.audioPath = path }
            }
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                
                Button(onClick = { 
                    try {
                        imageLauncher.launch(arrayOf("image/*")) 
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open image picker", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (card.hasImage()) "Change Picture" else "Add Picture")
                }

                if (card.hasImage()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.imageScale *= 1.1f } }, modifier = Modifier.weight(1f)) { Text("Zoom +") }
                        Button(onClick = { viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.imageScale *= 0.9f } }, modifier = Modifier.weight(1f)) { Text("Zoom -") }
                        Button(onClick = { viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.resetImagePosition() } }, modifier = Modifier.weight(1f)) { Text("Reset") }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.playCardAudio(cardIndex) }, 
                        enabled = card.hasAudio(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Play") }
                    
                    Button(
                        onClick = {
                            if (!isRecording) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val path = viewModel.startRecording(cardIndex)
                                    if (path != null) {
                                        isRecording = true
                                        viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.audioPath = path }
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                viewModel.stopRecording(true)
                                isRecording = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                    ) { 
                        Text(if (isRecording) "Stop" else "Record") 
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(label) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { card.clear(); onDismiss() }) { Text("Clear", color = Color.Red) } }
    )
}
