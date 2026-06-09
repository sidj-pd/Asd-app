package com.example.picturesoundpanels.v3

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(storagePermission), 101)
        }

        setContent {
            val viewModel: PanelViewModel = viewModel()
            val isDark by viewModel.darkMode
            AsdAppTheme(darkTheme = isDark) {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PanelViewModel = viewModel()) {
    val context = LocalContext.current
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
                text = "Speak Help",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary, // Dynamically uses theme primary
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(panels) { index, panel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PanelItem(
                                icon = panel.icon,
                                name = panel.name,
                                isSelected = index == selectedIndex,
                                onClick = { viewModel.selectPanel(index) }
                            )
                        }
                        if (editMode) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = { viewModel.movePanelUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "▲",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.movePanelDown(index) },
                                    enabled = index < panels.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "▼",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < panels.size - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
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
                .padding(12.dp) // Reduced outer padding from 24.dp to 12.dp
                .clip(RoundedCornerShape(24.dp)) // Adjusted corner radius to match
                .background(if (isSystemInDarkTheme()) Color(0xFF252525) else Color(0xFFE5DFD5)) // Soothing charcoal in dark mode vs warm sand
                .padding(12.dp) // Reduced inner padding from 24.dp to 12.dp
        ) {
            val currentPanel = panels.getOrNull(selectedIndex)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentPanel?.icon ?: ""} ${currentPanel?.name ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground // Correctly adapts to light/dark themes
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editMode) {
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = viewModel.darkMode.value,
                            onCheckedChange = { viewModel.setDarkMode(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Button(
                        onClick = { viewModel.toggleEditMode() },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (editMode) "Done" else "Edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Grid (Fixed 2x2 Layout to prevent height collapse)
            currentPanel?.let { panel ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Reduced grid gap to 12.dp
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp) // Reduced grid gap to 12.dp
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardItem(
                                card = panel.cards[0],
                                editMode = editMode,
                                isActive = activePlaybackIndex == 0,
                                isDimmed = activePlaybackIndex != -1 && activePlaybackIndex != 0,
                                onClick = {
                                    if (editMode) showCardDialog = 0
                                    else viewModel.playCardAudio(0)
                                },
                                onImageUpdate = { scale, x, y ->
                                    viewModel.updateCard(selectedIndex, 0, false) {
                                        it.imageScale = scale
                                        it.imageOffsetX = x
                                        it.imageOffsetY = y
                                    }
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardItem(
                                card = panel.cards[1],
                                editMode = editMode,
                                isActive = activePlaybackIndex == 1,
                                isDimmed = activePlaybackIndex != -1 && activePlaybackIndex != 1,
                                onClick = {
                                    if (editMode) showCardDialog = 1
                                    else viewModel.playCardAudio(1)
                                },
                                onImageUpdate = { scale, x, y ->
                                    viewModel.updateCard(selectedIndex, 1, false) {
                                        it.imageScale = scale
                                        it.imageOffsetX = x
                                        it.imageOffsetY = y
                                    }
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp) // Reduced grid gap to 12.dp
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardItem(
                                card = panel.cards[2],
                                editMode = editMode,
                                isActive = activePlaybackIndex == 2,
                                isDimmed = activePlaybackIndex != -1 && activePlaybackIndex != 2,
                                onClick = {
                                    if (editMode) showCardDialog = 2
                                    else viewModel.playCardAudio(2)
                                },
                                onImageUpdate = { scale, x, y ->
                                    viewModel.updateCard(selectedIndex, 2, false) {
                                        it.imageScale = scale
                                        it.imageOffsetX = x
                                        it.imageOffsetY = y
                                    }
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardItem(
                                card = panel.cards[3],
                                editMode = editMode,
                                isActive = activePlaybackIndex == 3,
                                isDimmed = activePlaybackIndex != -1 && activePlaybackIndex != 3,
                                onClick = {
                                    if (editMode) showCardDialog = 3
                                    else viewModel.playCardAudio(3)
                                },
                                onImageUpdate = { scale, x, y ->
                                    viewModel.updateCard(selectedIndex, 3, false) {
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
                    val mediaStoreUri = getMediaStoreUri(context, uri)
                    viewModel.updateCard(selectedIndex, cardIndex) { 
                        it.imageUri = mediaStoreUri.toString()
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
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) primaryColor.copy(alpha = 0.15f) else surfaceColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) primaryColor else onSurfaceColor.copy(alpha = 0.15f)
        ),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) primaryColor else onSurfaceColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CardItem(
    card: CardModel, 
    editMode: Boolean, 
    isActive: Boolean, 
    isDimmed: Boolean,
    onClick: () -> Unit,
    onImageUpdate: (Float, Float, Float) -> Unit
) {
    val currentCard by rememberUpdatedState(card)
    val currentOnImageUpdate by rememberUpdatedState(onImageUpdate)

    var scaleState by remember(card) { mutableStateOf(card.imageScale) }
    var offsetXState by remember(card) { mutableStateOf(card.imageOffsetX) }
    var offsetYState by remember(card) { mutableStateOf(card.imageOffsetY) }
    
    // Smooth spotlight transitions
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.7f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardAlpha"
    )

    val isDark = isSystemInDarkTheme()
    val activeBgColor = if (isDark) Color(0xFF332F1A) else Color(0xFFFFF9E6)
    val inactiveBgColor = MaterialTheme.colorScheme.surface

    val containerColor by animateColorAsState(
        targetValue = if (isActive) activeBgColor else inactiveBgColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "containerColor"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1.00f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardScale"
    )

    val imageScaleAnim by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1.00f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "imageScale"
    )

    val activeBorderColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFFBC02D)
    val inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    val borderStrokeColor by animateColorAsState(
        targetValue = if (isActive) activeBorderColor else inactiveBorderColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "borderColor"
    )

    val borderStrokeWidth by animateDpAsState(
        targetValue = if (isActive) 3.dp else 1.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "borderWidth"
    )

    val textScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f, // Smooth text expansion
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "textScale"
    )

    val activeTextColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    val inactiveTextColor = MaterialTheme.colorScheme.onSurface

    val textColor by animateColorAsState(
        targetValue = if (isActive) activeTextColor else inactiveTextColor,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "textColor"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .border(borderStrokeWidth, borderStrokeColor, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF121212) else Color(0xFFFAF7F1)),
                contentAlignment = Alignment.Center
            ) {
                if (card.hasImage()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = Uri.parse(card.imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onError = { state ->
                            val error = state.result.throwable
                            Toast.makeText(context, "Image error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            error.printStackTrace()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pulse = imageScaleAnim
                                scaleX = scaleState * pulse
                                scaleY = scaleState * pulse
                                translationX = offsetXState
                                translationY = offsetYState
                            }
                            .then(if (editMode) {
                                Modifier.pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scaleState = (scaleState * zoom).coerceIn(0.5f, 5.0f)
                                        offsetXState += pan.x
                                        offsetYState += pan.y
                                        currentOnImageUpdate(scaleState, offsetXState, offsetYState)
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
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp), // Increased height for larger font
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Symmetrical placeholder on the left to perfectly center the text
                Spacer(modifier = Modifier.size(32.dp))

                Text(
                    text = if (card.label.isEmpty() && editMode) "Tap to edit" else card.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), // Larger 18sp font
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = textColor,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = textScale
                            scaleY = textScale
                        }
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isActive) Color(0xFFD32F2F)
                            else if (editMode) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (editMode) {
                            if (card.hasAudio()) "🔊" else "🎙️"
                        } else {
                            "▶"
                        },
                        color = Color.White,
                        fontSize = 14.sp
                    )
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
        title = { Text("Category Editor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = icon, onValueChange = { icon = it }, label = { Text("Icon (e.g. 🍎)") })
                TextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") })
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
        uri?.let { onImagePick(it) }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                imageLauncher.launch(arrayOf("image/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open image picker", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Storage permission required to select images", Toast.LENGTH_SHORT).show()
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
                    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    if (ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            imageLauncher.launch(arrayOf("image/*")) 
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open image picker", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        storagePermissionLauncher.launch(permissionToRequest)
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (card.hasImage()) "Change Picture" else "Add Picture")
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
        dismissButton = {
            TextButton(onClick = {
                viewModel.updateCard(viewModel.selectedPanelIndex.value, cardIndex) { it.clear() }
                onDismiss()
            }) {
                Text("Clear", color = Color.Red)
            }
        }
    )
}

fun getMediaStoreUri(context: Context, uri: Uri): Uri {
    if (DocumentsContract.isDocumentUri(context, uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        if (docId.startsWith("image:")) {
            val id = docId.split(":")[1]
            try {
                return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toLong()
                )
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
    }
    return uri
}
