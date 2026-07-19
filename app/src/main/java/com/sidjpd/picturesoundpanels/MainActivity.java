package com.sidjpd.picturesoundpanels;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
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
    private static final int CONFIRM_DEVICE_CREDENTIAL_REQUEST = 303;
    private static final int CONFIRM_DONATION_AUTH_REQUEST = 304;
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
    private boolean isAudioPlaying = false;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private BillingHelper billingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPanels();
        buildLayout();
        renderAll();
        initBilling();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
        stopRecording(false);
        savePanels();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) {
            billingHelper.destroy();
        }
    }

    private void initBilling() {
        billingHelper = new BillingHelper(this, new BillingHelper.DonationCallback() {
            @Override
            public void onDonationSuccess(String productId) {
                Toast.makeText(MainActivity.this, "Thank you so much for your support! ❤️", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDonationError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Color.rgb(246, 248, 250));
        root.setPadding(0, getStatusBarTopPadding(), 0, 0);
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
        addPanelButton.setTextSize(isTablet() ? 16 : 13);
        addPanelButton.setOnClickListener(v -> {
            if (!isAudioPlaying) {
                showPanelEditor(-1);
            }
        });
        side.addView(addPanelButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button donateButton = new Button(this);
        donateButton.setText("Donate ☕");
        donateButton.setAllCaps(false);
        donateButton.setTextSize(isTablet() ? 16 : 13);
        donateButton.setOnClickListener(v -> {
            if (!isAudioPlaying) {
                handleDonateClick();
            }
        });
        side.addView(donateButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(12), dp(8), dp(12), dp(12));
        main.setBackground(makeRoundedBackground(Color.rgb(252, 253, 255), Color.rgb(172, 196, 224), 1));
        root.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 83));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        main.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        titleText = new TextView(this);
        titleText.setTextSize(isTablet() ? 24 : 18);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Color.rgb(24, 36, 52));
        header.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        editButton = new Button(this);
        editButton.setAllCaps(false);
        editButton.setTextSize(isTablet() ? 16 : 13);
        editButton.setOnClickListener(v -> {
            if (!isAudioPlaying) {
                if (editMode) {
                    editMode = false;
                    renderAll();
                } else {
                    tryAuthentication();
                }
            }
        });
        header.addView(editButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cardGrid = new GridLayout(this);
        cardGrid.setColumnCount(2);
        cardGrid.setRowCount(2);
        cardGrid.setUseDefaultMargins(true);
        cardGrid.setPadding(dp(3), dp(3), dp(3), dp(3));
        cardGrid.setBackground(makeRoundedBackground(Color.rgb(213, 226, 240), Color.rgb(74, 118, 168), 2));
        main.addView(cardGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    private void renderAll() {
        if (panels.isEmpty()) {
            addDefaultPanels();
        }
        selectedPanelIndex = Math.max(0, Math.min(selectedPanelIndex, panels.size() - 1));
        Panel selectedPanel = panels.get(selectedPanelIndex);
        titleText.setText("Picto Play  •  " + selectedPanel.icon + " " + selectedPanel.name);
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
            button.setTextSize(isTablet() ? 16 : 12);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setPadding(dp(8), dp(8), dp(8), dp(8));
            button.setBackground(makeRoundedBackground(i == selectedPanelIndex ? Color.rgb(198, 225, 255) : Color.WHITE, Color.rgb(150, 167, 185), 2));
            final int index = i;
            button.setOnClickListener(v -> {
                if (!isAudioPlaying) {
                    selectedPanelIndex = index;
                    renderAll();
                }
            });
            button.setOnLongClickListener(v -> {
                if (!isAudioPlaying) {
                    showPanelEditor(index);
                }
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
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            cardGrid.addView(cardView, params);
        }
    }

    private boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private View createCardView(Card card, int cardIndex) {
        boolean isPhone = !isTablet();
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(dp(6), dp(6), dp(6), dp(6));
        cardLayout.setBackground(makeRoundedBackground(Color.WHITE, Color.rgb(145, 155, 165), 1));
        cardLayout.setOnClickListener(v -> {
            if (editMode) {
                showCardEditor(cardIndex);
            } else if (!isAudioPlaying) {
                playCardAudioWithAnimation(cardLayout, card);
            }
        });

        FrameLayout imageFrame = new FrameLayout(this);
        imageFrame.setBackground(makeRoundedBackground(Color.rgb(249, 250, 252), Color.rgb(135, 145, 155), 1));
        cardLayout.addView(imageFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        if (card.hasImage()) {
            AdjustableImageView image = new AdjustableImageView(this, card);
            image.setImageURI(Uri.parse(card.imageUri));
            image.setOnClickListener(v -> {
                if (!editMode && !isAudioPlaying) {
                    playCardAudioWithAnimation(cardLayout, card);
                }
            });
            imageFrame.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (editMode) {
                TextView hint = new TextView(this);
                hint.setText("Drag / pinch");
                hint.setTextSize(isPhone ? 9 : 12);
                hint.setTextColor(Color.WHITE);
                hint.setGravity(Gravity.CENTER);
                hint.setPadding(dp(4), dp(2), dp(4), dp(2));
                hint.setBackground(makeRoundedBackground(Color.argb(160, 0, 0, 0), Color.TRANSPARENT, 0));
                FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
                hintParams.setMargins(0, dp(4), dp(4), 0);
                imageFrame.addView(hint, hintParams);
            }
        } else {
            TextView placeholder = new TextView(this);
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setText(editMode ? "+\nAdd picture" : "Empty");
            placeholder.setTextSize(isPhone ? 14 : 20);
            placeholder.setTextColor(Color.rgb(115, 130, 145));
            imageFrame.addView(placeholder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        TextView label = new TextView(this);
        label.setText(card.label.isEmpty() ? (editMode ? "Tap to edit" : "") : card.label);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.rgb(20, 30, 40));
        label.setGravity(isPhone ? (Gravity.START | Gravity.CENTER_VERTICAL) : Gravity.CENTER);
        label.setSingleLine(isPhone);
        if (isPhone) {
            label.setEllipsize(TextUtils.TruncateAt.END);
        }

        Button playButton = new Button(this);
        playButton.setText(editMode ? (card.hasAudio() ? "Play/Rec" : "Record") : "Play");
        playButton.setAllCaps(false);
        playButton.setTextColor(Color.WHITE);
        playButton.setTypeface(Typeface.DEFAULT_BOLD);
        playButton.setBackground(makeRoundedBackground(Color.rgb(198, 40, 40), Color.rgb(130, 20, 20), 1));
        playButton.setEnabled(editMode || card.hasAudio());
        playButton.setOnClickListener(v -> {
            if (editMode) {
                showCardEditor(cardIndex);
            } else if (card.hasAudio() && !isAudioPlaying) {
                playCardAudioWithAnimation(cardLayout, card);
            }
        });
        if (isPhone) {
            playButton.setPadding(0, 0, 0, 0);
            playButton.setMinWidth(0);
            playButton.setMinHeight(0);
        }

        if (isPhone) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            label.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(label, lp);

            playButton.setTextSize(11);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(76), dp(30));
            bp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(playButton, bp);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, dp(4), 0, 0);
            cardLayout.addView(row, rowParams);
        } else {
            label.setTextSize(18);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, dp(4), 0, dp(3));
            cardLayout.addView(label, labelParams);

            playButton.setTextSize(14);
            cardLayout.addView(playButton, new LinearLayout.LayoutParams(dp(92), dp(40)));
        }

        return cardLayout;
    }
    private void playCardAudioWithAnimation(View cardView, Card card) {
        popView(cardView, 1.16f, 150, 320);

        // Dim other cards to highlight the active one
        for (int i = 0; i < cardGrid.getChildCount(); i++) {
            View child = cardGrid.getChildAt(i);
            if (child != cardView) {
                child.animate()
                     .alpha(0.4f)
                     .setDuration(150)
                     .withEndAction(() -> child.animate().alpha(1.0f).setDuration(320).start())
                     .start();
            }
        }

        // Apply a bold red border highlight to the clicked card during animation
        GradientDrawable highlightBg = makeRoundedBackground(Color.WHITE, Color.rgb(198, 40, 40), 3);
        GradientDrawable normalBg = makeRoundedBackground(Color.WHITE, Color.rgb(145, 155, 165), 1);
        cardView.setBackground(highlightBg);
        cardView.postDelayed(() -> cardView.setBackground(normalBg), 470); // 150ms + 320ms = 470ms

        if (card.hasAudio()) {
            playAudio(card.audioPath);
        }
    }
    private void popView(View view, float peakScale, long growDuration, long settleDuration) {
        view.animate().cancel();
        view.bringToFront();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setAlpha(1.0f);
        AnimatorSet pop = new AnimatorSet();
        ObjectAnimator growX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, peakScale);
        ObjectAnimator growY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, peakScale);
        ObjectAnimator fadeDown = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.86f);
        ObjectAnimator settleX = ObjectAnimator.ofFloat(view, View.SCALE_X, peakScale, 1.0f);
        ObjectAnimator settleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, peakScale, 1.0f);
        ObjectAnimator fadeUp = ObjectAnimator.ofFloat(view, View.ALPHA, 0.86f, 1.0f);
        growX.setDuration(growDuration);
        growY.setDuration(growDuration);
        fadeDown.setDuration(growDuration);
        settleX.setDuration(settleDuration);
        settleY.setDuration(settleDuration);
        fadeUp.setDuration(settleDuration);
        settleX.setInterpolator(new OvershootInterpolator());
        settleY.setInterpolator(new OvershootInterpolator());
        pop.play(growX).with(growY).with(fadeDown);
        pop.play(settleX).with(settleY).with(fadeUp).after(growX);
        pop.start();
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

        if (card.hasImage()) {
            TextView adjustHint = new TextView(this);
            adjustHint.setText("In Edit mode, drag the picture with one finger or pinch with two fingers. These buttons also fine-tune the picture.");
            adjustHint.setTextSize(13);
            adjustHint.setTextColor(Color.rgb(70, 80, 90));
            form.addView(adjustHint);

            LinearLayout zoomRow = new LinearLayout(this);
            zoomRow.setOrientation(LinearLayout.HORIZONTAL);
            zoomRow.addView(makeAdjustButton("Zoom +", () -> adjustImage(card, 1.12f, 0, 0)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            zoomRow.addView(makeAdjustButton("Zoom -", () -> adjustImage(card, 0.88f, 0, 0)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            zoomRow.addView(makeAdjustButton("Reset", () -> {
                card.resetImagePosition();
                renderAll();
            }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            form.addView(zoomRow);

            LinearLayout panRow = new LinearLayout(this);
            panRow.setOrientation(LinearLayout.HORIZONTAL);
            panRow.addView(makeAdjustButton("←", () -> adjustImage(card, 1f, -24, 0)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            panRow.addView(makeAdjustButton("↑", () -> adjustImage(card, 1f, 0, -24)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            panRow.addView(makeAdjustButton("↓", () -> adjustImage(card, 1f, 0, 24)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            panRow.addView(makeAdjustButton("→", () -> adjustImage(card, 1f, 24, 0)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            form.addView(panRow);
        }

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

    private Button makeAdjustButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void adjustImage(Card card, float scaleMultiplier, float offsetXDelta, float offsetYDelta) {
        card.imageScale = Math.max(0.5f, Math.min(4.0f, card.imageScale * scaleMultiplier));
        card.imageOffsetX += offsetXDelta;
        card.imageOffsetY += offsetYDelta;
        renderAll();
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
            isAudioPlaying = true;
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
        isAudioPlaying = false;
    }

    private void tryAuthentication() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            Intent intent = km.createConfirmDeviceCredentialIntent("Access Edit Mode", "Please verify your PIN, pattern, or password to access care-giver edit options.");
            if (intent != null) {
                startActivityForResult(intent, CONFIRM_DEVICE_CREDENTIAL_REQUEST);
                return;
            }
        }
        Toast.makeText(this, "Device is not secure. Edit Mode unlocked.", Toast.LENGTH_SHORT).show();
        editMode = true;
        renderAll();
    }

    private void handleDonateClick() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            Intent intent = km.createConfirmDeviceCredentialIntent("Parent Verification", "Please verify to open the Donation Support dialog.");
            if (intent != null) {
                startActivityForResult(intent, CONFIRM_DONATION_AUTH_REQUEST);
                return;
            }
        }
        showDonationDialog();
    }

    private void showDonationDialog() {
        String[] options = {
            "☕ Small Support Tip ($1.99)",
            "🍰 Medium Support Tip ($4.99)",
            "🍔 Large Support Tip ($9.99)"
        };
        String[] productIds = {
            "tip_small",
            "tip_medium",
            "tip_large"
        };
        new AlertDialog.Builder(this)
                .setTitle("Support Picto Play - Select Tip Amount")
                .setItems(options, (dialog, which) -> {
                    if (billingHelper != null) {
                        billingHelper.makeDonation(productIds[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null && pendingImageCardIndex >= 0) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
            Card selectedCard = panels.get(selectedPanelIndex).cards.get(pendingImageCardIndex);
            selectedCard.imageUri = uri.toString();
            selectedCard.resetImagePosition();
            pendingImageCardIndex = -1;
            renderAll();
        } else if (requestCode == CONFIRM_DEVICE_CREDENTIAL_REQUEST) {
            if (resultCode == RESULT_OK) {
                editMode = true;
                renderAll();
            } else {
                Toast.makeText(this, "Authentication failed. Edit Mode remains locked.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CONFIRM_DONATION_AUTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                showDonationDialog();
            }
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
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private int getStatusBarTopPadding() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return dp(24);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class AdjustableImageView extends ImageView {
        private final Card card;
        private final Matrix imageMatrix = new Matrix();
        private final PointF lastTouch = new PointF();
        private float lastPinchDistance = 0f;

        AdjustableImageView(Activity activity, Card card) {
            super(activity);
            this.card = card;
            setScaleType(ImageView.ScaleType.MATRIX);
            setOnTouchListener((view, event) -> handleImageTouch(event));
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            updateImageMatrix();
        }

        @Override
        public void setImageURI(Uri uri) {
            super.setImageURI(uri);
            post(this::updateImageMatrix);
        }

        private boolean handleImageTouch(MotionEvent event) {
            if (!editMode || !card.hasImage()) {
                return false;
            }
            if (event.getPointerCount() >= 2) {
                float distance = pinchDistance(event);
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    lastPinchDistance = distance;
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && lastPinchDistance > 0f) {
                    float multiplier = distance / lastPinchDistance;
                    card.imageScale = Math.max(0.5f, Math.min(4.0f, card.imageScale * multiplier));
                    lastPinchDistance = distance;
                    updateImageMatrix();
                    savePanels();
                }
                return true;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouch.set(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastTouch.x;
                    float dy = event.getY() - lastTouch.y;
                    card.imageOffsetX += dx;
                    card.imageOffsetY += dy;
                    lastTouch.set(event.getX(), event.getY());
                    updateImageMatrix();
                    savePanels();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastPinchDistance = 0f;
                    return true;
                default:
                    return true;
            }
        }

        private float pinchDistance(MotionEvent event) {
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private void updateImageMatrix() {
            if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }
            float drawableWidth = getDrawable().getIntrinsicWidth();
            float drawableHeight = getDrawable().getIntrinsicHeight();
            if (drawableWidth <= 0 || drawableHeight <= 0) {
                return;
            }
            float baseScale = Math.max(getWidth() / drawableWidth, getHeight() / drawableHeight);
            float finalScale = baseScale * card.imageScale;
            float scaledWidth = drawableWidth * finalScale;
            float scaledHeight = drawableHeight * finalScale;
            float translateX = (getWidth() - scaledWidth) / 2f + card.imageOffsetX;
            float translateY = (getHeight() - scaledHeight) / 2f + card.imageOffsetY;

            imageMatrix.reset();
            imageMatrix.postScale(finalScale, finalScale);
            imageMatrix.postTranslate(translateX, translateY);
            setImageMatrix(imageMatrix);
        }
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
        float imageScale = 1f;
        float imageOffsetX = 0f;
        float imageOffsetY = 0f;

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
            resetImagePosition();
        }

        void resetImagePosition() {
            imageScale = 1f;
            imageOffsetX = 0f;
            imageOffsetY = 0f;
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("imageUri", imageUri);
                object.put("label", label);
                object.put("audioPath", audioPath);
                object.put("imageScale", imageScale);
                object.put("imageOffsetX", imageOffsetX);
                object.put("imageOffsetY", imageOffsetY);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static Card fromJson(JSONObject object) {
            Card card = new Card();
            card.imageUri = object.optString("imageUri", "");
            card.label = object.optString("label", "");
            card.audioPath = object.optString("audioPath", "");
            card.imageScale = (float) object.optDouble("imageScale", 1.0);
            card.imageOffsetX = (float) object.optDouble("imageOffsetX", 0.0);
            card.imageOffsetY = (float) object.optDouble("imageOffsetY", 0.0);
            return card;
        }
    }
}
