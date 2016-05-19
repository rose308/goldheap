package yuku.alkitab.base.ac;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import yuku.afw.V;
import yuku.alkitab.base.App;
import yuku.alkitab.base.ac.base.BaseActivity;
import yuku.alkitab.base.dialog.VersesDialog;
import yuku.alkitab.base.util.Announce;
import yuku.alkitab.base.util.TargetDecoder;
import yuku.alkitab.debug.BuildConfig;
import yuku.alkitab.debug.R;
import yuku.alkitab.util.IntArrayList;
import yuku.alkitabintegration.display.Launcher;

import java.util.Locale;

public class HelpActivity extends BaseActivity {
	private static final String EXTRA_page = "page";
	private static final String EXTRA_overrideTitle = "overrideTitle";
	private static final String EXTRA_overflowMenuItemTitle = "overflowMenuItemTitle";
	private static final String EXTRA_overflowMenuItemIntent = "overflowMenuItemIntent";
	private static final String EXTRA_announcementIds = "announcementIds";
	public static final int REQCODE_overflowMenuItem = 1;

	WebView webview;
	String overflowMenuItemTitle;
	Intent overflowMenuItemIntent;

	public static Intent createIntent(final String page) {
		return _createIntent(page, null, null, null);
	}

	public static Intent createIntentWithOverflowMenu(final String page, final String overflowMenuItemTitle, final Intent overflowMenuItemIntent) {
		return _createIntent(page, null, overflowMenuItemTitle, overflowMenuItemIntent);
	}

	public static Intent createIntent(final String page, final String overrideTitle) {
		return _createIntent(page, overrideTitle, null, null);
	}

	private static Intent _createIntent(final String page, final String overrideTitle, final String overflowMenuItemTitle, final Intent overflowMenuItemIntent) {
		return new Intent(App.context, HelpActivity.class)
			.putExtra(EXTRA_page, page)
			.putExtra(EXTRA_overrideTitle, overrideTitle)
			.putExtra(EXTRA_overflowMenuItemTitle, overflowMenuItemTitle)
			.putExtra(EXTRA_overflowMenuItemIntent, overflowMenuItemIntent);
	}

	public static Intent createViewAnnouncementIntent(final long[] announcementIds) {
		return new Intent(App.context, HelpActivity.class)
			.putExtra(EXTRA_announcementIds, announcementIds);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		enableNonToolbarUpButton();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		webview = V.get(this, R.id.webView);

		if (BuildConfig.DEBUG) {
			if (Build.VERSION.SDK_INT >= 19) {
				WebView.setWebContentsDebuggingEnabled(true);
			}
		}

		final WebSettings webSettings = webview.getSettings();
		//noinspection deprecation
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);

		final String page = getIntent().getStringExtra(EXTRA_page);
		final String overrideTitle = getIntent().getStringExtra(EXTRA_overrideTitle);
		final long[] announcementIds = getIntent().getLongArrayExtra(EXTRA_announcementIds);
		overflowMenuItemTitle = getIntent().getStringExtra(EXTRA_overflowMenuItemTitle);
		overflowMenuItemIntent = getIntent().getParcelableExtra(EXTRA_overflowMenuItemIntent);

		if (overrideTitle != null) {
			setTitle(overrideTitle);
		}

		if (page != null) {
			if (page.startsWith("http:") || page.startsWith("https:")) {
				webview.loadUrl(page);
			} else {
				webview.loadUrl("file:///android_asset/" + page);
			}
		} else if (announcementIds != null) {
			final Locale locale = getResources().getConfiguration().locale;
			final String url = "https://alkitab-host.appspot.com/announce/view?ids=" + App.getDefaultGson().toJson(announcementIds) + (locale == null ? "" : ("&locale=" + locale.toString()));
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "loading announce view url: " + url);
			}
			webview.loadUrl(url);
		}

		webview.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				final Uri uri = Uri.parse(url);
				final String scheme = uri.getScheme();

				if (scheme == null) {
					return false;
				}

				switch (scheme) {
					case "http":
					case "https":
					case "market": {
						// open in external browser
						final Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(uri);
						startActivity(intent);
					} return true;
					case "alkitab": {
						// send back to caller
						final Intent intent = new Intent();
						intent.setData(uri);
						setResult(RESULT_OK, intent);
						finish();
					} return true;
					case "suggest":
						startActivity(com.example.android.wizardpager.MainActivity.createIntent(App.context));
						finish();
						return true;
					case "bible":
						// try to decode using OSIS format
						final String ssp = uri.getSchemeSpecificPart();
						final IntArrayList ariRanges = TargetDecoder.decode("o:" + ssp);
						if (ariRanges == null || ariRanges.size() == 0) {
							new AlertDialogWrapper.Builder(HelpActivity.this)
								.setMessage(getString(R.string.alamat_tidak_sah_alamat, url))
								.setPositiveButton(R.string.ok, null)
								.show();
						} else {
							final VersesDialog dialog = VersesDialog.newInstance(ariRanges);
							dialog.show(getSupportFragmentManager(), VersesDialog.class.getSimpleName());
							dialog.setListener(new VersesDialog.VersesDialogListener() {
								@Override
								public void onVerseSelected(final VersesDialog dialog, final int ari) {
									Log.d(TAG, "Verse link clicked from page");
									startActivity(Launcher.openAppAtBibleLocation(ari));
								}
							});
						}
						return true;
				}
				return false;
			}

			@Override
			public void onPageFinished(final WebView view, final String url) {
				super.onPageFinished(view, url);

				if (overrideTitle == null) {
					setTitle(view.getTitle());
				}

				if (announcementIds != null) {
					Announce.markAsRead(announcementIds);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (overflowMenuItemTitle != null) {
			final MenuItem item = menu.add(0, 1, 0, overflowMenuItemTitle);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		}

		return overflowMenuItemTitle != null;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == 1) {
			startActivityForResult(overflowMenuItemIntent, REQCODE_overflowMenuItem);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQCODE_overflowMenuItem && resultCode == RESULT_OK) {
			setResult(resultCode, data);
			finish();
		}
	}
}
