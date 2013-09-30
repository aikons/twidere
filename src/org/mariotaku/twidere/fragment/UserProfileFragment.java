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

import static android.text.TextUtils.isEmpty;
import static org.mariotaku.twidere.util.ParseUtils.parseLong;
import static org.mariotaku.twidere.util.Utils.addIntentToMenu;
import static org.mariotaku.twidere.util.Utils.clearUserColor;
import static org.mariotaku.twidere.util.Utils.formatToLongTimeString;
import static org.mariotaku.twidere.util.Utils.getAccountColor;
import static org.mariotaku.twidere.util.Utils.getAccountScreenName;
import static org.mariotaku.twidere.util.Utils.getErrorMessage;
import static org.mariotaku.twidere.util.Utils.getLocalizedNumber;
import static org.mariotaku.twidere.util.Utils.getOriginalTwitterProfileImage;
import static org.mariotaku.twidere.util.Utils.getTwitterInstance;
import static org.mariotaku.twidere.util.Utils.getUserColor;
import static org.mariotaku.twidere.util.Utils.getUserTypeIconRes;
import static org.mariotaku.twidere.util.Utils.isMyAccount;
import static org.mariotaku.twidere.util.Utils.openImage;
import static org.mariotaku.twidere.util.Utils.openIncomingFriendships;
import static org.mariotaku.twidere.util.Utils.openSavedSearches;
import static org.mariotaku.twidere.util.Utils.openStatus;
import static org.mariotaku.twidere.util.Utils.openTweetSearch;
import static org.mariotaku.twidere.util.Utils.openUserBlocks;
import static org.mariotaku.twidere.util.Utils.openUserFavorites;
import static org.mariotaku.twidere.util.Utils.openUserFollowers;
import static org.mariotaku.twidere.util.Utils.openUserFriends;
import static org.mariotaku.twidere.util.Utils.openUserListMemberships;
import static org.mariotaku.twidere.util.Utils.openUserLists;
import static org.mariotaku.twidere.util.Utils.openUserMentions;
import static org.mariotaku.twidere.util.Utils.openUserProfile;
import static org.mariotaku.twidere.util.Utils.openUserTimeline;
import static org.mariotaku.twidere.util.Utils.setUserColor;
import static org.mariotaku.twidere.util.Utils.showInfoMessage;

import java.util.Locale;

import org.mariotaku.popupmenu.PopupMenu;
import org.mariotaku.popupmenu.PopupMenu.OnMenuItemClickListener;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.ColorSelectorActivity;
import org.mariotaku.twidere.activity.EditUserProfileActivity;
import org.mariotaku.twidere.activity.UserListSelectorActivity;
import org.mariotaku.twidere.adapter.ListActionAdapter;
import org.mariotaku.twidere.loader.ParcelableUserLoader;
import org.mariotaku.twidere.model.ListAction;
import org.mariotaku.twidere.model.Panes;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.model.ParcelableUserList;
import org.mariotaku.twidere.model.SingleResponse;
import org.mariotaku.twidere.provider.TweetStore.Accounts;
import org.mariotaku.twidere.provider.TweetStore.CachedUsers;
import org.mariotaku.twidere.provider.TweetStore.Filters;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;
import org.mariotaku.twidere.util.ImageLoaderWrapper;
import org.mariotaku.twidere.util.ThemeUtils;
import org.mariotaku.twidere.util.TwidereLinkify;
import org.mariotaku.twidere.util.TwidereLinkify.OnLinkClickListener;
import org.mariotaku.twidere.view.ColorLabelRelativeLayout;
import org.mariotaku.twidere.view.ProfileImageBannerLayout;
import org.mariotaku.twidere.view.iface.IExtendedView.OnSizeChangedListener;

import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class UserProfileFragment extends BaseSupportListFragment implements OnClickListener, OnItemClickListener,
		OnItemLongClickListener, OnMenuItemClickListener, OnLinkClickListener, Panes.Right, OnSizeChangedListener {

	private static final int LOADER_ID_USER = 1;
	private static final int LOADER_ID_FRIENDSHIP = 2;

	private ImageLoaderWrapper mProfileImageLoader;
	private SharedPreferences mPreferences;

	private ImageView mProfileImageView, mProfileBannerView;
	private TextView mNameView, mScreenNameView, mDescriptionView, mLocationView, mURLView, mCreatedAtView,
			mTweetCount, mFollowersCount, mFriendsCount, mFollowingYouIndicator, mErrorMessageView;
	private View mNameContainer, mDescriptionContainer, mLocationContainer, mURLContainer, mTweetsContainer,
			mFollowersContainer, mFriendsContainer, mEditFollowContainer, mMoreOptionsContainer;
	private ProgressBar mFollowProgress, mMoreOptionsProgress;
	private Button mEditFollowButton, mMoreOptionsButton, mRetryButton;
	private ColorLabelRelativeLayout mProfileNameContainer;
	private ProfileImageBannerLayout mProfileImageBannerLayout;
	private ListView mListView;
	private View mHeaderView;

	private ListActionAdapter mAdapter;

	private long mAccountId;
	private Relationship mFriendship;
	private ParcelableUser mUser = null;
	private Locale mLocale;

	private View mListContainer, mErrorRetryContainer;

	private boolean mGetUserInfoLoaderInitialized, mGetFriendShipLoaderInitialized;

	private long mUserId;
	private String mScreenName;
	private int mBannerWidth;

	private AsyncTwitterWrapper mTwitterWrapper;

	private PopupMenu mFollowPopupMenu, mOptionsPopupMenu;

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (getActivity() == null || !isAdded() || isDetached()) return;
			if (mUser == null) return;
			final String action = intent.getAction();
			if (BROADCAST_FRIENDSHIP_CHANGED.equals(action)) {
				if (intent.getLongExtra(INTENT_KEY_USER_ID, -1) == mUser.id
						&& intent.getBooleanExtra(INTENT_KEY_SUCCEED, false)) {
					getFriendship();
				}
			}
			if (BROADCAST_BLOCKSTATE_CHANGED.equals(action)) {
				if (intent.getLongExtra(INTENT_KEY_USER_ID, -1) == mUser.id
						&& intent.getBooleanExtra(INTENT_KEY_SUCCEED, false)) {
					getFriendship();
				}
			}
			if (BROADCAST_PROFILE_UPDATED.equals(action) || BROADCAST_PROFILE_IMAGE_UPDATED.equals(action)
					|| BROADCAST_PROFILE_BANNER_UPDATED.equals(action)) {
				if (intent.getLongExtra(INTENT_KEY_USER_ID, -1) == mUser.id
						&& intent.getBooleanExtra(INTENT_KEY_SUCCEED, false)) {
					getUserInfo(true);
				}
			}
		}
	};

	private final LoaderCallbacks<SingleResponse<ParcelableUser>> mUserInfoLoaderCallbacks = new LoaderCallbacks<SingleResponse<ParcelableUser>>() {

		@Override
		public Loader<SingleResponse<ParcelableUser>> onCreateLoader(final int id, final Bundle args) {
			mListContainer.setVisibility(View.VISIBLE);
			mErrorRetryContainer.setVisibility(View.GONE);
			mErrorMessageView.setText(null);
			mErrorMessageView.setVisibility(View.GONE);
			setListShown(mUser != null);
			setProgressBarIndeterminateVisibility(true);
			final boolean omit_intent_extra = args != null ? args.getBoolean(INTENT_KEY_OMIT_INTENT_EXTRA, true) : true;
			return new ParcelableUserLoader(getActivity(), mAccountId, mUserId, mScreenName, getArguments(),
					omit_intent_extra, mUser == null || !mUser.is_cache && mUserId != mUser.id);
		}

		@Override
		public void onLoaderReset(final Loader<SingleResponse<ParcelableUser>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<SingleResponse<ParcelableUser>> loader,
				final SingleResponse<ParcelableUser> data) {
			if (getActivity() == null) return;
			if (data.data != null && data.data.id > 0) {
				setListShown(true);
				displayUser(data.data);
				mErrorRetryContainer.setVisibility(View.GONE);
				if (data.data.is_cache) {
					getLoaderManager().restartLoader(LOADER_ID_USER, null, this);
				}
			} else if (mUser != null && mUser.is_cache
					&& (mUserId == mUser.id || mScreenName != null && mScreenName.equals(mUser.screen_name))) {
				setListShown(true);
				displayUser(mUser);
			} else {
				if (data.exception != null) {
					mErrorMessageView.setText(getErrorMessage(getActivity(), data.exception));
					mErrorMessageView.setVisibility(View.VISIBLE);
				}
				mListContainer.setVisibility(View.GONE);
				mErrorRetryContainer.setVisibility(View.VISIBLE);
			}
			setProgressBarIndeterminateVisibility(false);
		}

	};

	private final LoaderCallbacks<SingleResponse<Relationship>> mFriendshipLoaderCallbacks = new LoaderCallbacks<SingleResponse<Relationship>>() {

		@Override
		public Loader<SingleResponse<Relationship>> onCreateLoader(final int id, final Bundle args) {
			final boolean user_is_me = mUserId == mAccountId;
			mFollowingYouIndicator.setVisibility(View.GONE);
			mEditFollowContainer.setVisibility(View.VISIBLE);
			mMoreOptionsContainer.setVisibility(user_is_me ? View.VISIBLE : View.GONE);
			mEditFollowButton.setVisibility(user_is_me ? View.VISIBLE : View.GONE);
			mFollowProgress.setVisibility(user_is_me ? View.GONE : View.VISIBLE);
			mMoreOptionsButton.setVisibility(View.GONE);
			mMoreOptionsProgress.setVisibility(user_is_me ? View.GONE : View.VISIBLE);
			mEditFollowButton.setText(user_is_me ? R.string.edit : R.string.loading);
			return new FriendshipLoader(getActivity(), mAccountId, mUserId);
		}

		@Override
		public void onLoaderReset(final Loader<SingleResponse<Relationship>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<SingleResponse<Relationship>> loader,
				final SingleResponse<Relationship> data) {
			mFriendship = null;
			if (mUser == null) return;
			final boolean user_is_me = mAccountId == mUserId;
			if (data.data != null) {
				mFriendship = data.data;
				final boolean followed_by_user = data.data.isTargetFollowingSource();
				mEditFollowButton.setVisibility(View.VISIBLE);
				if (data.data.isSourceFollowingTarget()) {
					mEditFollowButton.setText(R.string.unfollow);
				} else {
					if (mUser.is_protected) {
						mEditFollowButton.setText(mUser.is_follow_request_sent ? R.string.follow_request_sent
								: R.string.send_follow_request);
					} else {
						mEditFollowButton.setText(R.string.follow);
					}
				}
				mFollowingYouIndicator.setVisibility(followed_by_user && !user_is_me ? View.VISIBLE : View.GONE);
				final ContentResolver resolver = getContentResolver();
				final String where = CachedUsers.USER_ID + " = " + mUserId;
				resolver.delete(CachedUsers.CONTENT_URI, where, null);
				// I bet you don't want to see blocked user in your auto
				// complete list.
				if (!data.data.isSourceBlockingTarget()) {
					final ContentValues cached_values = ParcelableUser.makeCachedUserContentValues(mUser);
					if (cached_values != null) {
						resolver.insert(CachedUsers.CONTENT_URI, cached_values);
					}
				}
			}
			mEditFollowContainer.setVisibility(data.data == null && !user_is_me ? View.GONE : View.VISIBLE);
			mMoreOptionsContainer.setVisibility(data.data == null && !user_is_me ? View.GONE : View.VISIBLE);
			mMoreOptionsButton.setVisibility(data.data != null || user_is_me ? View.VISIBLE : View.GONE);
			mFollowProgress.setVisibility(View.GONE);
			mMoreOptionsProgress.setVisibility(View.GONE);
		}

	};

	public void displayUser(final ParcelableUser user) {
		mFriendship = null;
		mUser = null;
		mUserId = -1;
		mAccountId = -1;
		mAdapter.clear();
		if (user == null || user.id <= 0 || getActivity() == null) return;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		final boolean user_is_me = user.account_id == user.id;
		mErrorRetryContainer.setVisibility(View.GONE);
		mAccountId = user.account_id;
		mUser = user;
		mUserId = user.id;
		mScreenName = user.screen_name;
		mProfileNameContainer.drawStart(getUserColor(getActivity(), mUserId));
		mProfileNameContainer.drawEnd(getAccountColor(getActivity(), user.account_id));
		mNameView.setText(user.name);
		mNameView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
				getUserTypeIconRes(user.is_verified, user.is_protected), 0);
		mScreenNameView.setText("@" + user.screen_name);
		mDescriptionContainer.setVisibility(user_is_me || !isEmpty(user.description_html) ? View.VISIBLE : View.GONE);
		mDescriptionView.setText(user.description_html != null ? Html.fromHtml(user.description_html) : null);
		final TwidereLinkify mLinkify = new TwidereLinkify(this);
		mLinkify.applyAllLinks(mDescriptionView, user.account_id, false);
		mDescriptionView.setMovementMethod(null);
		mLocationContainer.setVisibility(user_is_me || !isEmpty(user.location) ? View.VISIBLE : View.GONE);
		mLocationView.setText(user.location);
		mURLContainer.setVisibility(user_is_me || !isEmpty(user.url) || !isEmpty(user.url_expanded) ? View.VISIBLE
				: View.GONE);
		mURLView.setText(isEmpty(user.url_expanded) ? user.url : user.url_expanded);
		mURLView.setMovementMethod(null);
		final String created_at = formatToLongTimeString(getActivity(), user.created_at);
		final double total_created_days = (System.currentTimeMillis() - user.created_at) / 1000 / 60 / 60 / 24;
		final long daily_tweets = Math.round(user.statuses_count / Math.max(1, total_created_days));
		mCreatedAtView.setText(getString(R.string.daily_statuses_count, created_at, daily_tweets));
		mTweetCount.setText(getLocalizedNumber(mLocale, user.statuses_count));
		mFollowersCount.setText(getLocalizedNumber(mLocale, user.followers_count));
		mFriendsCount.setText(getLocalizedNumber(mLocale, user.friends_count));
		if (mPreferences.getBoolean(PREFERENCE_KEY_DISPLAY_PROFILE_IMAGE, true)) {
			mProfileImageLoader.displayProfileImage(mProfileImageView, user.profile_image_url);
			final int def_width = getResources().getDisplayMetrics().widthPixels;
			final int width = mBannerWidth > 0 ? mBannerWidth : def_width;
			mProfileBannerView.setImageBitmap(null);
			mProfileImageLoader.displayProfileBanner(mProfileBannerView, user.profile_banner_url, width);
		} else {
			mProfileImageView.setImageResource(R.drawable.ic_profile_image_default);
			mProfileBannerView.setImageResource(android.R.color.transparent);
		}
		if (isMyAccount(getActivity(), user.id)) {
			final ContentResolver resolver = getContentResolver();
			final ContentValues values = new ContentValues();
			values.put(Accounts.NAME, user.name);
			values.put(Accounts.SCREEN_NAME, user.screen_name);
			values.put(Accounts.PROFILE_IMAGE_URL, user.profile_image_url);
			values.put(Accounts.PROFILE_BANNER_URL, user.profile_banner_url);
			final String where = Accounts.ACCOUNT_ID + " = " + user.id;
			resolver.update(Accounts.CONTENT_URI, values, where, null);
		}
		mAdapter.add(new FavoritesAction(1));
		mAdapter.add(new UserMentionsAction(2));
		mAdapter.add(new UserListsAction(3));
		mAdapter.add(new UserListMembershipsAction(4));
		if (user_is_me) {
			mAdapter.add(new SavedSearchesAction(11));
			if (user.is_protected) {
				mAdapter.add(new IncomingFriendshipsAction(12));
			}
			mAdapter.add(new UserBlocksAction(13));
		}
		mAdapter.notifyDataSetChanged();
		if (!user.is_cache) {
			getFriendship();
		}
	}

	public void getUserInfo(final long account_id, final long user_id, final String screen_name,
			final boolean omit_intent_extra) {
		mAccountId = account_id;
		mUserId = user_id;
		mScreenName = screen_name;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		if (!isMyAccount(getActivity(), mAccountId)) {
			mListContainer.setVisibility(View.GONE);
			mErrorRetryContainer.setVisibility(View.GONE);
			return;
		}
		final Bundle args = new Bundle();
		args.putBoolean(INTENT_KEY_OMIT_INTENT_EXTRA, omit_intent_extra);
		if (!mGetUserInfoLoaderInitialized) {
			lm.initLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
			mGetUserInfoLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_USER, args, mUserInfoLoaderCallbacks);
		}
		if (account_id == -1 || user_id == -1 && screen_name == null) {
			mListContainer.setVisibility(View.GONE);
			mErrorRetryContainer.setVisibility(View.GONE);
			return;
		}
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		mTwitterWrapper = getApplication().getTwitterWrapper();
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mLocale = getResources().getConfiguration().locale;
		super.onActivityCreated(savedInstanceState);
		final Bundle args = getArguments();
		long account_id = -1, user_id = -1;
		String screen_name = null;
		if (args != null) {
			if (savedInstanceState != null) {
				args.putAll(savedInstanceState);
			}
			account_id = args.getLong(INTENT_KEY_ACCOUNT_ID, -1);
			user_id = args.getLong(INTENT_KEY_USER_ID, -1);
			screen_name = args.getString(INTENT_KEY_SCREEN_NAME);
		}
		mProfileImageLoader = getApplication().getImageLoaderWrapper();
		mAdapter = new ListActionAdapter(getActivity());
		mProfileImageView.setOnClickListener(this);
		mProfileBannerView.setOnClickListener(this);
		mNameContainer.setOnClickListener(this);
		mEditFollowButton.setOnClickListener(this);
		mTweetsContainer.setOnClickListener(this);
		mFollowersContainer.setOnClickListener(this);
		mFriendsContainer.setOnClickListener(this);
		mRetryButton.setOnClickListener(this);
		mMoreOptionsButton.setOnClickListener(this);
		mProfileImageBannerLayout.setOnSizeChangedListener(this);
		setListAdapter(null);
		mListView = getListView();
		mListView.addHeaderView(mHeaderView, null, false);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		setListAdapter(mAdapter);
		getUserInfo(account_id, user_id, screen_name, false);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (intent == null) return;
		switch (requestCode) {
			case REQUEST_SET_COLOR: {
				if (resultCode == Activity.RESULT_OK && intent != null) {
					final int color = intent.getIntExtra(Accounts.USER_COLOR, Color.TRANSPARENT);
					setUserColor(getActivity(), mUserId, color);
					mProfileNameContainer.drawStart(getUserColor(getActivity(), mUserId));
				}
				break;
			}
			case REQUEST_ADD_TO_LIST: {
				if (resultCode == Activity.RESULT_OK && intent != null) {
					final ParcelableUserList list = intent.getParcelableExtra(INTENT_KEY_USER_LIST);
					if (list == null) return;
					mTwitterWrapper.addUserListMembers(mAccountId, list.id, mUserId);
				}
				break;
			}
		}

	}

	@Override
	public void onClick(final View view) {
		if (getActivity() == null) return;
		switch (view.getId()) {
			case R.id.edit_follow: {
				if (mUser == null || mAccountId <= 0) return;
				if (mAccountId == mUserId) {
					final Bundle bundle = getArguments();
					final Intent intent = new Intent(INTENT_ACTION_EDIT_USER_PROFILE);
					intent.setClass(getActivity(), EditUserProfileActivity.class);
					if (bundle != null) {
						intent.putExtras(bundle);
					}
					startActivity(intent);
				} else {
					if (mUser.is_follow_request_sent) return;
					if (mFriendship.isSourceFollowingTarget()) {
						mFollowPopupMenu = PopupMenu.getInstance(getActivity(), view);
						mFollowPopupMenu.inflate(R.menu.action_user_profile_follow);
						mFollowPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(final MenuItem item) {
								switch (item.getItemId()) {
									case R.id.unfollow: {
										mFollowProgress.setVisibility(View.VISIBLE);
										mEditFollowButton.setVisibility(View.GONE);
										mTwitterWrapper.destroyFriendship(mAccountId, mUser.id);
										return true;
									}
								}
								return false;
							}
						});
						mFollowPopupMenu.show();
					} else {
						mFollowProgress.setVisibility(View.VISIBLE);
						mEditFollowButton.setVisibility(View.GONE);
						mTwitterWrapper.createFriendship(mAccountId, mUser.id);
					}
				}
				break;
			}
			case R.id.retry: {
				getUserInfo(true);
				break;
			}
			case ProfileImageBannerLayout.VIEW_ID_PROFILE_IMAGE: {
				final String profile_image_url_string = getOriginalTwitterProfileImage(mUser.profile_image_url);
				openImage(getActivity(), profile_image_url_string, null, false);
				break;
			}
			case ProfileImageBannerLayout.VIEW_ID_PROFILE_BANNER: {
				final String profile_banner_url = mUser.profile_banner_url;
				if (profile_banner_url == null) return;
				openImage(getActivity(), profile_banner_url + "/ipad_retina", null, false);
				break;
			}
			case R.id.tweets_container: {
				if (mUser == null) return;
				openUserTimeline(getActivity(), mAccountId, mUser.id, mUser.screen_name);
				break;
			}
			case R.id.followers_container: {
				if (mUser == null) return;
				openUserFollowers(getActivity(), mAccountId, mUser.id, mUser.screen_name);
				break;
			}
			case R.id.friends_container: {
				if (mUser == null) return;
				openUserFriends(getActivity(), mAccountId, mUser.id, mUser.screen_name);
				break;
			}
			case R.id.more_options: {
				if (mUser == null) return;
				mOptionsPopupMenu = PopupMenu.getInstance(getActivity(), view);
				mOptionsPopupMenu.inflate(R.menu.action_user_profile);
				final Menu menu = mOptionsPopupMenu.getMenu();
				if (mUser.id != mAccountId) {
					if (mFriendship == null) return;
					final MenuItem blockItem = menu.findItem(MENU_BLOCK);
					if (blockItem != null) {
						final Drawable blockIcon = blockItem.getIcon();
						if (mFriendship.isSourceBlockingTarget()) {
							blockItem.setTitle(R.string.unblock);
							blockIcon.mutate().setColorFilter(ThemeUtils.getThemeColor(getActivity()),
									PorterDuff.Mode.MULTIPLY);
						} else {
							blockItem.setTitle(R.string.block);
							blockIcon.clearColorFilter();
						}
					}
					final MenuItem sendDirectMessageItem = menu.findItem(MENU_SEND_DIRECT_MESSAGE);
					if (sendDirectMessageItem != null) {
						sendDirectMessageItem.setVisible(mFriendship.isTargetFollowingSource());
					}
				} else {
					final int size = menu.size();
					for (int i = 0; i < size; i++) {
						final MenuItem item = menu.getItem(i);
						final int id = item.getItemId();
						item.setVisible(id == R.id.set_color_submenu || id == 0);
					}
				}
				final Intent intent = new Intent(INTENT_ACTION_EXTENSION_OPEN_USER);
				final Bundle extras = new Bundle();
				extras.putParcelable(INTENT_KEY_USER, mUser);
				intent.putExtras(extras);
				addIntentToMenu(getActivity(), menu, intent);
				mOptionsPopupMenu.setOnMenuItemClickListener(this);
				mOptionsPopupMenu.show();
				break;
			}
			case R.id.name_container: {
				if (mUser == null || mAccountId != mUserId) return;
				startActivity(new Intent(getActivity(), EditUserProfileActivity.class));
				break;
			}
		}

	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mHeaderView = inflater.inflate(R.layout.user_profile_header, null, false);
		mNameContainer = mHeaderView.findViewById(R.id.name_container);
		mNameView = (TextView) mHeaderView.findViewById(R.id.name);
		mScreenNameView = (TextView) mHeaderView.findViewById(R.id.screen_name);
		mDescriptionView = (TextView) mHeaderView.findViewById(R.id.description);
		mLocationView = (TextView) mHeaderView.findViewById(R.id.location);
		mURLView = (TextView) mHeaderView.findViewById(R.id.url);
		mCreatedAtView = (TextView) mHeaderView.findViewById(R.id.created_at);
		mTweetsContainer = mHeaderView.findViewById(R.id.tweets_container);
		mTweetCount = (TextView) mHeaderView.findViewById(R.id.statuses_count);
		mFollowersContainer = mHeaderView.findViewById(R.id.followers_container);
		mFollowersCount = (TextView) mHeaderView.findViewById(R.id.followers_count);
		mFriendsContainer = mHeaderView.findViewById(R.id.friends_container);
		mFriendsCount = (TextView) mHeaderView.findViewById(R.id.friends_count);
		mProfileNameContainer = (ColorLabelRelativeLayout) mHeaderView.findViewById(R.id.profile_name_container);
		mProfileImageBannerLayout = (ProfileImageBannerLayout) mHeaderView.findViewById(R.id.profile_image_banner);
		mProfileImageView = mProfileImageBannerLayout.getProfileImageView();
		mProfileBannerView = mProfileImageBannerLayout.getProfileBannerImageView();
		mDescriptionContainer = mHeaderView.findViewById(R.id.description_container);
		mLocationContainer = mHeaderView.findViewById(R.id.location_container);
		mURLContainer = mHeaderView.findViewById(R.id.url_container);
		mEditFollowContainer = mHeaderView.findViewById(R.id.edit_follow_container);
		mEditFollowButton = (Button) mHeaderView.findViewById(R.id.edit_follow);
		mFollowProgress = (ProgressBar) mHeaderView.findViewById(R.id.follow_progress);
		mMoreOptionsContainer = mHeaderView.findViewById(R.id.more_options_container);
		mMoreOptionsButton = (Button) mHeaderView.findViewById(R.id.more_options);
		mMoreOptionsProgress = (ProgressBar) mHeaderView.findViewById(R.id.more_options_progress);
		mFollowingYouIndicator = (TextView) mHeaderView.findViewById(R.id.following_you_indicator);
		mListContainer = super.onCreateView(inflater, container, savedInstanceState);
		final View container_view = inflater.inflate(R.layout.list_with_error_message, null);
		((FrameLayout) container_view.findViewById(R.id.list_container)).addView(mListContainer);
		mErrorRetryContainer = container_view.findViewById(R.id.error_retry_container);
		mRetryButton = (Button) container_view.findViewById(R.id.retry);
		mErrorMessageView = (TextView) container_view.findViewById(R.id.error_message);
		return container_view;
	}

	@Override
	public void onDestroyView() {
		mUser = null;
		mFriendship = null;
		mAccountId = -1;
		mUserId = -1;
		mScreenName = null;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_USER);
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		super.onDestroyView();
	}

	@Override
	public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
		final ListAction action = mAdapter.findItem(id);
		if (action != null) {
			action.onClick();
		}
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> adapter, final View view, final int position, final long id) {
		final ListAction action = mAdapter.findItem(id);
		if (action != null) return action.onLongClick();
		return false;
	}

	@Override
	public void onLinkClick(final String link, final String orig, final long account_id, final int type,
			final boolean sensitive) {
		if (mUser == null) return;
		switch (type) {
			case TwidereLinkify.LINK_TYPE_MENTION: {
				openUserProfile(getActivity(), mAccountId, -1, link);
				break;
			}
			case TwidereLinkify.LINK_TYPE_HASHTAG: {
				openTweetSearch(getActivity(), mAccountId, link);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LINK_WITH_IMAGE_EXTENSION: {
				openImage(getActivity(), link, orig, false);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LINK: {
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
				startActivity(intent);
				break;
			}
			case TwidereLinkify.LINK_TYPE_LIST: {
				final String[] mention_list = link.split("\\/");
				if (mention_list == null || mention_list.length != 2) {
					break;
				}
				break;
			}
			case TwidereLinkify.LINK_TYPE_STATUS: {
				openStatus(getActivity(), account_id, parseLong(link));
				break;
			}
		}
	}

	@Override
	public boolean onMenuItemClick(final MenuItem item) {
		if (mUser == null || mTwitterWrapper == null) return false;
		switch (item.getItemId()) {
			case MENU_BLOCK: {
				if (mTwitterWrapper == null || mFriendship == null) {
					break;
				}
				if (mFriendship.isSourceBlockingTarget()) {
					mTwitterWrapper.destroyBlock(mAccountId, mUser.id);
				} else {
					mTwitterWrapper.createBlockAsync(mAccountId, mUser.id);
				}
				break;
			}
			case MENU_REPORT_SPAM: {
				mTwitterWrapper.reportSpam(mAccountId, mUser.id);
				break;
			}
			case MENU_MUTE_USER: {
				final String screen_name = mUser.screen_name;
				final Uri uri = Filters.Users.CONTENT_URI;
				final ContentValues values = new ContentValues();
				final ContentResolver resolver = getContentResolver();
				values.put(Filters.Users.VALUE, screen_name);
				resolver.delete(uri, Filters.Users.VALUE + " = ?", new String[] { screen_name });
				resolver.insert(uri, values);
				showInfoMessage(getActivity(), R.string.user_muted, false);
				break;
			}
			case MENU_MENTION: {
				final Intent intent = new Intent(INTENT_ACTION_MENTION);
				final Bundle bundle = new Bundle();
				bundle.putParcelable(INTENT_KEY_USER, mUser);
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			}
			case MENU_SEND_DIRECT_MESSAGE: {
				final Uri.Builder builder = new Uri.Builder();
				builder.scheme(SCHEME_TWIDERE);
				builder.authority(AUTHORITY_DIRECT_MESSAGES_CONVERSATION);
				builder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(mAccountId));
				builder.appendQueryParameter(QUERY_PARAM_CONVERSATION_ID, String.valueOf(mUser.id));
				startActivity(new Intent(Intent.ACTION_VIEW, builder.build()));
				break;
			}
			case MENU_SET_COLOR: {
				final Intent intent = new Intent(getActivity(), ColorSelectorActivity.class);
				startActivityForResult(intent, REQUEST_SET_COLOR);
				break;
			}
			case MENU_CLEAR_COLOR: {
				clearUserColor(getActivity(), mUserId);
				mProfileNameContainer.drawStart(getUserColor(getActivity(), mUserId));
				break;
			}
			case MENU_ADD_TO_LIST: {
				final Bundle extras = new Bundle();
				extras.putLong(INTENT_KEY_ACCOUNT_ID, mAccountId);
				extras.putString(INTENT_KEY_SCREEN_NAME, getAccountScreenName(getActivity(), mAccountId));
				final Intent intent = new Intent(INTENT_ACTION_SELECT_USER_LIST);
				intent.setClass(getActivity(), UserListSelectorActivity.class);
				intent.putExtras(extras);
				startActivityForResult(intent, REQUEST_ADD_TO_LIST);
				break;
			}
			default: {
				if (item.getIntent() != null) {
					try {
						startActivity(item.getIntent());
					} catch (final ActivityNotFoundException e) {
						Log.w(LOGTAG, e);
						return false;
					}
				}
				break;
			}
		}
		return true;
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		outState.putLong(INTENT_KEY_ACCOUNT_ID, mAccountId);
		outState.putLong(INTENT_KEY_USER_ID, mUserId);
		outState.putString(INTENT_KEY_SCREEN_NAME, mScreenName);
		outState.putParcelable(INTENT_KEY_USER, mUser);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onSizeChanged(final View view, final int w, final int h, final int oldw, final int oldh) {
		mBannerWidth = w;
	}

	@Override
	public void onStart() {
		super.onStart();
		final IntentFilter filter = new IntentFilter(BROADCAST_FRIENDSHIP_CHANGED);
		filter.addAction(BROADCAST_BLOCKSTATE_CHANGED);
		filter.addAction(BROADCAST_PROFILE_UPDATED);
		filter.addAction(BROADCAST_PROFILE_IMAGE_UPDATED);
		filter.addAction(BROADCAST_PROFILE_BANNER_UPDATED);
		registerReceiver(mStatusReceiver, filter);
		mProfileNameContainer.drawStart(getUserColor(getActivity(), mUserId));
	}

	@Override
	public void onStop() {
		unregisterReceiver(mStatusReceiver);
		super.onStop();
	}

	private void getFriendship() {
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_FRIENDSHIP);
		if (!mGetFriendShipLoaderInitialized) {
			lm.initLoader(LOADER_ID_FRIENDSHIP, null, mFriendshipLoaderCallbacks);
			mGetFriendShipLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_FRIENDSHIP, null, mFriendshipLoaderCallbacks);
		}
	}

	private void getUserInfo(final boolean omit_intent_extra) {
		getUserInfo(mAccountId, mUserId, mScreenName, omit_intent_extra);
	}

	final class FavoritesAction extends ListAction {

		public FavoritesAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.favorites);
		}

		@Override
		public String getSummary() {
			if (mUser == null) return null;
			return getLocalizedNumber(mLocale, mUser.favorites_count);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openUserFavorites(getActivity(), mAccountId, mUser.id, mUser.screen_name);
		}

	}

	static class FriendshipLoader extends AsyncTaskLoader<SingleResponse<Relationship>> {

		private final Context context;
		private final long account_id, user_id;

		public FriendshipLoader(final Context context, final long account_id, final long user_id) {
			super(context);
			this.context = context;
			this.account_id = account_id;
			this.user_id = user_id;
		}

		@Override
		public SingleResponse<Relationship> loadInBackground() {
			if (account_id == user_id) return new SingleResponse<Relationship>(null, null);
			final Twitter twitter = getTwitterInstance(context, account_id, false);
			if (twitter == null) return new SingleResponse<Relationship>(null, null);
			try {
				final Relationship result = twitter.showFriendship(account_id, user_id);
				return new SingleResponse<Relationship>(result, null);
			} catch (final TwitterException e) {
				return new SingleResponse<Relationship>(null, e);
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}
	}

	final class IncomingFriendshipsAction extends ListAction {

		public IncomingFriendshipsAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.incoming_friendships);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openIncomingFriendships(getActivity(), mAccountId);
		}

	}

	final class SavedSearchesAction extends ListAction {

		public SavedSearchesAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.saved_searches);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openSavedSearches(getActivity(), mAccountId);
		}

	}

	final class UserBlocksAction extends ListAction {

		public UserBlocksAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.blocked_users);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openUserBlocks(getActivity(), mAccountId);
		}

	}

	final class UserListMembershipsAction extends ListAction {
		public UserListMembershipsAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.lists_following_user);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openUserListMemberships(getActivity(), mAccountId, mUser.id, mUser.screen_name);
		}
	}

	final class UserListsAction extends ListAction {

		public UserListsAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.user_list);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openUserLists(getActivity(), mAccountId, mUser.id, mUser.screen_name);
		}

	}

	final class UserMentionsAction extends ListAction {

		public UserMentionsAction(final int order) {
			super(order);
		}

		@Override
		public String getName() {
			return getString(R.string.user_mentions);
		}

		@Override
		public void onClick() {
			if (mUser == null) return;
			openUserMentions(getActivity(), mAccountId, mUser.screen_name);
		}

	}

}
