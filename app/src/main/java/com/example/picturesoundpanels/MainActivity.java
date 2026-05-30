package com.example.picturesoundpanels;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 301;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 302;
    private static final String PREFS_NAME = "picture_sound_panels";
    private static final String PANELS_KEY = "panels";
    private static final int MAX_CARDS = 4;

    private final List<Panel> panels = new ArrayList<>();
    private LinearLayout panelList;
    private GridLayout cardGrid;
    private Button editButton;
    private Button addPanelButton;
    private TextView titleText;
    private int selectedPanelIndex = 0;
    private int pendingImageCardIndex = -1;
    private int pendingRecordCardIndex = -1;
    private boolean editMode = false;
    private MediaRecorder recorder;
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPanels();
        buildLayout();
        renderAll();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
        stopRecording(false);
        savePanels();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Color.rgb(246, 248, 250));
        setContentView(root);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(dp(10), dp(10), dp(10), dp(10));
        side.setBackgroundColor(Color.rgb(232, 238, 245));
        root.addView(side, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 17));

        TextView sideTitle = new TextView(this);
        sideTitle.setText("Panels");
        sideTitle.setTextSize(20);
        sideTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sideTitle.setTextColor(Color.rgb(30, 45, 65));
        side.addView(sideTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        panelList = new LinearLayout(this);
        panelList.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(panelList);
        side.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        addPanelButton = new Button(this);
        addPanelButton.setText("+ Panel");
        addPanelButton.setAllCaps(false);
        addPanelButton.setOnClickListener(v -> showPanelEditor(-1));
        side.addView(addPanelButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18), dp(12), dp(18), dp(18));
        root.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 83));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        main.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        titleText = new TextView(this);
        titleText.setTextSize(26);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Color.rgb(24, 36, 52));
        header.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        editButton = new Button(this);
        editButton.setAllCaps(false);
        editButton.setOnClickListener(v -> {
            editMode = !editMode;
            renderAll();
        });
        header.addView(editButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cardGrid = new GridLayout(this);
        cardGrid.setColumnCount(2);
        cardGrid.setRowCount(2);
        cardGrid.setUseDefaultMargins(true);
        main.addView(cardGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    private void renderAll() {
        if (panels.isEmpty()) {
            addDefaultPanels();
        }
        selectedPanelIndex = Math.max(0, Math.min(selectedPanelIndex, panels.size() - 1));
        Panel selectedPanel = panels.get(selectedPanelIndex);
        titleText.setText(selectedPanel.icon + " " + selectedPanel.name);
        editButton.setText(editMode ? "Done" : "Edit");
        addPanelButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
        renderPanelList();
        renderCards(selectedPanel);
        savePanels();
    }

    private void renderPanelList() {
        panelList.removeAllViews();
        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            Button button = new Button(this);
            button.setText(panel.icon + "  " + panel.name);
            button.setTextSize(16);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setPadding(dp(8), dp(8), dp(8), dp(8));
            button.setBackground(makeRoundedBackground(i == selectedPanelIndex ? Color.rgb(198, 225, 255) : Color.WHITE, Color.rgb(150, 167, 185), 2));
            final int index = i;
            button.setOnClickListener(v -> {
                selectedPanelIndex = index;
                renderAll();
            });
            button.setOnLongClickListener(v -> {
                showPanelEditor(index);
                return true;
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(6), 0, dp(6));
            panelList.addView(button, params);
        }
    }

    private void renderCards(Panel panel) {
        cardGrid.removeAllViews();
        for (int i = 0; i < MAX_CARDS; i++) {
            Card card = panel.cards.get(i);
            View cardView = createCardView(card, i);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(i / 2, 1f), GridLayout.spec(i % 2, 1f));
            params.width = 0;
            params.height = 0;
            params.setMargins(dp(10), dp(10), dp(10), dp(10));
            cardGrid.addView(cardView, params);
        }
    }

    private View createCardView(Card card, int cardIndex) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(dp(14), dp(14), dp(14), dp(14));
        cardLayout.setBackground(makeRoundedBackground(Color.WHITE, Color.rgb(120, 135, 150), 3));
        cardLayout.setOnClickListener(v -> {
            if (editMode) {
                showCardEditor(cardIndex);
            } else if (card.hasAudio()) {
                playAudio(card.audioPath);
            }
        });

        FrameLayout imageFrame = new FrameLayout(this);
        imageFrame.setBackground(makeRoundedBackground(Color.rgb(249, 250, 252), Color.rgb(95, 111, 128), 3));
        cardLayout.addView(imageFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        if (card.hasImage()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(Uri.parse(card.imageUri));
            imageFrame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            TextView placeholder = new TextView(this);
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setText(editMode ? "+\nAdd picture" : "Empty");
            placeholder.setTextSize(24);
            placeholder.setTextColor(Color.rgb(115, 130, 145));
            imageFrame.addView(placeholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        TextView label = new TextView(this);
        label.setText(card.label.isEmpty() ? (editMode ? "Tap to edit" : "") : card.label);
        label.setTextSize(26);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.rgb(20, 30, 40));
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, dp(10), 0, dp(8));
        cardLayout.addView(label, labelParams);

        Button playButton = new Button(this);
        playButton.setText(editMode ? (card.hasAudio() ? "Play / Record" : "Record") : "Play");
        playButton.setAllCaps(false);
        playButton.setTextColor(Color.WHITE);
        playButton.setTextSize(18);
        playButton.setTypeface(Typeface.DEFAULT_BOLD);
        playButton.setBackground(makeRoundedBackground(Color.rgb(198, 40, 40), Color.rgb(130, 20, 20), 2));
        playButton.setEnabled(editMode || card.hasAudio());
        playButton.setOnClickListener(v -> {
            if (editMode) {
                showCardEditor(cardIndex);
            } else if (card.hasAudio()) {
                playAudio(card.audioPath);
            }
        });
        cardLayout.addView(playButton, new LinearLayout.LayoutParams(dp(150), dp(56)));

        return cardLayout;
    }

    private void showPanelEditor(int index) {
        if (!editMode && index >= 0) {
            return;
        }
        Panel existing = index >= 0 ? panels.get(index) : null;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), 0, dp(18), 0);

        EditText iconInput = new EditText(this);
        iconInput.setHint("Icon, e.g. 🍎");
        iconInput.setText(existing == null ? "⭐" : existing.icon);
        form.addView(iconInput);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Panel name");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setText(existing == null ? "New Panel" : existing.name);
        form.addView(nameInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(index >= 0 ? "Edit panel" : "Add panel")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    String icon = iconInput.getText().toString().trim();
                    String name = nameInput.getText().toString().trim();
                    if (icon.isEmpty()) icon = "⭐";
                    if (name.isEmpty()) name = "Panel";
                    if (index >= 0) {
                        existing.icon = icon;
                        existing.name = name;
                    } else {
                        panels.add(new Panel(icon, name));
                        selectedPanelIndex = panels.size() - 1;
                        editMode = true;
                    }
                    renderAll();
                })
                .setNegativeButton("Cancel", null);
        if (index >= 0 && panels.size() > 1) {
            builder.setNeutralButton("Delete", (dialog, which) -> confirmDeletePanel(index));
        }
        builder.show();
    }

    private void confirmDeletePanel(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Delete panel?")
                .setMessage("This removes the panel and its four card slots from this device.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    panels.remove(index);
                    selectedPanelIndex = Math.max(0, selectedPanelIndex - 1);
                    renderAll();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCardEditor(int cardIndex) {
        Panel panel = panels.get(selectedPanelIndex);
        Card card = panel.cards.get(cardIndex);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), 0, dp(18), 0);

        EditText labelInput = new EditText(this);
        labelInput.setHint("Word or short phrase");
        labelInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        labelInput.setText(card.label);
        form.addView(labelInput);

        Button imageButton = new Button(this);
        imageButton.setText(card.hasImage() ? "Change picture" : "Add picture");
        imageButton.setAllCaps(false);
        imageButton.setOnClickListener(v -> {
            card.label = labelInput.getText().toString().trim();
            pendingImageCardIndex = cardIndex;
            savePanels();
            openImagePicker();
        });
        form.addView(imageButton);

        Button audioButton = new Button(this);
        audioButton.setText(card.hasAudio() ? "Play audio" : "No audio yet");
        audioButton.setAllCaps(false);
        audioButton.setEnabled(card.hasAudio());
        audioButton.setOnClickListener(v -> playAudio(card.audioPath));
        form.addView(audioButton);

        Button recordButton = new Button(this);
        recordButton.setText(card.hasAudio() ? "Record again" : "Record audio");
        recordButton.setAllCaps(false);
        recordButton.setOnClickListener(v -> {
            card.label = labelInput.getText().toString().trim();
            savePanels();
            startRecordingForCard(cardIndex);
        });
        form.addView(recordButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Edit card")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    card.label = labelInput.getText().toString().trim();
                    renderAll();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (dialog, which) -> {
                    card.clear();
                    renderAll();
                });
        builder.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void startRecordingForCard(int cardIndex) {
        pendingRecordCardIndex = cardIndex;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST);
            return;
        }
        showRecordingDialog(cardIndex);
    }

    private void showRecordingDialog(int cardIndex) {
        Card card = panels.get(selectedPanelIndex).cards.get(cardIndex);
        File audioFile = new File(getFilesDir(), "panel_" + selectedPanelIndex + "_card_" + cardIndex + "_" + System.currentTimeMillis() + ".m4a");
        try {
            stopPlayback();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (IOException | RuntimeException ex) {
            recorder = null;
            Toast.makeText(this, "Could not start recording.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Recording")
                .setMessage("Speak the word or phrase now, then tap Stop.")
                .setPositiveButton("Stop", (dialog, which) -> {
                    stopRecording(true);
                    card.audioPath = audioFile.getAbsolutePath();
                    renderAll();
                })
                .setOnCancelListener(dialog -> stopRecording(false))
                .show();
    }

    private void stopRecording(boolean keepFile) {
        if (recorder == null) {
            return;
        }
        try {
            recorder.stop();
        } catch (RuntimeException ignored) {
        }
        recorder.release();
        recorder = null;
        if (!keepFile && pendingRecordCardIndex >= 0) {
            pendingRecordCardIndex = -1;
        }
    }

    private void playAudio(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        stopPlayback();
        try {
            player = new MediaPlayer();
            player.setDataSource(path);
            player.setOnCompletionListener(mp -> stopPlayback());
            player.prepare();
            player.start();
        } catch (IOException | RuntimeException ex) {
            stopPlayback();
            Toast.makeText(this, "Could not play audio.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null && pendingImageCardIndex >= 0) {
            Uri uri = data.getData();
            try {
                final int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
            panels.get(selectedPanelIndex).cards.get(pendingImageCardIndex).imageUri = uri.toString();
            pendingImageCardIndex = -1;
            renderAll();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingRecordCardIndex >= 0) {
                showRecordingDialog(pendingRecordCardIndex);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Microphone permission needed")
                        .setMessage("Recording card audio requires microphone permission. You can enable it in Android Settings.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    private void loadPanels() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(PANELS_KEY, null);
        if (json == null) {
            addDefaultPanels();
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            panels.clear();
            for (int i = 0; i < array.length(); i++) {
                panels.add(Panel.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ex) {
            panels.clear();
            addDefaultPanels();
        }
    }

    private void savePanels() {
        JSONArray array = new JSONArray();
        for (Panel panel : panels) {
            array.put(panel.toJson());
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PANELS_KEY, array.toString()).apply();
    }

    private void addDefaultPanels() {
        panels.clear();
        panels.add(new Panel("🍎", "Food"));
        panels.add(new Panel("🐶", "Animals"));
        panels.add(new Panel("👨", "Family"));
        panels.add(new Panel("😊", "Feelings"));
    }

    private GradientDrawable makeRoundedBackground(int fillColor, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Panel {
        String icon;
        String name;
        final List<Card> cards = new ArrayList<>();

        Panel(String icon, String name) {
            this.icon = icon;
            this.name = name;
            while (cards.size() < MAX_CARDS) {
                cards.add(new Card());
            }
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            JSONArray cardArray = new JSONArray();
            try {
                object.put("icon", icon);
                object.put("name", name);
                for (Card card : cards) {
                    cardArray.put(card.toJson());
                }
                object.put("cards", cardArray);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static Panel fromJson(JSONObject object) throws JSONException {
            Panel panel = new Panel(object.optString("icon", "⭐"), object.optString("name", "Panel"));
            panel.cards.clear();
            JSONArray cardArray = object.optJSONArray("cards");
            if (cardArray != null) {
                for (int i = 0; i < cardArray.length() && i < MAX_CARDS; i++) {
                    panel.cards.add(Card.fromJson(cardArray.getJSONObject(i)));
                }
            }
            while (panel.cards.size() < MAX_CARDS) {
                panel.cards.add(new Card());
            }
            return panel;
        }
    }

    private static class Card {
        String imageUri = "";
        String label = "";
        String audioPath = "";

        boolean hasImage() {
            return imageUri != null && !imageUri.isEmpty();
        }

        boolean hasAudio() {
            return audioPath != null && !audioPath.isEmpty();
        }

        void clear() {
            imageUri = "";
            label = "";
            audioPath = "";
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("imageUri", imageUri);
                object.put("label", label);
                object.put("audioPath", audioPath);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static Card fromJson(JSONObject object) {
            Card card = new Card();
            card.imageUri = object.optString("imageUri", "");
            card.label = object.optString("label", "");
            card.audioPath = object.optString("audioPath", "");
            return card;
        }
    }
}
