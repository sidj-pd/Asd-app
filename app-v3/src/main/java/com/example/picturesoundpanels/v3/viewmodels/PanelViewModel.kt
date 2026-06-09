package com.example.picturesoundpanels.v3.viewmodels

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.picturesoundpanels.v3.models.CardModel
import com.example.picturesoundpanels.v3.models.PanelModel
import org.json.JSONArray
import org.json.JSONException
import java.io.File

class PanelViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("picture_sound_panels", Context.MODE_PRIVATE)
    private val PANELS_KEY = "panels"

    private val _panels = mutableStateListOf<PanelModel>()
    val panels: List<PanelModel> = _panels

    private val _selectedPanelIndex = mutableStateOf(0)
    val selectedPanelIndex: State<Int> = _selectedPanelIndex

    private val _editMode = mutableStateOf(false)
    val editMode: State<Boolean> = _editMode

    private val _activePlaybackCardIndex = mutableStateOf(-1)
    val activePlaybackCardIndex: State<Int> = _activePlaybackCardIndex

    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private val playbackHandler = Handler(Looper.getMainLooper())
    private val CHILD_PLAYBACK_FALLBACK_MS = 10000L

    init {
        loadPanels()
    }

    private fun loadPanels() {
        val json = prefs.getString(PANELS_KEY, null)
        if (json == null) {
            addDefaultPanels()
            return
        }
        try {
            val array = JSONArray(json)
            _panels.clear()
            for (i in 0 until array.length()) {
                _panels.add(PanelModel.fromJson(array.getJSONObject(i)))
            }
        } catch (e: JSONException) {
            _panels.clear()
            addDefaultPanels()
        }
    }

    fun savePanels() {
        val array = JSONArray()
        _panels.forEach { array.put(it.toJson()) }
        prefs.edit().putString(PANELS_KEY, array.toString()).apply()
    }

    private fun addDefaultPanels() {
        _panels.clear()
        _panels.add(PanelModel("🍎", "Fruits"))
        _panels.add(PanelModel("🐶", "Animals"))
        _panels.add(PanelModel("👨", "Family"))
        _panels.add(PanelModel("😊", "Feelings"))
        savePanels()
    }

    fun selectPanel(index: Int) {
        if (_activePlaybackCardIndex.value != -1) return
        _selectedPanelIndex.value = index.coerceIn(0, _panels.size - 1)
    }

    fun toggleEditMode() {
        if (_activePlaybackCardIndex.value != -1) stopPlayback()
        _editMode.value = !_editMode.value
        if (!_editMode.value) {
            savePanels()
        }
    }

    fun addPanel(icon: String, name: String) {
        _panels.add(PanelModel(icon, name))
        _selectedPanelIndex.value = _panels.size - 1
        savePanels()
    }

    fun updatePanel(index: Int, icon: String, name: String) {
        if (index in _panels.indices) {
            val panel = _panels[index]
            panel.icon = icon
            panel.name = name
            _panels[index] = panel.copy()
            savePanels()
        }
    }

    fun removePanel(index: Int) {
        if (_panels.size > 1 && index in _panels.indices) {
            _panels.removeAt(index)
            _selectedPanelIndex.value = _selectedPanelIndex.value.coerceAtMost(_panels.size - 1)
            savePanels()
        }
    }

    fun updateCard(panelIndex: Int, cardIndex: Int, update: (CardModel) -> Unit) {
        if (panelIndex in _panels.indices) {
            val panel = _panels[panelIndex]
            if (cardIndex in panel.cards.indices) {
                update(panel.cards[cardIndex])
                _panels[panelIndex] = panel.copy()
                savePanels()
            }
        }
    }

    fun playCardAudio(cardIndex: Int) {
        if (_editMode.value || _activePlaybackCardIndex.value != -1) return
        val card = _panels[_selectedPanelIndex.value].cards[cardIndex]
        if (!card.hasAudio()) return

        _activePlaybackCardIndex.value = cardIndex
        playAudio(card.audioPath) {
            _activePlaybackCardIndex.value = -1
        }

        playbackHandler.postDelayed({
            if (_activePlaybackCardIndex.value == cardIndex) {
                stopPlayback()
                _activePlaybackCardIndex.value = -1
            }
        }, CHILD_PLAYBACK_FALLBACK_MS)
    }

    private fun playAudio(path: String, onComplete: () -> Unit) {
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stopPlayback()
                    onComplete()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopPlayback()
            onComplete()
        }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
        playbackHandler.removeCallbacksAndMessages(null)
    }

    fun startRecording(cardIndex: Int): String? {
        stopPlayback()
        val audioFile = File(getApplication<Application>().filesDir, "panel_${_selectedPanelIndex.value}_card_${cardIndex}_${System.currentTimeMillis()}.m4a")
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            return audioFile.absolutePath
        } catch (e: Exception) {
            recorder = null
            return null
        }
    }

    fun stopRecording(keepFile: Boolean) {
        try {
            recorder?.stop()
        } catch (e: Exception) {}
        recorder?.release()
        recorder = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        stopRecording(false)
    }
}
