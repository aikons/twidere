/*
 *				Twidere - Twitter client for Android
 * 
 * Copyright (C) 2012 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;

import org.mariotaku.twidere.provider.TweetStore.Statuses;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;

public class StaggeredHomeTimelineFragment extends CursorStatusesStaggeredGridFragment {

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (getActivity() == null || !isAdded() || isDetached()) return;
			final String action = intent.getAction();
			if (BROADCAST_HOME_TIMELINE_REFRESHED.equals(action)) {
				setRefreshComplete();
				getLoaderManager().restartLoader(0, null, StaggeredHomeTimelineFragment.this);
			} else if (BROADCAST_HOME_TIMELINE_DATABASE_UPDATED.equals(action)) {
				getLoaderManager().restartLoader(0, null, StaggeredHomeTimelineFragment.this);
			} else if (BROADCAST_TASK_STATE_CHANGED.equals(action)) {
				final AsyncTwitterWrapper twitter = getTwitterWrapper();
				setRefreshing(twitter != null && twitter.isHomeTimelineRefreshing());
			}
		}
	};

	@Override
	public int getStatuses(final long[] account_ids, final long[] max_ids, final long[] since_ids) {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		if (twitter == null) return 0;
		if (max_ids == null) return twitter.refreshAll();
		return twitter.getHomeTimelineAsync(account_ids, max_ids, since_ids);
	}

	@Override
	public void onStart() {
		super.onStart();
		final IntentFilter filter = new IntentFilter(BROADCAST_HOME_TIMELINE_REFRESHED);
		filter.addAction(BROADCAST_ACCOUNT_LIST_DATABASE_UPDATED);
		filter.addAction(BROADCAST_HOME_TIMELINE_DATABASE_UPDATED);
		filter.addAction(BROADCAST_TASK_STATE_CHANGED);
		registerReceiver(mStatusReceiver, filter);
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		setRefreshing(twitter != null && twitter.isHomeTimelineRefreshing());
	}

	@Override
	public void onStop() {
		unregisterReceiver(mStatusReceiver);
		super.onStop();
	}

	@Override
	protected Uri getContentUri() {
		return Statuses.CONTENT_URI;
	}

	@Override
	protected int getNotificationIdToClear() {
		return NOTIFICATION_ID_HOME_TIMELINE;
	}

	@Override
	protected String getPositionKey() {
		return "home_timeline";
	}

	@Override
	protected boolean isFiltersEnabled() {
		final SharedPreferences pref = getSharedPreferences();
		return pref != null && pref.getBoolean(PREFERENCE_KEY_FILTERS_IN_HOME_TIMELINE, true);
	}

}
