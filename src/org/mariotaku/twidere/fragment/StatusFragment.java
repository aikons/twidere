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
import static org.mariotaku.twidere.util.Utils.cancelRetweet;
import static org.mariotaku.twidere.util.Utils.clearUserColor;
import static org.mariotaku.twidere.util.Utils.findStatus;
import static org.mariotaku.twidere.util.Utils.formatToLongTimeString;
import static org.mariotaku.twidere.util.Utils.getAccountColor;
import static org.mariotaku.twidere.util.Utils.getBiggerTwitterProfileImage;
import static org.mariotaku.twidere.util.Utils.getDefaultTextSize;
import static org.mariotaku.twidere.util.Utils.getImagesInStatus;
import static org.mariotaku.twidere.util.Utils.getTwitterInstance;
import static org.mariotaku.twidere.util.Utils.getUserColor;
import static org.mariotaku.twidere.util.Utils.getUserTypeIconRes;
import static org.mariotaku.twidere.util.Utils.isMyRetweet;
import static org.mariotaku.twidere.util.Utils.isSameAccount;
import static org.mariotaku.twidere.util.Utils.openImage;
import static org.mariotaku.twidere.util.Utils.openStatusRetweeters;
import static org.mariotaku.twidere.util.Utils.openUserProfile;
import static org.mariotaku.twidere.util.Utils.setMenuForStatus;
import static org.mariotaku.twidere.util.Utils.setUserColor;
import static org.mariotaku.twidere.util.Utils.showErrorMessage;
import static org.mariotaku.twidere.util.Utils.showOkMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mariotaku.menubar.MenuBar;
import org.mariotaku.menubar.MenuBar.OnMenuItemClickListener;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.ColorSelectorActivity;
import org.mariotaku.twidere.adapter.ImagePreviewAdapter;
import org.mariotaku.twidere.adapter.ParcelableStatusesAdapter;
import org.mariotaku.twidere.adapter.iface.IStatusesAdapter;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.loader.DummyParcelableStatusesLoader;
import org.mariotaku.twidere.model.Panes;
import org.mariotaku.twidere.model.ParcelableLocation;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.PreviewImage;
import org.mariotaku.twidere.provider.TweetStore.Accounts;
import org.mariotaku.twidere.provider.TweetStore.Filters;
import org.mariotaku.twidere.util.AsyncTask;
import org.mariotaku.twidere.util.AsyncTwitterWrapper;
import org.mariotaku.twidere.util.ClipboardUtils;
import org.mariotaku.twidere.util.HtmlEscapeHelper;
import org.mariotaku.twidere.util.ImageLoaderWrapper;
import org.mariotaku.twidere.util.OnLinkClickHandler;
import org.mariotaku.twidere.util.TwidereLinkify;
import org.mariotaku.twidere.util.Utils;
import org.mariotaku.twidere.view.ColorLabelRelativeLayout;
import org.mariotaku.twidere.view.ExtendedFrameLayout;

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
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.CroutonStyle;
import edu.ucdavis.earlybird.ProfilingUtil;

public class StatusFragment extends ParcelableStatusesListFragment implements OnClickListener, Panes.Right,
		OnItemClickListener, OnItemSelectedListener {

	private static final int LOADER_ID_STATUS = 1;
	private static final int LOADER_ID_FOLLOW = 2;
	private static final int LOADER_ID_LOCATION = 3;

	private long mAccountId, mStatusId;
	private ParcelableStatus mStatus;

	private boolean mLoadMoreAutomatically;
	private boolean mFollowInfoDisplayed, mLocationInfoDisplayed;
	private boolean mStatusLoaderInitialized, mLocationLoaderInitialized;
	private boolean mFollowInfoLoaderInitialized;;
	private boolean mShouldScroll;

	private SharedPreferences mPreferences;
	private AsyncTwitterWrapper mTwitterWrapper;
	private ImageLoaderWrapper mProfileImageLoader;

	private TextView mNameView, mScreenNameView, mTextView, mTimeAndSourceView, mInReplyToView, mLocationView,
			mRetweetedStatusView;
	private ImageView mProfileImageView;
	private Button mFollowButton;
	private View mMainContent, mFollowIndicator, mImagePreviewContainer, mGalleryContainer;
	private ColorLabelRelativeLayout mProfileView;
	private MenuBar mMenuBar;
	private ProgressBar mStatusLoadProgress, mFollowInfoProgress;
	private AbsSpinner mImagePreviewGallery;
	private ImageButton mPrevImage, mNextImage;
	private View mStatusView;
	private View mLoadImagesIndicator;
	private ExtendedFrameLayout mStatusContainer;
	private ListView mListView;

	private ImagePreviewAdapter mImagePreviewAdapter;

	private LoadConversationTask mConversationTask;

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (getActivity() == null || !isAdded() || isDetached()) return;
			final String action = intent.getAction();
			if (BROADCAST_FRIENDSHIP_CHANGED.equals(action)) {
				if (mStatus != null && mStatus.user_id == intent.getLongExtra(INTENT_KEY_USER_ID, -1)
						&& intent.getBooleanExtra(INTENT_KEY_SUCCEED, false)) {
					showFollowInfo(true);
				}
			} else if (BROADCAST_FAVORITE_CHANGED.equals(action)) {
				final ParcelableStatus status = intent.getParcelableExtra(INTENT_KEY_STATUS);
				if (mStatus != null && status != null && isSameAccount(context, status.account_id, mStatus.account_id)
						&& status.id == mStatusId) {
					getStatus(true);
				}
			} else if (BROADCAST_RETWEET_CHANGED.equals(action)) {
				final long status_id = intent.getLongExtra(INTENT_KEY_STATUS_ID, -1);
				if (status_id > 0 && status_id == mStatusId) {
					getStatus(true);
				}
			}
		}
	};

	private final LoaderCallbacks<Response<ParcelableStatus>> mStatusLoaderCallbacks = new LoaderCallbacks<Response<ParcelableStatus>>() {

		@Override
		public Loader<Response<ParcelableStatus>> onCreateLoader(final int id, final Bundle args) {
			mStatusLoadProgress.setVisibility(View.VISIBLE);
			mMainContent.setVisibility(View.INVISIBLE);
			mMainContent.setEnabled(false);
			setProgressBarIndeterminateVisibility(true);
			final boolean omit_intent_extra = args != null ? args.getBoolean(INTENT_KEY_OMIT_INTENT_EXTRA, true) : true;
			return new StatusLoader(getActivity(), omit_intent_extra, getArguments(), mAccountId, mStatusId);
		}

		@Override
		public void onLoaderReset(final Loader<Response<ParcelableStatus>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<Response<ParcelableStatus>> loader,
				final Response<ParcelableStatus> data) {
			if (data.value == null) {
				showErrorMessage(getActivity(), getString(R.string.getting_status), data.exception, true);
			} else {
				displayStatus(data.value);
				mStatusLoadProgress.setVisibility(View.GONE);
				mMainContent.setVisibility(View.VISIBLE);
				mMainContent.setEnabled(true);
			}
			setProgressBarIndeterminateVisibility(false);
		}

	};

	private final LoaderCallbacks<String> mLocationLoaderCallbacks = new LoaderCallbacks<String>() {

		@Override
		public Loader<String> onCreateLoader(final int id, final Bundle args) {
			return new LocationInfoLoader(getActivity(), mStatus != null ? mStatus.location : null);
		}

		@Override
		public void onLoaderReset(final Loader<String> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<String> loader, final String data) {
			if (data != null) {
				mLocationView.setText(data);
				mLocationInfoDisplayed = true;
			} else {
				mLocationView.setText(R.string.view_map);
				mLocationInfoDisplayed = false;
			}
		}

	};

	private final LoaderCallbacks<Response<Boolean>> mFollowInfoLoaderCallbacks = new LoaderCallbacks<Response<Boolean>>() {

		@Override
		public Loader<Response<Boolean>> onCreateLoader(final int id, final Bundle args) {
			mFollowIndicator.setVisibility(View.VISIBLE);
			mFollowButton.setVisibility(View.GONE);
			mFollowInfoProgress.setVisibility(View.VISIBLE);
			return new FollowInfoLoader(getActivity(), mStatus);
		}

		@Override
		public void onLoaderReset(final Loader<Response<Boolean>> loader) {

		}

		@Override
		public void onLoadFinished(final Loader<Response<Boolean>> loader, final Response<Boolean> data) {
			if (data.exception == null) {
				mFollowIndicator.setVisibility(data.value == null || data.value ? View.GONE : View.VISIBLE);
				if (data.value != null) {
					mFollowButton.setVisibility(data.value ? View.GONE : View.VISIBLE);
					mFollowInfoDisplayed = true;
				}
			}
			mFollowInfoProgress.setVisibility(View.GONE);
		}

	};

	private final OnMenuItemClickListener mMenuItemClickListener = new OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(final MenuItem item) {
			if (mStatus == null) return false;
			final String text_plain = mStatus.text_plain;
			switch (item.getItemId()) {
				case MENU_SHARE: {
					final Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_TEXT, "@" + mStatus.user_screen_name + ": " + text_plain);
					startActivity(Intent.createChooser(intent, getString(R.string.share)));
					break;
				}
				case MENU_COPY: {
					if (ClipboardUtils.setText(getActivity(), mStatus.text_plain)) {
						showOkMessage(getActivity(), R.string.text_copied, false);
					}
					break;
				}
				case MENU_RETWEET: {
					if (isMyRetweet(mStatus)) {
						cancelRetweet(mTwitterWrapper, mStatus);
					} else {
						final long id_to_retweet = mStatus.is_retweet && mStatus.retweet_id > 0 ? mStatus.retweet_id
								: mStatus.id;
						mTwitterWrapper.retweetStatus(mStatus.account_id, id_to_retweet);
					}
					break;
				}
				case MENU_QUOTE: {
					final Intent intent = new Intent(INTENT_ACTION_QUOTE);
					final Bundle bundle = new Bundle();
					bundle.putParcelable(INTENT_KEY_STATUS, mStatus);
					intent.putExtras(bundle);
					startActivity(intent);
					break;
				}
				case MENU_REPLY: {
					final Intent intent = new Intent(INTENT_ACTION_REPLY);
					final Bundle bundle = new Bundle();
					bundle.putParcelable(INTENT_KEY_STATUS, mStatus);
					intent.putExtras(bundle);
					startActivity(intent);
					break;
				}
				case MENU_FAVORITE: {
					if (mStatus.is_favorite) {
						mTwitterWrapper.destroyFavorite(mAccountId, mStatusId);
					} else {
						mTwitterWrapper.createFavoriteAsync(mAccountId, mStatusId);
					}
					break;
				}
				case MENU_DELETE: {
					mTwitterWrapper.destroyStatus(mAccountId, mStatusId);
					break;
				}
				case MENU_MUTE_SOURCE: {
					final String source = HtmlEscapeHelper.toPlainText(mStatus.source);
					if (source == null) return false;
					final Uri uri = Filters.Sources.CONTENT_URI;
					final ContentValues values = new ContentValues();
					final ContentResolver resolver = getContentResolver();
					values.put(Filters.VALUE, source);
					resolver.delete(uri, Filters.VALUE + " = ?", new String[] { source });
					resolver.insert(uri, values);
					Crouton.showText(getActivity(), getString(R.string.source_muted, source), CroutonStyle.INFO);
					break;
				}
				case MENU_SET_COLOR: {
					final Intent intent = new Intent(getActivity(), ColorSelectorActivity.class);
					startActivityForResult(intent, REQUEST_SET_COLOR);
					break;
				}
				case MENU_CLEAR_COLOR: {
					clearUserColor(getActivity(), mStatus.user_id);
					updateUserColor();
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
	};

	private final ExtendedFrameLayout.OnSizeChangedListener mOnSizeChangedListener = new ExtendedFrameLayout.OnSizeChangedListener() {

		@Override
		public void onSizeChanged(final View view, final int w, final int h, final int oldw, final int oldh) {
			if (getActivity() == null) return;
			// final float density = getResources().getDisplayMetrics().density;
			// mStatusView.setMinimumHeight(h - (int) (density * 2));
		}

	};

	public void displayStatus(final ParcelableStatus status) {
		final boolean status_unchanged = mStatus != null && status != null && status.equals(mStatus);
		if (!status_unchanged) {
			getListAdapter().setData(null);
			if (mStatus != null) {
				// UCD
				ProfilingUtil.profile(getActivity(), mStatus.account_id, "End, " + mStatus.id);
			}
		} else {
			setSelection(0);
		}
		if (mConversationTask != null && mConversationTask.getStatus() == AsyncTask.Status.RUNNING) {
			mConversationTask.cancel(true);
		}
		mStatusId = -1;
		mAccountId = -1;
		mStatus = status;
		if (mStatus != null) {
			// UCD
			ProfilingUtil.profile(getActivity(), mStatus.account_id, "Start, " + mStatus.id);
			mAccountId = mStatus.account_id;
			mStatusId = mStatus.id;
		}
		clearPreviewImages();
		if (!status_unchanged) {
			hidePreviewImages();
		}
		if (status == null || getActivity() == null) return;
		final Bundle args = getArguments();
		args.putLong(INTENT_KEY_ACCOUNT_ID, mAccountId);
		args.putLong(INTENT_KEY_STATUS_ID, mStatusId);
		args.putParcelable(INTENT_KEY_STATUS, status);
		mMenuBar.inflate(R.menu.menu_status);
		setMenuForStatus(getActivity(), mMenuBar.getMenu(), status);
		mMenuBar.show();

		updateUserColor();
		mProfileView.drawEnd(getAccountColor(getActivity(), status.account_id));

		mNameView.setText(status.user_name);
		mNameView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
				getUserTypeIconRes(status.user_is_verified, status.user_is_protected), 0);
		mScreenNameView.setText("@" + status.user_screen_name);
		mTextView.setText(Html.fromHtml(status.text_html));
		final TwidereLinkify linkify = new TwidereLinkify(new OnLinkClickHandler(getActivity()));
		linkify.applyAllLinks(mTextView, status.account_id, status.is_possibly_sensitive);
		final String time = formatToLongTimeString(getActivity(), status.timestamp);
		final String source_html = status.source;
		if (!isEmpty(time) && !isEmpty(source_html)) {
			mTimeAndSourceView.setText(Html.fromHtml(getString(R.string.time_source, time, source_html)));
		} else if (isEmpty(time) && !isEmpty(source_html)) {
			mTimeAndSourceView.setText(Html.fromHtml(getString(R.string.source, source_html)));
		} else if (!isEmpty(time) && isEmpty(source_html)) {
			mTimeAndSourceView.setText(time);
		}
		mTimeAndSourceView.setMovementMethod(LinkMovementMethod.getInstance());
		final boolean display_screen_name = NAME_DISPLAY_OPTION_SCREEN_NAME.equals(mPreferences.getString(
				PREFERENCE_KEY_NAME_DISPLAY_OPTION, NAME_DISPLAY_OPTION_BOTH));
		mInReplyToView.setText(getString(R.string.in_reply_to, "@" + status.in_reply_to_screen_name));

		if (mPreferences.getBoolean(PREFERENCE_KEY_DISPLAY_PROFILE_IMAGE, true)) {
			final boolean hires_profile_image = getResources().getBoolean(R.bool.hires_profile_image);
			mProfileImageLoader.displayProfileImage(mProfileImageView,
					hires_profile_image ? getBiggerTwitterProfileImage(status.user_profile_image_url)
							: status.user_profile_image_url);
		} else {
			mProfileImageView.setImageResource(R.drawable.ic_profile_image_default);
		}
		final List<PreviewImage> images = getImagesInStatus(status.text_html);
		mImagePreviewContainer.setVisibility(images.isEmpty() ? View.GONE : View.VISIBLE);
		if (mLoadMoreAutomatically) {
			loadPreviewImages();
		}
		mRetweetedStatusView.setVisibility(!status.user_is_protected ? View.VISIBLE : View.GONE);
		if (status.is_retweet && status.retweet_id > 0) {
			final String name = display_screen_name ? status.retweeted_by_screen_name : status.retweeted_by_name;
			if (status.retweet_count > 1) {
				mRetweetedStatusView
						.setText(getString(R.string.retweeted_by_with_count, name, status.retweet_count - 1));
			} else {
				mRetweetedStatusView.setText(getString(R.string.retweeted_by, name));
			}
		} else {
			if (status.retweet_count > 0) {
				mRetweetedStatusView.setText(getString(R.string.retweeted_by_count, status.retweet_count));
			} else {
				mRetweetedStatusView.setText(R.string.users_retweeted_this);
			}
		}
		mLocationView.setVisibility(ParcelableLocation.isValidLocation(status.location) ? View.VISIBLE : View.GONE);
		if (mLoadMoreAutomatically) {
			showFollowInfo(true);
			showLocationInfo(true);
			showConversation();
		} else {
			mFollowIndicator.setVisibility(View.GONE);
		}
		updateConversationInfo();
		scrollToStart();
	}

	@Override
	public String getPullToRefreshTag() {
		return "view_status";
	}

	@Override
	public Loader<List<ParcelableStatus>> newLoaderInstance(final Context context, final Bundle args) {
		return new DummyParcelableStatusesLoader(getActivity(), getData());
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListShownNoAnimation(true);
		mListView = getListView();
		// mListView.setStackFromBottom(true);
		getListAdapter().setGapDisallowed(true);
		final TwidereApplication application = getApplication();
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mProfileImageLoader = application.getImageLoaderWrapper();
		mTwitterWrapper = getTwitterWrapper();
		mLoadMoreAutomatically = mPreferences.getBoolean(PREFERENCE_KEY_LOAD_MORE_AUTOMATICALLY, false);
		mImagePreviewAdapter = new ImagePreviewAdapter(getActivity());
		setPullToRefreshEnabled(false);
		// getPullToRefreshAttacher().setEnabled(false);
		final Bundle bundle = getArguments();
		if (bundle != null) {
			mAccountId = bundle.getLong(INTENT_KEY_ACCOUNT_ID);
			mStatusId = bundle.getLong(INTENT_KEY_STATUS_ID);
		}
		mLoadImagesIndicator.setOnClickListener(this);
		mInReplyToView.setOnClickListener(this);
		mFollowButton.setOnClickListener(this);
		mProfileView.setOnClickListener(this);
		mLocationView.setOnClickListener(this);
		mRetweetedStatusView.setOnClickListener(this);
		mMenuBar.setOnMenuItemClickListener(mMenuItemClickListener);
		getStatus(false);
		mImagePreviewGallery.setAdapter(mImagePreviewAdapter);
		mImagePreviewGallery.setOnItemClickListener(this);
		mImagePreviewGallery.setOnItemSelectedListener(this);
		mPrevImage.setOnClickListener(this);
		mNextImage.setOnClickListener(this);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (intent == null || mStatus == null) return;
		switch (requestCode) {
			case REQUEST_SET_COLOR: {
				if (resultCode == Activity.RESULT_OK) if (intent != null && intent.getExtras() != null) {
					final int color = intent.getIntExtra(Accounts.USER_COLOR, Color.TRANSPARENT);
					setUserColor(getActivity(), mStatus.user_id, color);
					updateUserColor();
				}
				break;
			}
		}

	}

	@Override
	public void onClick(final View view) {
		if (mStatus == null) return;
		switch (view.getId()) {
			case R.id.profile: {
				openUserProfile(getActivity(), mStatus.account_id, mStatus.user_id, null);
				break;
			}
			case R.id.follow: {
				mTwitterWrapper.createFriendship(mAccountId, mStatus.user_id);
				break;
			}
			case R.id.in_reply_to: {
				showConversation();
				break;
			}
			case R.id.location_view: {
				if (mStatus.location == null) return;
				final ParcelableLocation location = mStatus.location;
				if (location == null || !location.isValid()) return;
				final Uri.Builder builder = new Uri.Builder();
				builder.scheme(SCHEME_TWIDERE);
				builder.authority(AUTHORITY_MAP);
				builder.appendQueryParameter(QUERY_PARAM_LAT, String.valueOf(location.latitude));
				builder.appendQueryParameter(QUERY_PARAM_LNG, String.valueOf(location.longitude));
				startActivity(new Intent(Intent.ACTION_VIEW, builder.build()));
				break;
			}
			case R.id.load_images: {
				loadPreviewImages();
				// UCD
				ProfilingUtil.profile(getActivity(), mAccountId, "Thumbnail click, " + mStatusId);
				break;
			}
			case R.id.retweet_view: {
				openStatusRetweeters(getActivity(), mAccountId, mStatus.retweet_id > 0 ? mStatus.retweet_id
						: mStatus.id);
				break;
			}
			case R.id.prev_image: {
				final int count = mImagePreviewAdapter.getCount(), pos = mImagePreviewGallery.getSelectedItemPosition();
				if (count == 0 || pos == 0) return;
				mImagePreviewGallery.setSelection(pos - 1, true);
				break;
			}
			case R.id.next_image: {
				final int count = mImagePreviewAdapter.getCount(), pos = mImagePreviewGallery.getSelectedItemPosition();
				if (count == 0 || pos == count - 1) return;
				mImagePreviewGallery.setSelection(pos + 1, true);
				break;
			}
		}

	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.status, null, false);
		mMainContent = view.findViewById(R.id.content);
		mStatusLoadProgress = (ProgressBar) view.findViewById(R.id.status_load_progress);
		mMenuBar = (MenuBar) view.findViewById(R.id.menu_bar);
		mStatusContainer = (ExtendedFrameLayout) view.findViewById(R.id.status_container);
		mStatusContainer.addView(super.onCreateView(inflater, container, savedInstanceState));
		mStatusContainer.setOnSizeChangedListener(mOnSizeChangedListener);
		mStatusView = inflater.inflate(R.layout.status_content, null, false);
		mImagePreviewContainer = mStatusView.findViewById(R.id.image_preview);
		mLocationView = (TextView) mStatusView.findViewById(R.id.location_view);
		mRetweetedStatusView = (TextView) mStatusView.findViewById(R.id.retweet_view);
		mNameView = (TextView) mStatusView.findViewById(R.id.name);
		mScreenNameView = (TextView) mStatusView.findViewById(R.id.screen_name);
		mTextView = (TextView) mStatusView.findViewById(R.id.text);
		mProfileImageView = (ImageView) mStatusView.findViewById(R.id.profile_image);
		mTimeAndSourceView = (TextView) mStatusView.findViewById(R.id.time_source);
		mInReplyToView = (TextView) mStatusView.findViewById(R.id.in_reply_to);
		mFollowButton = (Button) mStatusView.findViewById(R.id.follow);
		mFollowIndicator = mStatusView.findViewById(R.id.follow_indicator);
		mFollowInfoProgress = (ProgressBar) mStatusView.findViewById(R.id.follow_info_progress);
		mProfileView = (ColorLabelRelativeLayout) mStatusView.findViewById(R.id.profile);
		mImagePreviewGallery = (AbsSpinner) mStatusView.findViewById(R.id.preview_gallery);
		mGalleryContainer = mStatusView.findViewById(R.id.gallery_container);
		mPrevImage = (ImageButton) mStatusView.findViewById(R.id.prev_image);
		mNextImage = (ImageButton) mStatusView.findViewById(R.id.next_image);
		mLoadImagesIndicator = mStatusView.findViewById(R.id.load_images);
		return view;
	}

	@Override
	public void onDestroyView() {
		// UCD
		if (mStatus != null) {
			ProfilingUtil.profile(getActivity(), mAccountId, "End, " + mStatus.id);
		}
		mStatus = null;
		mAccountId = -1;
		mStatusId = -1;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_STATUS);
		lm.destroyLoader(LOADER_ID_LOCATION);
		lm.destroyLoader(LOADER_ID_FOLLOW);
		if (mConversationTask != null && mConversationTask.getStatus() == AsyncTask.Status.RUNNING) {
			mConversationTask.cancel(true);
		}
		super.onDestroyView();
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final PreviewImage image = mImagePreviewAdapter.getItem(position);
		if (mStatus == null || image == null) return;
		// UCD
		ProfilingUtil.profile(getActivity(), mAccountId, "Large image click, " + mStatusId + ", " + image);
		openImage(getActivity(), image.image_full_url, image.image_original_url, mStatus.is_possibly_sensitive);
	}

	@Override
	public void onItemsCleared() {

	}

	@Override
	public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
		final int count = mImagePreviewAdapter.getCount();
		if (count <= 1) {
			mPrevImage.setVisibility(View.GONE);
			mNextImage.setVisibility(View.GONE);
		} else if (position == 0) {
			mPrevImage.setVisibility(View.GONE);
			mNextImage.setVisibility(View.VISIBLE);
		} else if (position == count - 1) {
			mPrevImage.setVisibility(View.VISIBLE);
			mNextImage.setVisibility(View.GONE);
		} else {
			mPrevImage.setVisibility(View.VISIBLE);
			mNextImage.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onNothingSelected(final AdapterView<?> parent) {

	}

	@Override
	public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
			final int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(final AbsListView view, final int scrollState) {
		super.onScrollStateChanged(view, scrollState);
		mShouldScroll = false;
	}

	@Override
	public void onStart() {
		super.onStart();
		final IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_FRIENDSHIP_CHANGED);
		filter.addAction(BROADCAST_FAVORITE_CHANGED);
		filter.addAction(BROADCAST_RETWEET_CHANGED);
		registerReceiver(mStatusReceiver, filter);
		updateUserColor();
		final int text_size = mPreferences.getInt(PREFERENCE_KEY_TEXT_SIZE, getDefaultTextSize(getActivity()));
		mNameView.setTextSize(text_size * 1.25f);
		mTextView.setTextSize(text_size * 1.25f);
		mScreenNameView.setTextSize(text_size * 0.85f);
		mTimeAndSourceView.setTextSize(text_size * 0.85f);
		mInReplyToView.setTextSize(text_size * 0.85f);
		mLocationView.setTextSize(text_size * 0.85f);
		mRetweetedStatusView.setTextSize(text_size * 0.85f);
	}

	@Override
	public void onStop() {
		unregisterReceiver(mStatusReceiver);
		super.onStop();
	}

	@Override
	public boolean scrollToStart() {
		if (mListView == null) return false;
		final IStatusesAdapter<List<ParcelableStatus>> adapter = getListAdapter();
		Utils.scrollListToPosition(mListView, adapter.getCount() + mListView.getFooterViewsCount() - 1, 0);
		return true;
	}

	@Override
	protected String[] getSavedStatusesFileArgs() {
		return null;
	}

	// @Override
	// protected void setItemSelected(final ParcelableStatus status, final int
	// position, final boolean selected) {
	// final MultiSelectManager manager = getMultiSelectManager();
	// final Object only_item = manager.getCount() == 1 ?
	// manager.getSelectedItems().get(0) : null;
	// final boolean only_item_selected = only_item != null &&
	// !only_item.equals(mStatus);
	// mListView.setItemChecked(0, only_item_selected);
	// if (mStatus != null) {
	// if (only_item_selected) {
	// manager.selectItem(mStatus);
	// } else {
	// manager.unselectItem(mStatus);
	// }
	// }
	// super.setItemSelected(status, position, selected);
	// }

	@Override
	protected void onReachedBottom() {

	}

	@Override
	protected void setItemSelected(final ParcelableStatus status, final int position, final boolean selected) {
	}

	@Override
	protected void setListHeaderFooters(final ListView list) {
		list.addHeaderView(mStatusView, null, true);
	}

	private void addConversationStatus(final ParcelableStatus status) {
		if (getActivity() == null || isDetached()) return;
		final List<ParcelableStatus> data = getData();
		if (data == null) return;
		data.add(status);
		final ParcelableStatusesAdapter adapter = (ParcelableStatusesAdapter) getListAdapter();
		adapter.setData(data);
		if (!mLoadMoreAutomatically && mShouldScroll) {
			setSelection(0);
		}
	}

	private void clearPreviewImages() {
		mImagePreviewAdapter.clear();
	}

	private void getStatus(final boolean omit_intent_extra) {
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_STATUS);
		final Bundle args = new Bundle();
		args.putBoolean(INTENT_KEY_OMIT_INTENT_EXTRA, omit_intent_extra);
		if (!mStatusLoaderInitialized) {
			lm.initLoader(LOADER_ID_STATUS, args, mStatusLoaderCallbacks);
			mStatusLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_STATUS, args, mStatusLoaderCallbacks);
		}
	}

	private void hidePreviewImages() {
		mLoadImagesIndicator.setVisibility(View.VISIBLE);
		mGalleryContainer.setVisibility(View.GONE);
	}

	private void loadPreviewImages() {
		if (mStatus == null) return;
		mLoadImagesIndicator.setVisibility(View.GONE);
		mGalleryContainer.setVisibility(View.VISIBLE);
		mImagePreviewAdapter.clear();
		final List<PreviewImage> images = getImagesInStatus(mStatus.text_html);
		mImagePreviewAdapter.addAll(images, mStatus.is_possibly_sensitive);
	}

	private void showConversation() {
		if (mConversationTask != null && mConversationTask.getStatus() == AsyncTask.Status.RUNNING) return;
		final IStatusesAdapter<List<ParcelableStatus>> adapter = getListAdapter();
		final int count = adapter.getCount();
		final ParcelableStatus status;
		if (count == 0) {
			mShouldScroll = !mLoadMoreAutomatically;
			status = mStatus;
		} else {
			status = adapter.getStatus(adapter.getCount() - 1);
		}
		if (status == null || status.in_reply_to_status_id <= 0) return;
		mConversationTask = new LoadConversationTask(this);
		mConversationTask.execute(status);
	}

	private void showFollowInfo(final boolean force) {
		if (mFollowInfoDisplayed && !force) return;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_FOLLOW);
		if (!mFollowInfoLoaderInitialized) {
			lm.initLoader(LOADER_ID_FOLLOW, null, mFollowInfoLoaderCallbacks);
			mFollowInfoLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_FOLLOW, null, mFollowInfoLoaderCallbacks);
		}
	}

	private void showLocationInfo(final boolean force) {
		if (mLocationInfoDisplayed && !force) return;
		final LoaderManager lm = getLoaderManager();
		lm.destroyLoader(LOADER_ID_LOCATION);
		if (!mLocationLoaderInitialized) {
			lm.initLoader(LOADER_ID_LOCATION, null, mLocationLoaderCallbacks);
			mLocationLoaderInitialized = true;
		} else {
			lm.restartLoader(LOADER_ID_LOCATION, null, mLocationLoaderCallbacks);
		}
	}

	private void updateConversationInfo() {
		final boolean has_converstion = mStatus != null && mStatus.in_reply_to_status_id > 0;
		final IStatusesAdapter<List<ParcelableStatus>> adapter = getListAdapter();
		final boolean load_not_finished = adapter.isEmpty()
				|| adapter.getStatus(adapter.getCount() - 1).in_reply_to_status_id > 0;
		final boolean enable = has_converstion && load_not_finished;
		mInReplyToView.setVisibility(enable ? View.VISIBLE : View.GONE);
		mInReplyToView.setClickable(enable);
	}

	private void updateUserColor() {
		if (mStatus == null) return;
		mProfileView.drawStart(getUserColor(getActivity(), mStatus.user_id));
	}

	public static class LoadConversationTask extends AsyncTask<ParcelableStatus, Void, Response<Boolean>> {

		final Handler handler;
		final Context context;
		final StatusFragment fragment;

		LoadConversationTask(final StatusFragment fragment) {
			context = fragment.getActivity();
			this.fragment = fragment;
			handler = new Handler();
		}

		@Override
		protected Response<Boolean> doInBackground(final ParcelableStatus... params) {
			if (params == null || params.length != 1) return new Response<Boolean>(false, null);
			try {
				final long account_id = params[0].account_id;
				ParcelableStatus status = params[0];
				while (status != null && status.in_reply_to_status_id > 0 && !isCancelled()) {
					status = findStatus(context, account_id, status.in_reply_to_status_id);
					if (status == null) {
						break;
					}
					handler.post(new AddStatusRunnable(status));
				}
			} catch (final TwitterException e) {
				return new Response<Boolean>(false, e);
			}
			return new Response<Boolean>(true, null);
		}

		@Override
		protected void onCancelled() {
			fragment.setProgressBarIndeterminateVisibility(false);
			fragment.updateConversationInfo();
		}

		@Override
		protected void onPostExecute(final Response<Boolean> data) {
			fragment.setProgressBarIndeterminateVisibility(false);
			fragment.updateConversationInfo();
			if (data == null || data.value == null || !data.value) {
				showErrorMessage(context, context.getString(R.string.getting_status), data.exception, true);
			}
		}

		@Override
		protected void onPreExecute() {
			fragment.setProgressBarIndeterminateVisibility(true);
			fragment.updateConversationInfo();
		}

		class AddStatusRunnable implements Runnable {

			final ParcelableStatus status;

			AddStatusRunnable(final ParcelableStatus status) {
				this.status = status;
			}

			@Override
			public void run() {
				fragment.addConversationStatus(status);
			}
		}
	}

	public static class LocationInfoLoader extends AsyncTaskLoader<String> {

		private final Context context;
		private final ParcelableLocation location;

		public LocationInfoLoader(final Context context, final ParcelableLocation location) {
			super(context);
			this.context = context;
			this.location = location;
		}

		@Override
		public String loadInBackground() {
			if (location == null) return null;
			try {
				final Geocoder coder = new Geocoder(context);
				final List<Address> addresses = coder.getFromLocation(location.latitude, location.longitude, 1);
				if (addresses.size() == 1) {
					final Address address = addresses.get(0);
					final StringBuilder builder = new StringBuilder();
					final int max_idx = address.getMaxAddressLineIndex();
					for (int i = 0; i < max_idx; i++) {
						builder.append(address.getAddressLine(i));
						if (i != max_idx - 1) {
							builder.append(", ");
						}
					}
					return builder.toString();

				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

	}

	public static class Response<T> {
		public final T value;
		public final TwitterException exception;

		public Response(final T value, final TwitterException exception) {
			this.value = value;
			this.exception = exception;
		}
	}

	public static class StatusLoader extends AsyncTaskLoader<Response<ParcelableStatus>> {

		private final Context context;
		private final boolean omit_intent_extra;
		private final Bundle extras;
		private final long account_id, status_id;

		public StatusLoader(final Context context, final boolean omit_intent_extra, final Bundle extras,
				final long account_id, final long status_id) {
			super(context);
			this.context = context;
			this.omit_intent_extra = omit_intent_extra;
			this.extras = extras;
			this.account_id = account_id;
			this.status_id = status_id;
		}

		@Override
		public Response<ParcelableStatus> loadInBackground() {
			if (!omit_intent_extra && extras != null) {
				final ParcelableStatus status = extras.getParcelable(INTENT_KEY_STATUS);
				if (status != null) return new Response<ParcelableStatus>(status, null);
			}
			try {
				return new Response<ParcelableStatus>(findStatus(context, account_id, status_id), null);
			} catch (final TwitterException e) {
				return new Response<ParcelableStatus>(null, e);
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

	}

	static class FollowInfoLoader extends AsyncTaskLoader<Response<Boolean>> {

		private final ParcelableStatus status;
		private final Context context;

		public FollowInfoLoader(final Context context, final ParcelableStatus status) {
			super(context);
			this.context = context;
			this.status = status;
		}

		@Override
		public Response<Boolean> loadInBackground() {
			return isAllFollowing();
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		private Response<Boolean> isAllFollowing() {
			if (status == null) return new Response<Boolean>(null, null);
			if (status.user_id == status.account_id) return new Response<Boolean>(true, null);
			final Twitter twitter = getTwitterInstance(context, status.account_id, false);
			if (twitter == null) return new Response<Boolean>(null, null);
			try {
				final Relationship result = twitter.showFriendship(status.account_id, status.user_id);
				if (!result.isSourceFollowingTarget()) return new Response<Boolean>(false, null);
			} catch (final TwitterException e) {
				return new Response<Boolean>(null, e);
			}
			return new Response<Boolean>(null, null);
		}
	}

	static class ImagesAdapter extends BaseAdapter {

		private final List<PreviewImage> mImages = new ArrayList<PreviewImage>();
		private final ImageLoaderWrapper mImageLoader;
		private final LayoutInflater mInflater;

		public ImagesAdapter(final Context context) {
			mImageLoader = TwidereApplication.getInstance(context).getImageLoaderWrapper();
			mInflater = LayoutInflater.from(context);
		}

		public boolean addAll(final Collection<? extends PreviewImage> images) {
			final boolean ret = images != null && mImages.addAll(images);
			notifyDataSetChanged();
			return ret;
		}

		public void clear() {
			mImages.clear();
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mImages.size();
		}

		@Override
		public PreviewImage getItem(final int position) {
			return mImages.get(position);
		}

		@Override
		public long getItemId(final int position) {
			final PreviewImage spec = getItem(position);
			return spec != null ? spec.hashCode() : 0;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final View view = convertView != null ? convertView : mInflater.inflate(R.layout.image_preview_item, null);
			final ImageView image = (ImageView) view.findViewById(R.id.image);
			final PreviewImage spec = getItem(position);
			mImageLoader.displayPreviewImage(image, spec != null ? spec.image_preview_url : null);
			return view;
		}

	}

}
