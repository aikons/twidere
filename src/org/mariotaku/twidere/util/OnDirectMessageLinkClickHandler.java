/*
 * 				Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2013 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util;

import org.mariotaku.twidere.fragment.PhishingLinkWarningDialogFragment;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

public class OnDirectMessageLinkClickHandler extends OnLinkClickHandler {

	private static final String[] SHORT_LINK_SERVICES = new String[] { "bit.ly", "ow.ly", "tinyurl.com", "goo.gl",
			"k6.kz", "is.gd", "tr.im", "x.co", "weepp.ru" };

	public OnDirectMessageLinkClickHandler(final Context context) {
		super(context);
	}

	@Override
	protected void openLink(final String link) {
		if (link == null) return;
		if (!hasShortenedLinks(link)) {
			super.openLink(link);
			return;
		}
		final SharedPreferences prefs = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		if (prefs.getBoolean(PREFERENCE_KEY_PHISHING_LINK_WARNING, true)) {
			final FragmentManager fm = activity.getFragmentManager();
			final DialogFragment fragment = new PhishingLinkWarningDialogFragment();
			final Bundle args = new Bundle();
			args.putParcelable(INTENT_KEY_URI, Uri.parse(link));
			fragment.setArguments(args);
			fragment.show(fm, "phishing_link_warning");
		} else {
			super.openLink(link);
		}

	}

	private boolean hasShortenedLinks(final String link) {
		for (final String short_link_service : SHORT_LINK_SERVICES) {
			if (link.contains(short_link_service)) return true;
		}
		return false;
	}
}
