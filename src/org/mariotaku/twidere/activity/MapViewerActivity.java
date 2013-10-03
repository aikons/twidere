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

package org.mariotaku.twidere.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.fragment.NativeMapFragment;
import org.mariotaku.twidere.fragment.WebMapFragment;
import org.mariotaku.twidere.util.MapInterface;

public class MapViewerActivity extends SwipeBackActivity implements Constants {

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_map_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				onBackPressed();
				break;
			}
			case MENU_CENTER: {
				final Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);
				if (!(fragment instanceof MapInterface)) {
					break;
				}
				((MapInterface) fragment).center();
				break;
			}
		}
		return true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		final Uri uri = getIntent().getData();
		if (uri == null || !AUTHORITY_MAP.equals(uri.getAuthority())) {
			finish();
			return;
		}
		final Bundle bundle = new Bundle();
		final String param_lat = uri.getQueryParameter(QUERY_PARAM_LAT);
		final String param_lng = uri.getQueryParameter(QUERY_PARAM_LNG);
		if (param_lat == null || param_lng == null) {
			finish();
			return;
		}
		try {
			bundle.putDouble(EXTRA_LATITUDE, Double.valueOf(param_lat));
			bundle.putDouble(EXTRA_LONGITUDE, Double.valueOf(param_lng));
		} catch (final NumberFormatException e) {
			finish();
			return;
		}
		final Fragment fragment = isNativeMapSupported() ? new NativeMapFragment() : new WebMapFragment();
		fragment.setArguments(bundle);
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(android.R.id.content, fragment).commit();
	}

	private boolean isNativeMapSupported() {
		return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
	}
}
