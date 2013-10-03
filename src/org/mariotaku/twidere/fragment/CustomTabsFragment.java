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

import static org.mariotaku.twidere.model.CustomTabConfiguration.getTabIconDrawable;
import static org.mariotaku.twidere.model.CustomTabConfiguration.getTabIconObject;
import static org.mariotaku.twidere.model.CustomTabConfiguration.getTabTypeName;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;

import org.mariotaku.popupmenu.PopupMenu;
import org.mariotaku.querybuilder.Columns.Column;
import org.mariotaku.querybuilder.RawItemArray;
import org.mariotaku.querybuilder.Where;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.graphic.DropShadowDrawable;
import org.mariotaku.twidere.model.CustomTabConfiguration;
import org.mariotaku.twidere.model.Panes;
import org.mariotaku.twidere.provider.TweetStore.Tabs;
import org.mariotaku.twidere.view.holder.TwoLineWithIconViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class CustomTabsFragment extends BaseListFragment implements LoaderCallbacks<Cursor>, Panes.Right,
		MultiChoiceModeListener {

	private ContentResolver mResolver;

	private DragSortListView mListView;

	private PopupMenu mPopupMenu;

	private CustomTabsAdapter mAdapter;

	private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (getActivity() == null || !isAdded() || isDetached()) return;
			final String action = intent.getAction();
			if (BROADCAST_TABS_UPDATED.equals(action)) {
				getLoaderManager().restartLoader(0, null, CustomTabsFragment.this);
			}
		}

	};

	@Override
	public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_DELETE: {
				final Where where = Where.in(new Column(Tabs._ID), new RawItemArray(mListView.getCheckedItemIds()));
				mResolver.delete(Tabs.CONTENT_URI, where.getSQL(), null);
				break;
			}
		}
		mode.finish();
		return true;
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		mResolver = getContentResolver();
		final Context context = getActivity();
		mAdapter = new CustomTabsAdapter(context);
		setListAdapter(mAdapter);
		mListView = (DragSortListView) getListView();
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mListView.setMultiChoiceModeListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
			case REQUEST_ADD_TAB: {
				if (resultCode == Activity.RESULT_OK) {
					final ContentValues values = new ContentValues();
					values.put(Tabs.ARGUMENTS, data.getStringExtra(EXTRA_ARGUMENTS));
					values.put(Tabs.NAME, data.getStringExtra(EXTRA_NAME));
					values.put(Tabs.TYPE, data.getStringExtra(EXTRA_TYPE));
					values.put(Tabs.ICON, data.getStringExtra(EXTRA_ICON));
					values.put(Tabs.POSITION, mAdapter.getCount());
					mResolver.insert(Tabs.CONTENT_URI, values);
				}
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
		new MenuInflater(getActivity()).inflate(R.menu.action_multi_select_items, menu);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		return new CursorLoader(getActivity(), Tabs.CONTENT_URI, Tabs.COLUMNS, null, null, Tabs.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_custom_tabs, menu);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.custom_tabs, container, false);
	}

	@Override
	public void onDestroyActionMode(final ActionMode mode) {

	}

	@Override
	public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id,
			final boolean checked) {
		final int count = mListView.getCheckedItemCount();
		mode.setTitle(getResources().getQuantityString(R.plurals.Nitems_selected, count, count));
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			default: {
				final Intent intent = item.getIntent();
				if (intent == null) return false;
				startActivityForResult(intent, REQUEST_ADD_TAB);
				return true;
			}
		}
	}

	@Override
	public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
		return true;
	}

	@Override
	public void onPrepareOptionsMenu(final Menu menu) {
		final Resources res = getResources();
		final MenuItem itemAdd = menu.findItem(R.id.add_submenu);
		if (itemAdd != null && itemAdd.hasSubMenu()) {
			final SubMenu subMenu = itemAdd.getSubMenu();
			subMenu.clear();
			final HashMap<String, CustomTabConfiguration> map = CustomTabConfiguration.getConfiguraionMap();
			for (final Entry<String, CustomTabConfiguration> entry : map.entrySet()) {
				final String type = entry.getKey();
				final CustomTabConfiguration conf = entry.getValue();
				final Intent intent = new Intent(INTENT_ACTION_EDIT_CUSTOM_TAB);
				intent.putExtra(EXTRA_TYPE, type);
				final MenuItem subItem = subMenu.add(conf.getDefaultTitle());
				subItem.setIcon(new DropShadowDrawable(res, res.getDrawable(conf.getDefaultIcon()), 2, 0x80000000));
				subItem.setIntent(intent);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		registerReceiver(mStateReceiver, new IntentFilter(BROADCAST_TABS_UPDATED));
	}

	@Override
	public void onStop() {
		if (mPopupMenu != null) {
			mPopupMenu.dismiss();
		}
		unregisterReceiver(mStateReceiver);
		final ArrayList<Integer> positions = mAdapter.getCursorPositions();
		final Cursor c = mAdapter.getCursor();
		if (positions != null && c != null && !c.isClosed()) {
			final int idIdx = c.getColumnIndex(Tabs._ID);
			for (int i = 0, j = positions.size(); i < j; i++) {
				c.moveToPosition(positions.get(i));
				final long id = c.getLong(idIdx);
				final ContentValues values = new ContentValues();
				values.put(Tabs.POSITION, i);
				final String where = Tabs._ID + " = " + id;
				mResolver.update(Tabs.CONTENT_URI, values, where, null);
			}
		}
		super.onStop();
	}

	public static class CustomTabsAdapter extends SimpleDragSortCursorAdapter implements OnClickListener {

		private final Context mContext;

		private CursorIndices mIndices;

		public CustomTabsAdapter(final Context context) {
			super(context, R.layout.custom_tab_list_item, null, new String[0], new int[0], 0);
			mContext = context;
		}

		@Override
		public void bindView(final View view, final Context context, final Cursor cursor) {
			super.bindView(view, context, cursor);
			final TwoLineWithIconViewHolder holder = (TwoLineWithIconViewHolder) view.getTag();
			// holder.checkbox.setVisibility(View.VISIBLE);
			// holder.checkbox.setCompoundDrawablesWithIntrinsicBounds(0, 0,
			// R.drawable.ic_menu_refresh, 0);
			// holder.checkbox.setOnClickListener(this);
			final String type = cursor.getString(mIndices.type);
			final String name = cursor.getString(mIndices.name), type_name = getTabTypeName(context, type);
			final String icon = cursor.getString(mIndices.icon);
			holder.text1.setText(TextUtils.isEmpty(name) ? name : type_name);
			holder.text2.setText(type_name);
			final Drawable d = getTabIconDrawable(mContext, getTabIconObject(icon));
			if (d != null) {
				holder.icon.setImageDrawable(new DropShadowDrawable(context.getResources(), d, 2, 0x80000000));
			} else {
				holder.icon.setImageResource(R.drawable.ic_tab_list);
			}
		}

		@Override
		public void changeCursor(final Cursor cursor) {
			if (cursor != null) {
				mIndices = new CursorIndices(cursor);
			}
			super.changeCursor(cursor);
		}

		@Override
		public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
			final View view = super.newView(context, cursor, parent);
			final Object tag = view.getTag();
			if (!(tag instanceof TwoLineWithIconViewHolder)) {
				final TwoLineWithIconViewHolder holder = new TwoLineWithIconViewHolder(view);
				view.setTag(holder);
			}
			return view;
		}

		@Override
		public void onClick(final View view) {
			// TODO Auto-generated method stub

		}

		static class CursorIndices {
			final int _id, name, icon, type, arguments;

			CursorIndices(final Cursor mCursor) {
				_id = mCursor.getColumnIndex(Tabs._ID);
				icon = mCursor.getColumnIndex(Tabs.ICON);
				name = mCursor.getColumnIndex(Tabs.NAME);
				type = mCursor.getColumnIndex(Tabs.TYPE);
				arguments = mCursor.getColumnIndex(Tabs.ARGUMENTS);
			}
		}

	}

}
