package mrp.checkin.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Material 3 (Material You, seed #6750A4) desenhado com views do framework —
 * o build é CLI puro (sem Gradle), então não há lib com.google.android.material.
 * Também centraliza decisões de ergonomia: alvos táteis >= 48dp, ações
 * primárias em pill no rodapé (zona do polegar, lei de Fitts), grade de 4dp.
 */
public final class M3 {
    private M3() {
    }

    // ---- Paleta Material 3 light (seed #6750A4) ---------------------------
    public static final int PRIMARY = 0xFF6750A4;
    public static final int ON_PRIMARY = 0xFFFFFFFF;
    public static final int PRIMARY_CONTAINER = 0xFFEADDFF;
    public static final int ON_PRIMARY_CONTAINER = 0xFF21005D;
    public static final int SECONDARY = 0xFF625B71;
    public static final int ON_SECONDARY = 0xFFFFFFFF;
    public static final int SECONDARY_CONTAINER = 0xFFE8DEF8;
    public static final int ON_SECONDARY_CONTAINER = 0xFF1D192B;
    public static final int SURFACE = 0xFFFEF7FF;
    public static final int SURFACE_CONTAINER = 0xFFF3EDF7;
    public static final int ON_SURFACE = 0xFF1D1B20;
    public static final int SURFACE_VARIANT = 0xFFE7E0EC;
    public static final int ON_SURFACE_VARIANT = 0xFF49454F;
    public static final int OUTLINE = 0xFF79747E;
    public static final int OUTLINE_VARIANT = 0xFFCAC4D0;
    public static final int ERROR = 0xFFB3261E;
    public static final int ON_ERROR = 0xFFFFFFFF;
    public static final int ERROR_CONTAINER = 0xFFF9DEDC;
    public static final int ON_ERROR_CONTAINER = 0xFF410E0B;
    public static final int SUCCESS = 0xFF006D2A;
    public static final int SUCCESS_CONTAINER = 0xFFB7F0C1;
    public static final int ON_SUCCESS_CONTAINER = 0xFF00210B;
    public static final int WARN = 0xFF735C00;
    public static final int WARN_CONTAINER = 0xFFFBE29D;

    public static final int SCRIM = 0x99000000;
    public static final int SCRIM_SOFT = 0x66000000;

    // Overlays de ripple
    public static final int RIPPLE_ON_PRIMARY = 0x33FFFFFF;      // 20% branco sobre cor forte
    public static final int RIPPLE_ON_CONTAINER = 0x334A3E8A;    // 20% primary sobre container

    // ---- Escalas ----------------------------------------------------------
    public static final float SHAPE_PILL = 28f;
    public static final float SHAPE_CARD = 16f;
    public static final float SHAPE_FIELD = 12f;

    public static int dp(Context context, int n) {
        return (int) (context.getResources().getDisplayMetrics().density * n);
    }

    // ---- Drawables --------------------------------------------------------
    public static GradientDrawable rounded(int color, float radiusDp, Context context) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(context, (int) radiusDp));
        return g;
    }

    public static Drawable ripple(int overlay, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(overlay), content, content);
    }

    public static View spacer(Context context, int height) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, height)));
        return v;
    }

    // ---- Tipografia -------------------------------------------------------
    public static void labelStyle(TextView view, int size, int color, boolean medium) {
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(
                medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setLetterSpacing(0.01f);
    }

    public static TextView headline(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(28);
        v.setTextColor(ON_SURFACE);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    public static TextView title(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(22);
        v.setTextColor(ON_SURFACE);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return v;
    }

    public static TextView body(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(16);
        v.setTextColor(ON_SURFACE);
        v.setLineSpacing(0f, 1.1f);
        return v;
    }

    public static TextView label(Context context, String text) {
        TextView v = new TextView(context);
        labelStyle(v, 12, ON_SURFACE_VARIANT, true);
        v.setText(text);
        v.setAllCaps(true);
        v.setLetterSpacing(0.08f);
        return v;
    }

    public static TextView pill(Context context, String text, int bg, int fg) {
        TextView v = new TextView(context);
        labelStyle(v, 13, fg, true);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 6));
        v.setBackground(rounded(bg, SHAPE_PILL, context));
        return v;
    }

    // ---- Cards ------------------------------------------------------------
    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 16);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(rounded(SURFACE_CONTAINER, SHAPE_CARD, context));
        return card;
    }

    // ---- Botões -----------------------------------------------------------
    private static Button baseButton(Context context, String text) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextSize(16);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setAllCaps(false);
        b.setLetterSpacing(0.02f);
        b.setMinWidth(0);
        b.setMinHeight(dp(context, 52));
        b.setMaxHeight(Integer.MAX_VALUE);
        b.setGravity(Gravity.CENTER);
        b.setStateListAnimator(null);
        b.setElevation(0f);
        b.setPadding(dp(context, 24), 0, dp(context, 24), 0);
        return b;
    }

    public static Button filledButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(ON_PRIMARY);
        b.setBackground(ripple(RIPPLE_ON_PRIMARY, rounded(PRIMARY, SHAPE_PILL, context)));
        return b;
    }

    public static Button tonalButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(ON_SECONDARY_CONTAINER);
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, rounded(SECONDARY_CONTAINER, SHAPE_PILL, context)));
        return b;
    }

    public static Button outlinedButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(PRIMARY);
        GradientDrawable g = rounded(SURFACE, SHAPE_PILL, context);
        g.setStroke(dp(context, 2), OUTLINE);
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, g));
        return b;
    }

    public static Button textButton(Context context, String text) {
        return textButton(context, text, PRIMARY);
    }

    public static Button textButton(Context context, String text, int color) {
        Button b = baseButton(context, text);
        b.setTextColor(color);
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, rounded(Color.TRANSPARENT, SHAPE_PILL, context)));
        return b;
    }

    public static Button errorButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(ON_ERROR);
        b.setBackground(ripple(RIPPLE_ON_PRIMARY, rounded(ERROR, SHAPE_PILL, context)));
        return b;
    }

    /** Botão compacto para overlay sobre a câmera (scrim arredondado). */
    public static Button scrimButton(Context context, String text, int fg) {
        Button b = baseButton(context, text);
        b.setMinHeight(dp(context, 48));
        b.setMinWidth(0);
        b.setTextSize(14);
        b.setTextColor(fg);
        b.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        b.setBackground(ripple(RIPPLE_ON_PRIMARY, rounded(SCRIM_SOFT, SHAPE_PILL, context)));
        return b;
    }

    /** Botão compacto de destaque (ligado/ativo) sobre a câmera. */
    public static Button scrimButtonActive(Context context, String text, int fg) {
        Button b = baseButton(context, text);
        b.setMinHeight(dp(context, 48));
        b.setMinWidth(0);
        b.setTextSize(14);
        b.setTextColor(fg);
        b.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, rounded(PRIMARY_CONTAINER, SHAPE_PILL, context)));
        return b;
    }

    // ---- Campos de texto --------------------------------------------------
    private static void applyCursor(EditText field) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(PRIMARY);
            cursor.setBounds(0, 0, dp(field.getContext(), 2), dp(field.getContext(), 24));
            field.setTextCursorDrawable(cursor);
        }
    }

    public static EditText outlinedInput(Context context, String hint) {
        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(ON_SURFACE);
        field.setHintTextColor(ON_SURFACE_VARIANT);
        field.setSingleLine(true);
        field.setMinHeight(dp(context, 56));
        field.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        applyCursor(field);
        final GradientDrawable bg = rounded(SURFACE, SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), OUTLINE_VARIANT);
        field.setBackground(bg);
        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                bg.setStroke(dp(context, hasFocus ? 2 : 1),
                        hasFocus ? PRIMARY : OUTLINE_VARIANT);
            }
        });
        return field;
    }

    public static EditText outlinedTextArea(Context context, String hint) {
        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(ON_SURFACE);
        field.setHintTextColor(ON_SURFACE_VARIANT);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP | Gravity.START);
        field.setMinHeight(dp(context, 96));
        field.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        applyCursor(field);
        final GradientDrawable bg = rounded(SURFACE, SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), OUTLINE_VARIANT);
        field.setBackground(bg);
        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                bg.setStroke(dp(context, hasFocus ? 2 : 1),
                        hasFocus ? PRIMARY : OUTLINE_VARIANT);
            }
        });
        return field;
    }

    /** Campo de senha Outline M3 com toggle "Mostrar/Ocultar" de rio da direita. */
    public static LinearLayout outlinedPassword(Context context, String hint) {
        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 16), 0, dp(context, 4), 0);
        final GradientDrawable bg = rounded(SURFACE, SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), OUTLINE_VARIANT);
        row.setBackground(bg);

        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(ON_SURFACE);
        field.setHintTextColor(ON_SURFACE_VARIANT);
        field.setSingleLine(true);
        field.setMinHeight(dp(context, 56));
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setPadding(0, dp(context, 14), 0, dp(context, 14));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        applyCursor(field);
        row.addView(field, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        final TextView toggle = new TextView(context);
        labelStyle(toggle, 13, PRIMARY, true);
        toggle.setText("Mostrar");
        toggle.setContentDescription("Mostrar senha");
        toggle.setGravity(Gravity.CENTER);
        toggle.setMinWidth(dp(context, 56));
        toggle.setMinHeight(dp(context, 48));
        toggle.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        toggle.setBackground(ripple(RIPPLE_ON_CONTAINER,
                rounded(Color.TRANSPARENT, SHAPE_FIELD, context)));
        toggle.setOnClickListener(new View.OnClickListener() {
            private boolean visible;

            @Override
            public void onClick(View v) {
                visible = !visible;
                int pos = field.getSelectionStart();
                field.setInputType(visible
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                if (pos >= 0) {
                    field.setSelection(Math.min(pos, field.getText().length()));
                }
                toggle.setText(visible ? "Ocultar" : "Mostrar");
                toggle.setContentDescription(visible ? "Ocultar senha" : "Mostrar senha");
            }
        });
        row.addView(toggle);

        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                bg.setStroke(dp(context, hasFocus ? 2 : 1),
                        hasFocus ? PRIMARY : OUTLINE_VARIANT);
            }
        });
        return row;
    }

    /** Recupera o EditText interno de um campos criado por {@link #outlinedPassword}. */
    public static EditText passwordInner(LinearLayout passwordRow) {
        return (EditText) passwordRow.getChildAt(0);
    }

    // ---- Progress ---------------------------------------------------------
    public static ProgressBar spinner(Context context) {
        ProgressBar pb = new ProgressBar(context);
        pb.setIndeterminate(true);
        pb.setIndeterminateTintList(ColorStateList.valueOf(PRIMARY));
        pb.getIndeterminateDrawable().setColorFilter(PRIMARY, PorterDuff.Mode.SRC_IN);
        return pb;
    }

    // ---- Janela (bottom sheet) -------------------------------------------
    public static LinearLayout sheet(Context context) {
        final LinearLayout sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));
        GradientDrawable g = new GradientDrawable();
        g.setColor(SURFACE);
        final float r = dp(context, 28);
        g.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, 0, 0});
        sheet.setBackground(g);
        sheet.setElevation(dp(context, 16));
        sheet.setClipToOutline(false);
        sheet.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        return sheet;
    }

    // ---- Utils de seleção ------------------------------------------------
    public static ColorStateList checkTint() {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{PRIMARY, OUTLINE});
    }

    // ---- Bloco de sucesso (✓ grande) -------------------------------------
    public static LinearLayout successBlock(Context context) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        block.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 12));
        block.setBackground(rounded(SUCCESS_CONTAINER, SHAPE_CARD, context));

        TextView check = new TextView(context);
        check.setText("\u2713");
        check.setTextSize(56);
        check.setTextColor(ON_SUCCESS_CONTAINER);
        check.setGravity(Gravity.CENTER);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        block.addView(check);

        TextView caption = new TextView(context);
        caption.setText("Confirmado");
        labelStyle(caption, 14, ON_SUCCESS_CONTAINER, true);
        caption.setAllCaps(false);
        caption.setLetterSpacing(0f);
        caption.setGravity(Gravity.CENTER);
        block.addView(caption);
        return block;
    }

    // ---- Insets -----------------------------------------------------------
    /** Aplica o inset de status bar a um paddingTop base (scroll/root). */
    public static void edgeToEdgeTop(final View view, final int baseTopDp) {
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public android.view.WindowInsets onApplyWindowInsets(View v,
                                                                 android.view.WindowInsets insets) {
                int base = dp(v.getContext(), baseTopDp);
                int left = v.getPaddingLeft();
                int bottom = v.getPaddingBottom();
                int right = v.getPaddingRight();
                v.setPadding(left, base + insets.getSystemWindowInsetTop(), right, bottom);
                return insets;
            }
        });
    }

    public static void surfaceSystemBars(android.app.Activity activity) {
        activity.getWindow().setStatusBarColor(SURFACE);
        activity.getWindow().setNavigationBarColor(SURFACE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            activity.getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }
}