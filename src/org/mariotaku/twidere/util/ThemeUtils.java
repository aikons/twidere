package org.mariotaku.twidere.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;

import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;

import java.util.HashMap;

public class ThemeUtils implements Constants {

	private static final String THEME_NAME_TWIDERE = "twidere";
	private static final String THEME_NAME_DARK = "dark";
	private static final String THEME_NAME_LIGHT = "light";
	private static final String THEME_NAME_LIGHT_DARKACTIONBAR = "light_darkactionbar";

	private static final HashMap<String, Integer> THEMES = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> THEMES_SWIPEBACK = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> THEMES_SOLIDBG = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> THEMES_SWIPEBACK_SOLIDBG = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> THEMES_DIALOG = new HashMap<String, Integer>();
	private static final HashMap<String, Integer> THEMES_COMPOSE = new HashMap<String, Integer>();

	static {
		THEMES.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere);
		THEMES.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark);
		THEMES.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light);
		THEMES.put(THEME_NAME_LIGHT_DARKACTIONBAR, R.style.Theme_Twidere_Light_DarkActionBar);
		THEMES_SWIPEBACK.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere_SwipeBack);
		THEMES_SWIPEBACK.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark_SwipeBack);
		THEMES_SWIPEBACK.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light_SwipeBack);
		THEMES_SWIPEBACK.put(THEME_NAME_LIGHT_DARKACTIONBAR, R.style.Theme_Twidere_Light_DarkActionBar_SwipeBack);
		THEMES_SOLIDBG.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere_SolidBackground);
		THEMES_SOLIDBG.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark_SolidBackground);
		THEMES_SOLIDBG.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light_SolidBackground);
		THEMES_SOLIDBG.put(THEME_NAME_LIGHT_DARKACTIONBAR, R.style.Theme_Twidere_Light_DarkActionBar_SolidBackground);
		THEMES_SWIPEBACK_SOLIDBG.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere_SwipeBack_SolidBackground);
		THEMES_SWIPEBACK_SOLIDBG.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark_SwipeBack_SolidBackground);
		THEMES_SWIPEBACK_SOLIDBG.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light_SwipeBack_SolidBackground);
		THEMES_SWIPEBACK_SOLIDBG.put(THEME_NAME_LIGHT_DARKACTIONBAR,
				R.style.Theme_Twidere_Light_DarkActionBar_SwipeBack_SolidBackground);
		THEMES_DIALOG.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere_Light_Dialog);
		THEMES_DIALOG.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark_Dialog);
		THEMES_DIALOG.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light_Dialog);
		THEMES_COMPOSE.put(THEME_NAME_TWIDERE, R.style.Theme_Twidere_Compose);
		THEMES_COMPOSE.put(THEME_NAME_DARK, R.style.Theme_Twidere_Dark_Compose);
		THEMES_COMPOSE.put(THEME_NAME_LIGHT, R.style.Theme_Twidere_Light_Compose);
		THEMES_COMPOSE.put(THEME_NAME_LIGHT_DARKACTIONBAR, R.style.Theme_Twidere_Light_DarkActionBar_Compose);
	}

	private ThemeUtils() {
		throw new AssertionError();
	}

	public static void applyBackground(final View view) {
		if (view == null) return;
		applyBackground(view, getThemeColor(view.getContext()));
	}

	public static void applyBackground(final View view, final int color) {
		if (view == null) return;
		try {
			final Drawable bg = view.getBackground();
			if (bg == null) return;
			final Drawable mutated = bg.mutate();
			if (mutated == null) return;
			mutated.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
			view.invalidate();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static Drawable getActionBarBackground(final Context context) {
		final TypedArray a = context.obtainStyledAttributes(null, new int[] { android.R.attr.background },
				android.R.attr.actionBarStyle, 0);
		final int color = ThemeUtils.getThemeColor(context);
		final Drawable d = a.getDrawable(0);
		if (!(d instanceof LayerDrawable)) return d;
		final LayerDrawable ld = (LayerDrawable) d.mutate();
		final Drawable color_layer = ld.findDrawableByLayerId(R.id.color_layer);
		if (color_layer != null) {
			color_layer.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
		}
		return ld;
	}

	public static int getCardListBackgroundColor(final Context context) {
		final TypedArray a = context.obtainStyledAttributes(new int[] { R.attr.cardListBackgroundColor });
		final int color = a.getColor(0, Color.TRANSPARENT);
		a.recycle();
		return color;
	}

	public static int getComposeThemeResource(final Context context) {
		return getComposeThemeResource(getThemeName(context));
	}

	public static int getComposeThemeResource(final String name) {
		final Integer res = THEMES_COMPOSE.get(name);
		return res != null ? res : R.style.Theme_Twidere_Compose;
	}

	public static int getDialogThemeResource(final Context context) {
		return getDialogThemeResource(getThemeName(context));
	}

	public static int getDialogThemeResource(final String name) {
		final Integer res = THEMES_DIALOG.get(name);
		return res != null ? res : R.style.Theme_Twidere_Dark_Dialog;
	}

	public static Drawable getListMenuOverflowButtonDrawable(final Context context) {
		final TypedArray a = context.obtainStyledAttributes(new int[] { R.attr.listMenuOverflowButton });
		final Drawable d = a.getDrawable(0);
		a.recycle();
		if (d == null)
			return context.getResources().getDrawable(R.drawable.ic_list_menu_moreoverflow_normal_holo_light);
		return d;
	}

	public static int getSwipeBackThemeResource(final Context context) {
		return getSwipeBackThemeResource(getThemeName(context), isSolidBackground(context));
	}

	public static int getSwipeBackThemeResource(final String name, final boolean solid_background) {
		final Integer res = (solid_background ? THEMES_SWIPEBACK_SOLIDBG : THEMES_SWIPEBACK).get(name);
		return res != null ? res : R.style.Theme_Twidere_SwipeBack;
	}

	public static int getTabIconColor(final Context context) {
		return getTabIconColor(getThemeResource(context));
	}

	public static int getTabIconColor(final int res) {
		switch (res) {
			case R.style.Theme_Twidere_Light:
			case R.style.Theme_Twidere_Light_SwipeBack:
			case R.style.Theme_Twidere_Light_SolidBackground:
			case R.style.Theme_Twidere_Light_SwipeBack_SolidBackground:
			case R.style.Theme_Twidere_Light_Compose:
				return 0xC0333333;
		}
		return Color.WHITE;
	}

	@SuppressLint("InlinedApi")
	public static int getThemeColor(final Context context) {
		if (context == null) return Color.TRANSPARENT;
		final int def = context.getResources().getColor(android.R.color.holo_blue_light);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) return def;
		try {
			final TypedArray a = context.obtainStyledAttributes(new int[] { android.R.attr.colorActivatedHighlight });
			final int color = a.getColor(0, def);
			a.recycle();
			return color;
		} catch (final Exception e) {
			return def;
		}
	}

	public static String getThemeName(final Context context) {
		if (context == null) return THEME_NAME_TWIDERE;
		final SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref != null ? pref.getString(PREFERENCE_KEY_THEME, THEME_NAME_TWIDERE) : THEME_NAME_TWIDERE;
	}

	public static int getThemeResource(final Context context) {
		return getThemeResource(getThemeName(context), isSolidBackground(context));
	}

	public static int getThemeResource(final String name, final boolean solid_background) {
		final Integer res = (solid_background ? THEMES_SOLIDBG : THEMES).get(name);
		return res != null ? res : R.style.Theme_Twidere;
	}

	public static boolean isDarkTheme(final Context context) {
		return isDarkTheme(getThemeResource(context));
	}

	public static boolean isDarkTheme(final int res) {
		switch (res) {
			case R.style.Theme_Twidere_Dark:
			case R.style.Theme_Twidere_Dark_SwipeBack:
			case R.style.Theme_Twidere_Dark_SolidBackground:
			case R.style.Theme_Twidere_Dark_SwipeBack_SolidBackground:
			case R.style.Theme_Twidere_Dark_Dialog:
			case R.style.Theme_Twidere_Compose:
				return true;
		}
		return false;
	}

	public static boolean isLightActionBar(final Context context) {
		return isLightActionBar(getThemeResource(context));
	}

	public static boolean isLightActionBar(final int res) {
		switch (res) {
			case R.style.Theme_Twidere_Light:
			case R.style.Theme_Twidere_Light_SwipeBack:
			case R.style.Theme_Twidere_Light_SolidBackground:
			case R.style.Theme_Twidere_Light_SwipeBack_SolidBackground:
			case R.style.Theme_Twidere_Light_Compose:
				return true;
		}
		return false;
	}

	public static boolean isSolidBackground(final Context context) {
		if (context == null) return false;
		final SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref != null ? pref.getBoolean(PREFERENCE_KEY_SOLID_COLOR_BACKGROUND, false) : false;
	}

	public static boolean shouldApplyColorFilter(final Context context) {
		return shouldApplyColorFilter(getThemeResource(context));
	}

	public static boolean shouldApplyColorFilter(final int res) {
		switch (res) {
			case R.style.Theme_Twidere:
			case R.style.Theme_Twidere_SwipeBack:
			case R.style.Theme_Twidere_SolidBackground:
			case R.style.Theme_Twidere_SwipeBack_SolidBackground:
			case R.style.Theme_Twidere_Compose:
				return false;
		}
		return true;
	}

	public static boolean shouldApplyColorFilterToTabIcons(final Context context) {
		return shouldApplyColorFilterToTabIcons(getThemeResource(context));
	}

	public static boolean shouldApplyColorFilterToTabIcons(final int res) {
		return isLightActionBar(res);
	}

}
