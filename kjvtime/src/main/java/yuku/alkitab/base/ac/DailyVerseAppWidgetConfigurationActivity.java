package yuku.alkitab.base.ac;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import yuku.afw.V;
import yuku.afw.widget.EasyAdapter;
import yuku.alkitab.base.App;
import yuku.alkitab.base.S;
import yuku.alkitab.base.ac.base.BaseActivity;
import yuku.alkitab.base.br.DailyVerseAppWidgetReceiver;
import yuku.alkitab.base.model.MVersion;
import yuku.alkitab.base.util.DailyVerseData;
import yuku.alkitab.debug.R;

import java.util.List;

public class DailyVerseAppWidgetConfigurationActivity extends BaseActivity {
	private VersionAdapter adapter;

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	int selectedVersionPosition = -1;
	CheckBox cDarkText;
	SeekBar sbTextSize;
	TextView tTextSize;
	View panelTransparent;
	SeekBar sbTransparent;
	TextView tTransparent;
	private CheckBox cTransparentBackground;

	final BroadcastReceiver br = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (VersionsActivity.VersionListFragment.ACTION_RELOAD.equals(intent.getAction())) {
				if (adapter != null) adapter.reload();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);
		setContentView(R.layout.activity_daily_verse_configuration);

		final ListView lsVersionsAppWidget = V.get(this, R.id.lsVersionsAppWidget);
		final Button bOk = V.get(this, R.id.bOk);
		final Button bCancel = V.get(this, R.id.bCancel);
		cTransparentBackground = V.get(this, R.id.cTransparentBackground);
		cDarkText = V.get(this, R.id.cDarkText);
		sbTextSize = V.get(this, R.id.sbTextSize);
		tTextSize = V.get(this, R.id.tTextSize);
		panelTransparent = V.get(this, R.id.panelTransparent);
		sbTransparent = V.get(this, R.id.sbTransparent);
		tTransparent = V.get(this, R.id.tTransparent);

		// Find the widget id from the intent.
		final Intent intent = getIntent();
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

		adapter = new VersionAdapter();
		adapter.reload();
		lsVersionsAppWidget.setAdapter(adapter);
		lsVersionsAppWidget.setOnItemClickListener((parent, view, position, id) -> {
			selectedVersionPosition = position;
			bOk.setEnabled(true);
			adapter.notifyDataSetChanged();
		});

		bOk.setEnabled(false);
		bOk.setOnClickListener(bOk_click);
		bCancel.setOnClickListener(v -> finish());

		cTransparentBackground.setOnCheckedChangeListener((buttonView, isChecked) -> {
			panelTransparent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			cDarkText.setEnabled(isChecked);
			if (!isChecked) {
				cDarkText.setChecked(false);
			}
		});

		sbTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				sbTextSize_progressChanged(progress);
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
			}
		});
		sbTextSize_progressChanged(sbTextSize.getProgress());

		sbTransparent.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
				sbTransparent_progressChanged(progress);
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
			}
		});
		sbTransparent_progressChanged(sbTransparent.getProgress());

		App.getLbm().registerReceiver(br, new IntentFilter(VersionsActivity.VersionListFragment.ACTION_RELOAD));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		App.getLbm().unregisterReceiver(br);
	}

	void sbTextSize_progressChanged(final int progress) {
		final float textSize = progressToActualTextSize(progress);
		tTextSize.setText("" + (int) textSize);
	}

	void sbTransparent_progressChanged(final int progress) {
		final int percent = progressToActualTransparentPercent(progress);
		tTransparent.setText(percent + "%");
	}

	int progressToActualTransparentPercent(final int progress) {
		return progress * 5;
	}

	int progressToActualAlpha(final int progress) {
		return (int) (255.f * ((100 - progressToActualTransparentPercent(progress)) / 100.f));
	}

	float progressToActualTextSize(final int progress) {
		return progress + 8.f;
	}

	private View.OnClickListener bOk_click = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			final Context context = DailyVerseAppWidgetConfigurationActivity.this;
			final String versionId = adapter.versions.get(selectedVersionPosition).getVersionId();

			final DailyVerseData.SavedState savedState = new DailyVerseData.SavedState();
			savedState.versionId = versionId;
			savedState.transparentBackground = cTransparentBackground.isChecked();
			savedState.backgroundAlpha = cTransparentBackground.isChecked() ? progressToActualAlpha(sbTransparent.getProgress()) : 255;
			savedState.darkText = cDarkText.isChecked();
			savedState.textSize = progressToActualTextSize(sbTextSize.getProgress());
			savedState.click = 0;
			DailyVerseData.saveSavedState(mAppWidgetId, savedState);

			DailyVerseAppWidgetReceiver.buildUpdate(context, mAppWidgetId, 1);

			ComponentName provider = new ComponentName(context, DailyVerseAppWidgetReceiver.class);
			int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(provider);
			DailyVerseAppWidgetReceiver.setAlarm(DailyVerseAppWidgetConfigurationActivity.this, ids);

			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	};

	class VersionAdapter extends EasyAdapter {
		private List<MVersion> versions;

		void reload() {
			versions = S.getAvailableVersions();
			notifyDataSetChanged();
		}

		@Override
		public View newView(final int position, final ViewGroup parent) {
			return getLayoutInflater().inflate(android.R.layout.simple_list_item_single_choice, parent, false);
		}

		@Override
		public void bindView(final View view, final int position, final ViewGroup parent) {
			CheckedTextView text1 = V.get(view, android.R.id.text1);
			text1.setText(versions.get(position).longName);

			text1.setChecked(position == selectedVersionPosition);
		}

		@Override
		public int getCount() {
			return versions.size();
		}
	}
}