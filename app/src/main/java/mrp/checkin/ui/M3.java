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
import android.view.WindowInsets;
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

    // ---- Paleta Material 3 dark (seed #6750A4, esquema oficial) -------------
    public static final int DARK_PRIMARY = 0xFFD0BCFF;
    public static final int DARK_ON_PRIMARY = 0xFF381E72;
    public static final int DARK_PRIMARY_CONTAINER = 0xFF4F378B;
    public static final int DARK_ON_PRIMARY_CONTAINER = 0xFFEADDFF;
    public static final int DARK_SECONDARY = 0xFFCCC2DC;
    public static final int DARK_ON_SECONDARY = 0xFF332D41;
    public static final int DARK_SECONDARY_CONTAINER = 0xFF4A4458;
    public static final int DARK_ON_SECONDARY_CONTAINER = 0xFFE8DEF8;
    public static final int DARK_SURFACE = 0xFF141218;
    public static final int DARK_SURFACE_CONTAINER = 0xFF211F26;
    public static final int DARK_ON_SURFACE = 0xFFE6E0E9;
    public static final int DARK_SURFACE_VARIANT = 0xFF49454F;
    public static final int DARK_ON_SURFACE_VARIANT = 0xFFCAC4D0;
    public static final int DARK_OUTLINE = 0xFF938F99;
    public static final int DARK_OUTLINE_VARIANT = 0xFF49454F;
    public static final int DARK_ERROR = 0xFFF2B8B5;
    public static final int DARK_ON_ERROR = 0xFF601410;
    public static final int DARK_ERROR_CONTAINER = 0xFF8C1D18;
    public static final int DARK_ON_ERROR_CONTAINER = 0xFFF9DEDC;
    public static final int DARK_SUCCESS = 0xFF7BD88F;
    public static final int DARK_SUCCESS_CONTAINER = 0xFF0B3D1F;
    public static final int DARK_ON_SUCCESS_CONTAINER = 0xFFB7F0C1;
    public static final int DARK_WARN = 0xFFE7B009;
    public static final int DARK_WARN_CONTAINER = 0xFF3E2F00;

    // ---- Night -------------------------------------------------------------
    /** True quando o sistema está em dark theme (Configuration.UI_MODE_NIGHT_YES). */
    public static boolean isNight(Context context) {
        int night = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    // Resolvedores dinâmicos: devolvem a cor light ou dark conforme o uiMode.
    // Fábricas abaixo e telas DEVEM usar estes em vez das constantes cruas.
    public static int primary(Context c) {
        return isNight(c) ? DARK_PRIMARY : PRIMARY;
    }

    public static int onPrimary(Context c) {
        return isNight(c) ? DARK_ON_PRIMARY : ON_PRIMARY;
    }

    public static int primaryContainer(Context c) {
        return isNight(c) ? DARK_PRIMARY_CONTAINER : PRIMARY_CONTAINER;
    }

    public static int onPrimaryContainer(Context c) {
        return isNight(c) ? DARK_ON_PRIMARY_CONTAINER : ON_PRIMARY_CONTAINER;
    }

    public static int secondaryContainer(Context c) {
        return isNight(c) ? DARK_SECONDARY_CONTAINER : SECONDARY_CONTAINER;
    }

    public static int onSecondaryContainer(Context c) {
        return isNight(c) ? DARK_ON_SECONDARY_CONTAINER : ON_SECONDARY_CONTAINER;
    }

    public static int surface(Context c) {
        return isNight(c) ? DARK_SURFACE : SURFACE;
    }

    public static int surfaceContainer(Context c) {
        return isNight(c) ? DARK_SURFACE_CONTAINER : SURFACE_CONTAINER;
    }

    public static int onSurface(Context c) {
        return isNight(c) ? DARK_ON_SURFACE : ON_SURFACE;
    }

    public static int surfaceVariant(Context c) {
        return isNight(c) ? DARK_SURFACE_VARIANT : SURFACE_VARIANT;
    }

    public static int onSurfaceVariant(Context c) {
        return isNight(c) ? DARK_ON_SURFACE_VARIANT : ON_SURFACE_VARIANT;
    }

    public static int outline(Context c) {
        return isNight(c) ? DARK_OUTLINE : OUTLINE;
    }

    public static int outlineVariant(Context c) {
        return isNight(c) ? DARK_OUTLINE_VARIANT : OUTLINE_VARIANT;
    }

    public static int error(Context c) {
        return isNight(c) ? DARK_ERROR : ERROR;
    }

    public static int onError(Context c) {
        return isNight(c) ? DARK_ON_ERROR : ON_ERROR;
    }

    public static int errorContainer(Context c) {
        return isNight(c) ? DARK_ERROR_CONTAINER : ERROR_CONTAINER;
    }

    public static int onErrorContainer(Context c) {
        return isNight(c) ? DARK_ON_ERROR_CONTAINER : ON_ERROR_CONTAINER;
    }

    public static int success(Context c) {
        return isNight(c) ? DARK_SUCCESS : SUCCESS;
    }

    public static int successContainer(Context c) {
        return isNight(c) ? DARK_SUCCESS_CONTAINER : SUCCESS_CONTAINER;
    }

    public static int onSuccessContainer(Context c) {
        return isNight(c) ? DARK_ON_SUCCESS_CONTAINER : ON_SUCCESS_CONTAINER;
    }

    public static int warn(Context c) {
        return isNight(c) ? DARK_WARN : WARN;
    }

    public static int warnContainer(Context c) {
        return isNight(c) ? DARK_WARN_CONTAINER : WARN_CONTAINER;
    }

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
        return dpPx(context.getResources().getDisplayMetrics().density, n);
    }

    /**
     * Conversão dp->px pura (arredonda para baixo, como sempre foi no app) —
     * separada de {@link #dp(Context, int)} para poder ser testada na JVM.
     */
    public static int dpPx(float density, int dp) {
        return (int) (density * dp);
    }

    // ---- Motion ------------------------------------------------------------
    /** True se o sistema permite animações (Settings "remover animações" = 0). */
    public static boolean motionEnabled(Context context) {
        try {
            return android.provider.Settings.Global.getFloat(
                    context.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception ignored) {
            return true;
        }
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
        v.setTextColor(onSurface(context));
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    public static TextView title(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(22);
        v.setTextColor(onSurface(context));
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return v;
    }

    public static TextView body(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(16);
        v.setTextColor(onSurface(context));
        v.setLineSpacing(0f, 1.1f);
        return v;
    }

    public static TextView label(Context context, String text) {
        TextView v = new TextView(context);
        labelStyle(v, 12, onSurfaceVariant(context), true);
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
        card.setBackground(rounded(surfaceContainer(context), SHAPE_CARD, context));
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
        b.setTextColor(onPrimary(context));
        b.setBackground(ripple(RIPPLE_ON_PRIMARY, rounded(primary(context), SHAPE_PILL, context)));
        return b;
    }

    public static Button tonalButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(onSecondaryContainer(context));
        b.setBackground(ripple(RIPPLE_ON_CONTAINER,
                rounded(secondaryContainer(context), SHAPE_PILL, context)));
        return b;
    }

    public static Button outlinedButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(primary(context));
        GradientDrawable g = rounded(surface(context), SHAPE_PILL, context);
        g.setStroke(dp(context, 2), outline(context));
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, g));
        return b;
    }

    public static Button textButton(Context context, String text) {
        return textButton(context, text, primary(context));
    }

    public static Button textButton(Context context, String text, int color) {
        Button b = baseButton(context, text);
        b.setTextColor(color);
        b.setBackground(ripple(RIPPLE_ON_CONTAINER, rounded(Color.TRANSPARENT, SHAPE_PILL, context)));
        return b;
    }

    public static Button errorButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(onError(context));
        b.setBackground(ripple(RIPPLE_ON_PRIMARY, rounded(error(context), SHAPE_PILL, context)));
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
            cursor.setColor(primary(field.getContext()));
            cursor.setBounds(0, 0, dp(field.getContext(), 2), dp(field.getContext(), 24));
            field.setTextCursorDrawable(cursor);
        }
    }

    public static EditText outlinedInput(Context context, String hint) {
        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(onSurface(context));
        field.setHintTextColor(onSurfaceVariant(context));
        field.setSingleLine(true);
        field.setMinHeight(dp(context, 56));
        field.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        applyCursor(field);
        final GradientDrawable bg = rounded(surface(context), SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), outlineVariant(context));
        field.setBackground(bg);
        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Context c = v.getContext();
                bg.setStroke(dp(c, hasFocus ? 2 : 1),
                        hasFocus ? primary(c) : outlineVariant(c));
            }
        });
        return field;
    }

    public static EditText outlinedTextArea(Context context, String hint) {
        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(onSurface(context));
        field.setHintTextColor(onSurfaceVariant(context));
        field.setMinLines(3);
        field.setGravity(Gravity.TOP | Gravity.START);
        field.setMinHeight(dp(context, 96));
        field.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        applyCursor(field);
        final GradientDrawable bg = rounded(surface(context), SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), outlineVariant(context));
        field.setBackground(bg);
        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Context c = v.getContext();
                bg.setStroke(dp(c, hasFocus ? 2 : 1),
                        hasFocus ? primary(c) : outlineVariant(c));
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
        final GradientDrawable bg = rounded(surface(context), SHAPE_FIELD, context);
        bg.setStroke(dp(context, 1), outlineVariant(context));
        row.setBackground(bg);

        final EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(onSurface(context));
        field.setHintTextColor(onSurfaceVariant(context));
        field.setSingleLine(true);
        field.setMinHeight(dp(context, 56));
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setPadding(0, dp(context, 14), 0, dp(context, 14));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        applyCursor(field);
        row.addView(field, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        final TextView toggle = new TextView(context);
        labelStyle(toggle, 13, primary(context), true);
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
                Context c = v.getContext();
                bg.setStroke(dp(c, hasFocus ? 2 : 1),
                        hasFocus ? primary(c) : outlineVariant(c));
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
        pb.setIndeterminateTintList(ColorStateList.valueOf(primary(context)));
        pb.getIndeterminateDrawable().setColorFilter(primary(context), PorterDuff.Mode.SRC_IN);
        return pb;
    }

    // ---- Janela (bottom sheet) -------------------------------------------
    public static LinearLayout sheet(Context context) {
        final LinearLayout sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));
        GradientDrawable g = new GradientDrawable();
        g.setColor(surface(context));
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

    /** Variante night-aware de {@link #checkTint()}. */
    public static ColorStateList checkTint(Context context) {
        return new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{primary(context), outline(context)});
    }

    // ---- Bloco de sucesso (✓ grande) -------------------------------------
    public static LinearLayout successBlock(Context context) {
        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        block.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 12));
        block.setBackground(rounded(successContainer(context), SHAPE_CARD, context));

        CheckBadge check = new CheckBadge(context, onSuccessContainer(context));
        int badgeDp = 72;
        android.widget.LinearLayout.LayoutParams checkLp =
                new android.widget.LinearLayout.LayoutParams(
                        dp(context, badgeDp), dp(context, badgeDp));
        checkLp.gravity = Gravity.CENTER_HORIZONTAL;
        block.addView(check, checkLp);

        TextView caption = new TextView(context);
        caption.setText("Confirmado");
        labelStyle(caption, 14, onSuccessContainer(context), true);
        caption.setAllCaps(false);
        caption.setLetterSpacing(0f);
        caption.setGravity(Gravity.CENTER);
        block.addView(caption);
        return block;
    }

    // ---- Insets (edge-to-edge sem androidx) --------------------------------
    // Defeito corrigido: botões interativos atrás da navbar / zona de gesto
    // "home". A solução NÃO bifurca "3 botões x gesto por swipe": resolve por
    // matemática de insets, que vale para os dois casos, e o maior valor
    // (tappableElement) é justamente o que garante que o controle não caia na
    // faixa de gesto.
    // API 30+: WindowInsets.Type.navigationBars()/systemGestures()/
    // tappableElement()/displayCutout()/ime(). Abaixo de 30: só existe o
    // getSystemWindowInset* (deprecado), que já vem como o pior caso.

    /**
     * Área (em px) onde a UI não pode encostar, já resolvida.
     *
     * <ul>
     *   <li>{@link #left}/{@link #right}: lateral completa (navbar + zona de
     *       gesto + cutout). É o que um controle INTERATIVO encostado na borda
     *       precisa — a faixa de gesto lateral roubaria o toque dele.
     *   <li>{@link #contentSide}: lateral simétrica só com barra + cutout (o
     *       equivalente ao safeDrawing do framework). Conteúdo de tela cheia
     *       usa esta: reservar a faixa de gesto também em portrait deixaria as
     *       telas de formulário estreitas em quase todo aparelho com gesto.
     * </ul>
     * top/bottom já vêm com gesto e teclado embutidos.
     */
    public static final class SafeArea {
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;
        /** Lateral de conteúdo, simétrica (navbar + cutout, sem zona de gesto). */
        public final int contentSide;

        SafeArea(int left, int top, int right, int bottom, int contentSide) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.contentSide = contentSide;
        }
    }

    /**
     * Inset de baixo: maior entre navbar, zona de gesto e elemento clicável —
     * e nunca menor que o teclado (IME), senão o botão de envio fica coberto.
     * Sem bifurcação por tipo de navegação: os valores competem entre si.
     *
     * @param extraGapPx respiro adicional entre o controle e a barra (px, >= 0)
     */
    public static int computeBottomInset(int navBar, int systemGestures, int tappable,
                                         int extraGapPx) {
        return computeBottomInset(navBar, systemGestures, tappable, 0, extraGapPx);
    }

    /** Igual a {@link #computeBottomInset(int, int, int, int)} considerando o IME. */
    public static int computeBottomInset(int navBar, int systemGestures, int tappable,
                                         int ime, int extraGapPx) {
        int base = Math.max(Math.max(px(navBar), px(systemGestures)), px(tappable));
        base = Math.max(base, px(ime));
        return base + px(extraGapPx);
    }

    /** Lado: navbar na lateral, gesto de ida e volta e cutout (notch). */
    public static int computeSideInset(int navBar, int gesture, int cutout) {
        return Math.max(Math.max(px(navBar), px(gesture)), px(cutout));
    }

    /**
     * Versão simétrica (maior dos dois lados), para containers cujo padding
     * horizontal é igual nos dois lados — o conteúdo sai dos dois cantos
     * incômodos com uma conta só. Passe 0 nos gestos para a versão só de
     * barra/cutout (padding de conteúdo).
     */
    public static int computeSideInset(int navBarLeft, int gestureLeft, int cutoutLeft,
                                       int navBarRight, int gestureRight, int cutoutRight) {
        return Math.max(computeSideInset(navBarLeft, gestureLeft, cutoutLeft),
                computeSideInset(navBarRight, gestureRight, cutoutRight));
    }

    /** Topo: status bar ou display cutout (notch) — vence o maior. */
    public static int computeTopInset(int statusBar, int cutout) {
        return Math.max(px(statusBar), px(cutout));
    }

    private static int px(int value) {
        return value > 0 ? value : 0;
    }

    /**
     * Lê o WindowInsets real e devolve a SafeArea resolvida (px). Envolve as
     * funções puras acima — a matemática é testável na JVM, a leitura não.
     */
    public static SafeArea safeArea(WindowInsets insets) {
        if (insets == null) {
            return new SafeArea(0, 0, 0, 0, 0);
        }
        int navL, navT, navR, navB;
        int gesL, gesR, gesB;
        int tapB, imeB;
        int cutL, cutT, cutR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets nav = insets.getInsets(WindowInsets.Type.navigationBars());
            navL = nav.left;
            navT = nav.top;
            navR = nav.right;
            navB = nav.bottom;
            android.graphics.Insets gestures =
                    insets.getInsets(WindowInsets.Type.systemGestures());
            gesL = gestures.left;
            gesR = gestures.right;
            gesB = gestures.bottom;
            tapB = insets.getInsets(WindowInsets.Type.tappableElement()).bottom;
            imeB = insets.getInsets(WindowInsets.Type.ime()).bottom;
            android.graphics.Insets cutout =
                    insets.getInsets(WindowInsets.Type.displayCutout());
            cutL = cutout.left;
            cutT = cutout.top;
            cutR = cutout.right;
        } else {
            navL = insets.getSystemWindowInsetLeft();
            navT = insets.getSystemWindowInsetTop();
            navR = insets.getSystemWindowInsetRight();
            navB = insets.getSystemWindowInsetBottom();
            // Pré-30 não separa navbar de gesto: o systemWindowInset JÁ é o pior
            // caso e ainda inclui o teclado quando ele abre.
            gesL = navL;
            gesR = navR;
            gesB = navB;
            tapB = 0;
            imeB = 0;
            cutL = 0;
            cutT = 0;
            cutR = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && insets.getDisplayCutout() != null) {
                android.view.DisplayCutout cut = insets.getDisplayCutout();
                cutL = cut.getSafeInsetLeft();
                cutT = cut.getSafeInsetTop();
                cutR = cut.getSafeInsetRight();
            }
        }
        return new SafeArea(
                computeSideInset(navL, gesL, cutL),
                computeTopInset(navT, cutT),
                computeSideInset(navR, gesR, cutR),
                computeBottomInset(navB, gesB, tapB, imeB, 0),
                // Conteúdo: sem a faixa de gesto (0) — maior barra/cutout dos dois lados.
                computeSideInset(navL, 0, cutL, navR, 0, cutR));
    }

    /**
     * Padding = base(dp) + inset resolvido nas 4 bordas, reaplicado a cada
     * mudança (teclado, rotação, navbar que aparece some). Lateral simétrica
     * (contentSide) porque os containers de tela cheia são de padding igual
     * nos dois lados.
     */
    public static void edgeToEdge(final View view, final int baseTopDp, final int baseBottomDp,
                                  final int baseSideDp) {
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                SafeArea sa = safeArea(insets);
                Context c = v.getContext();
                v.setPadding(dp(c, baseSideDp) + sa.contentSide,
                        dp(c, baseTopDp) + sa.top,
                        dp(c, baseSideDp) + sa.contentSide,
                        dp(c, baseBottomDp) + sa.bottom);
                return insets;
            }
        });
    }

    /**
     * Barras de sistema na cor de superfície do tema vigente.
     * No night usa superfície escura com ícones claros (limpa as flags
     * LIGHT_*); no light mantém o comportamento anterior.
     */
    public static void surfaceSystemBars(android.app.Activity activity) {
        boolean night = isNight(activity);
        int bar = night ? DARK_SURFACE : SURFACE;
        activity.getWindow().setStatusBarColor(bar);
        activity.getWindow().setNavigationBarColor(bar);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = activity.getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (night) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (android.os.Build.VERSION.SDK_INT
                        >= android.os.Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (android.os.Build.VERSION.SDK_INT
                        >= android.os.Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    /**
     * Barras sobre overlay escuro de câmera (Scan): status translúcido escuro
     * com ícones claros sempre; navegação segue o tema para não sumir o
     * gestual em aparelhos com botões.
     */
    public static void darkOverlaySystemBars(android.app.Activity activity, int statusColor) {
        activity.getWindow().setStatusBarColor(statusColor);
        activity.getWindow().setNavigationBarColor(
                isNight(activity) ? DARK_SURFACE : SURFACE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = activity.getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility()
                    & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (isNight(activity)) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decor.setSystemUiVisibility(flags);
        }
    }
}