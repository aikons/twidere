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

import static org.mariotaku.twidere.util.Utils.closeSilently;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import edu.ucdavis.earlybird.CSVFileFilter;

import org.mariotaku.twidere.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DataProfilingSettingsActivity extends BaseSupportActivity implements OnClickListener {

	private SharedPreferences mPreferences;

	private Button mSaveButton, mPreviewButton;
	private CheckBox mCheckBox;
	private TextView mTextView;
	private boolean mShowingPreview;

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.save: {
				final SharedPreferences.Editor editor = mPreferences.edit();
				editor.putBoolean(PREFERENCE_KEY_UCD_DATA_PROFILING, mCheckBox.isChecked());
				editor.putBoolean(PREFERENCE_KEY_SHOW_UCD_DATA_PROFILING_REQUEST, false);
				editor.commit();
				finish();
				break;
			}
			case R.id.preview: {
				if (!mShowingPreview) {
					mTextView.setText(null);
					final File dir = getFilesDir();
					for (final File file : dir.listFiles(new CSVFileFilter())) {
						mTextView.append(file.getName() + ":\n------\n");
						try {
							final BufferedReader br = new BufferedReader(new FileReader(file));
							String line = null;
							int i = 0;
							while ((line = br.readLine()) != null && i < 10) {
								mTextView.append(line + "\n");
								i++;
							}
							mTextView.append("------------\n\n");
							closeSilently(br);
						} catch (final IOException e) {
							mTextView.append("Cannot read this file");
						}
					}
				} else {
					mTextView.setText(R.string.data_profiling_summary);
				}
				mShowingPreview = !mShowingPreview;
				break;
			}
		}

	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mTextView = (TextView) findViewById(android.R.id.text1);
		mCheckBox = (CheckBox) findViewById(R.id.checkbox);
		mSaveButton = (Button) findViewById(R.id.save);
		mPreviewButton = (Button) findViewById(R.id.preview);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.data_profiling_settings);
		if (mPreferences.contains(PREFERENCE_KEY_UCD_DATA_PROFILING)) {
			mCheckBox.setChecked(mPreferences.getBoolean(PREFERENCE_KEY_UCD_DATA_PROFILING, false));
		}
		mSaveButton.setOnClickListener(this);
		mPreviewButton.setOnClickListener(this);
	}

}
