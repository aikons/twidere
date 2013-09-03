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

package org.mariotaku.twidere.adapter;

import static org.mariotaku.twidere.util.Utils.configBaseAdapter;
import static org.mariotaku.twidere.util.Utils.getAccountColor;
import static org.mariotaku.twidere.util.Utils.getLocalizedNumber;
import static org.mariotaku.twidere.util.Utils.getUserColor;
import static org.mariotaku.twidere.util.Utils.getUserTypeIconRes;

import java.util.List;
import java.util.Locale;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.adapter.iface.IBaseAdapter;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.util.ImageLoaderWrapper;
import org.mariotaku.twidere.util.MultiSelectManager;
import org.mariotaku.twidere.view.holder.UserViewHolder;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

public class ParcelableUsersAdapter extends ArrayAdapter<ParcelableUser> implements IBaseAdapter {

	private final ImageLoaderWrapper mProfileImageLoader;
	private final MultiSelectManager mMultiSelectManager;
	private final Context mContext;
	private final Locale mLocale;

	private boolean mDisplayProfileImage, mShowAccountColor, mMultiSelectEnabled;

	private float mTextSize;

	private int mNameDisplayOption;

	public ParcelableUsersAdapter(final Context context) {
		super(context, R.layout.user_list_item);
		mContext = context;
		mLocale = context.getResources().getConfiguration().locale;
		final TwidereApplication application = TwidereApplication.getInstance(context);
		mProfileImageLoader = application.getImageLoaderWrapper();
		mMultiSelectManager = application.getMultiSelectManager();
		configBaseAdapter(context, this);
	}

	@Override
	public long getItemId(final int position) {
		return getItem(position) != null ? getItem(position).id : -1;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final Object tag = view.getTag();
		UserViewHolder holder = null;
		if (tag instanceof UserViewHolder) {
			holder = (UserViewHolder) tag;
		} else {
			holder = new UserViewHolder(view);
			view.setTag(holder);
		}
		final ParcelableUser user = getItem(position);

		if (mMultiSelectEnabled) {
			holder.setSelected(mMultiSelectManager.isUserSelected(user.id));
		} else {
			holder.setSelected(false);
		}

		holder.setAccountColorEnabled(mShowAccountColor);

		if (mShowAccountColor) {
			holder.setAccountColor(getAccountColor(mContext, user.account_id));
		}

		holder.setUserColor(getUserColor(mContext, user.id));

		holder.setTextSize(mTextSize);
		holder.name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
				getUserTypeIconRes(user.is_verified, user.is_protected), 0);
		switch (mNameDisplayOption) {
			case NAME_DISPLAY_OPTION_CODE_NAME: {
				holder.name.setText(user.name);
				holder.screen_name.setText(null);
				holder.screen_name.setVisibility(View.GONE);
				break;
			}
			case NAME_DISPLAY_OPTION_CODE_SCREEN_NAME: {
				holder.name.setText(user.screen_name);
				holder.screen_name.setText(null);
				holder.screen_name.setVisibility(View.GONE);
				break;
			}
			default: {
				holder.name.setText(user.name);
				holder.screen_name.setText("@" + user.screen_name);
				holder.screen_name.setVisibility(View.VISIBLE);
				break;
			}
		}
		holder.description.setVisibility(TextUtils.isEmpty(user.description_unescaped) ? View.GONE : View.VISIBLE);
		holder.description.setText(user.description_unescaped);
		holder.location.setVisibility(TextUtils.isEmpty(user.location) ? View.GONE : View.VISIBLE);
		holder.location.setText(user.location);
		holder.url.setVisibility(TextUtils.isEmpty(user.url_expanded) ? View.GONE : View.VISIBLE);
		holder.url.setText(user.url_expanded);
		holder.statuses_count.setText(getLocalizedNumber(mLocale, user.statuses_count));
		holder.followers_count.setText(getLocalizedNumber(mLocale, user.followers_count));
		holder.friends_count.setText(getLocalizedNumber(mLocale, user.friends_count));
		holder.profile_image.setVisibility(mDisplayProfileImage ? View.VISIBLE : View.GONE);
		if (mDisplayProfileImage) {
			mProfileImageLoader.displayProfileImage(holder.profile_image, user.profile_image_url);
		}
		return view;
	}

	public void setData(final List<ParcelableUser> data) {
		setData(data, false);
	}

	public void setData(final List<ParcelableUser> data, final boolean clear_old) {
		if (clear_old) {
			clear();
		}
		if (data == null) return;
		for (final ParcelableUser user : data) {
			if (clear_old || findItem(user.id) == null) {
				add(user);
			}
		}
	}

	@Override
	public void setDisplayProfileImage(final boolean display) {
		if (display != mDisplayProfileImage) {
			mDisplayProfileImage = display;
			notifyDataSetChanged();
		}
	}

	@Override
	public void setMultiSelectEnabled(final boolean multi) {
		if (mMultiSelectEnabled != multi) {
			mMultiSelectEnabled = multi;
			notifyDataSetChanged();
		}
	}

	@Override
	public void setNameDisplayOption(final String option) {
		if (NAME_DISPLAY_OPTION_NAME.equals(option)) {
			mNameDisplayOption = NAME_DISPLAY_OPTION_CODE_NAME;
		} else if (NAME_DISPLAY_OPTION_SCREEN_NAME.equals(option)) {
			mNameDisplayOption = NAME_DISPLAY_OPTION_CODE_SCREEN_NAME;
		} else {
			mNameDisplayOption = 0;
		}
	}

	public void setShowAccountColor(final boolean show) {
		if (show != mShowAccountColor) {
			mShowAccountColor = show;
			notifyDataSetChanged();
		}
	}

	@Override
	public void setTextSize(final float text_size) {
		if (text_size != mTextSize) {
			mTextSize = text_size;
			notifyDataSetChanged();
		}
	}
}
