package yuku.alkitab.base;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONException;
import org.json.JSONObject;
import yuku.afw.V;
import yuku.afw.storage.Preferences;
import yuku.afw.widget.EasyAdapter;
import yuku.alkitab.base.ac.GotoActivity;
import yuku.alkitab.base.ac.MarkerListActivity;
import yuku.alkitab.base.ac.MarkersActivity;
import yuku.alkitab.base.ac.NoteActivity;
import yuku.alkitab.base.ac.SearchActivity;
import yuku.alkitab.base.ac.SettingsActivity;
import yuku.alkitab.base.ac.ShareActivity;
import yuku.alkitab.base.ac.base.BaseLeftDrawerActivity;
import yuku.alkitab.base.config.AppConfig;
import yuku.alkitab.base.dialog.ProgressMarkListDialog;
import yuku.alkitab.base.dialog.ProgressMarkRenameDialog;
import yuku.alkitab.base.dialog.TypeBookmarkDialog;
import yuku.alkitab.base.dialog.TypeHighlightDialog;
import yuku.alkitab.base.dialog.VersesDialog;
import yuku.alkitab.base.dialog.XrefDialog;
import yuku.alkitab.base.model.MVersion;
import yuku.alkitab.base.model.MVersionDb;
import yuku.alkitab.base.model.MVersionInternal;
import yuku.alkitab.base.model.VersionImpl;
import yuku.alkitab.base.storage.Prefkey;
import yuku.alkitab.base.util.Announce;
import yuku.alkitab.base.util.Appearances;
import yuku.alkitab.base.util.CurrentReading;
import yuku.alkitab.base.util.ExtensionManager;
import yuku.alkitab.base.util.Highlights;
import yuku.alkitab.base.util.History;
import yuku.alkitab.base.util.Jumper;
import yuku.alkitab.base.util.LidToAri;
import yuku.alkitab.base.util.OtherAppIntegration;
import yuku.alkitab.base.util.ShareUrl;
import yuku.alkitab.base.util.Sqlitil;
import yuku.alkitab.base.widget.CallbackSpan;
import yuku.alkitab.base.widget.Floater;
import yuku.alkitab.base.widget.FormattedTextRenderer;
import yuku.alkitab.base.widget.GotoButton;
import yuku.alkitab.base.widget.LabeledSplitHandleButton;
import yuku.alkitab.base.widget.LeftDrawer;
import yuku.alkitab.base.widget.ScrollbarSetter;
import yuku.alkitab.base.widget.SingleViewVerseAdapter;
import yuku.alkitab.base.widget.SplitHandleButton;
import yuku.alkitab.base.widget.TextAppearancePanel;
import yuku.alkitab.base.widget.TwofingerLinearLayout;
import yuku.alkitab.base.widget.VerseInlineLinkSpan;
import yuku.alkitab.base.widget.VerseRenderer;
import yuku.alkitab.base.widget.VersesView;
import yuku.alkitab.debug.BuildConfig;
import yuku.alkitab.debug.R;
import yuku.alkitab.model.Book;
import yuku.alkitab.model.FootnoteEntry;
import yuku.alkitab.model.Label;
import yuku.alkitab.model.Marker;
import yuku.alkitab.model.PericopeBlock;
import yuku.alkitab.model.ProgressMark;
import yuku.alkitab.model.SingleChapterVerses;
import yuku.alkitab.model.Version;
import yuku.alkitab.reminder.util.DevotionReminder;
import yuku.alkitab.util.Ari;
import yuku.alkitab.util.IntArrayList;
import yuku.devoxx.flowlayout.FlowLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static yuku.alkitab.base.util.Literals.Array;

public class IsiActivity extends BaseLeftDrawerActivity implements XrefDialog.XrefDialogListener, LeftDrawer.Text.Listener, ProgressMarkListDialog.Listener {
	public static final String TAG = IsiActivity.class.getSimpleName();

	public static final String ACTION_ATTRIBUTE_MAP_CHANGED = "yuku.alkitab.action.ATTRIBUTE_MAP_CHANGED";
	public static final String ACTION_ACTIVE_VERSION_CHANGED = IsiActivity.class.getName() + ".action.ACTIVE_VERSION_CHANGED";
	public static final String ACTION_NIGHT_MODE_CHANGED = IsiActivity.class.getName() + ".action.NIGHT_MODE_CHANGED";
	public static final String ACTION_NEEDS_RESTART = IsiActivity.class.getName() + ".action.NEEDS_RESTART";

	private static final int REQCODE_goto = 1;
	private static final int REQCODE_share = 7;
	private static final int REQCODE_textAppearanceGetFonts = 9;
	private static final int REQCODE_textAppearanceCustomColors = 10;
	private static final int REQCODE_edit_note_1 = 11;
	private static final int REQCODE_edit_note_2 = 12;

	private static final String EXTRA_verseUrl = "verseUrl";
	private boolean uncheckVersesWhenActionModeDestroyed = true;

	private boolean needsRestart; // whether this activity needs to be restarted

	private GotoButton.FloaterDragListener bGoto_floaterDrag = new GotoButton.FloaterDragListener() {
		final int[] floaterLocationOnScreen = {0, 0};

		@Override
		public void onFloaterDragStart(final float screenX, final float screenY) {
			floater.show(activeBook.bookId, chapter_1);
			floater.onDragStart(S.activeVersion);
		}

		@Override
		public void onFloaterDragMove(final float screenX, final float screenY) {
			floater.getLocationOnScreen(floaterLocationOnScreen);
			floater.onDragMove(screenX - floaterLocationOnScreen[0], screenY - floaterLocationOnScreen[1]);
		}

		@Override
		public void onFloaterDragComplete(final float screenX, final float screenY) {
			floater.hide();
			floater.onDragComplete(screenX - floaterLocationOnScreen[0], screenY - floaterLocationOnScreen[1]);
		}
	};

	final Floater.Listener floater_listener = new Floater.Listener() {
		@Override
		public void onSelectComplete(final int ari) {
			jumpToAri(ari);
			history.add(ari);
		}
	};

	TwofingerLinearLayout.Listener splitRoot_listener = new TwofingerLinearLayout.Listener() {
		float startFontSize;
		float startDx = Float.MIN_VALUE;
		float chapterSwipeCellWidth; // initted later
		boolean moreSwipeYAllowed = true; // to prevent setting and unsetting fullscreen many times within one gesture

		@Override
		public void onOnefingerLeft() {
			App.trackEvent("text_onefinger_left");
			bRight_click();
		}

		@Override
		public void onOnefingerRight() {
			App.trackEvent("text_onefinger_right");
			bLeft_click();
		}

		@Override
		public void onTwofingerStart() {
			chapterSwipeCellWidth = 24.f * getResources().getDisplayMetrics().density;
			startFontSize = Preferences.getFloat(Prefkey.ukuranHuruf2, (float) App.context.getResources().getInteger(R.integer.pref_ukuranHuruf2_default));
		}

		@Override
		public void onTwofingerScale(final float scale) {
			float nowFontSize = startFontSize * scale;

			if (nowFontSize < 2.f) nowFontSize = 2.f;
			if (nowFontSize > 42.f) nowFontSize = 42.f;

			Preferences.setFloat(Prefkey.ukuranHuruf2, nowFontSize);

			applyPreferences();

			if (textAppearancePanel != null) {
				textAppearancePanel.displayValues();
			}
		}

		@Override
		public void onTwofingerDragX(final float dx) {
			if (startDx == Float.MIN_VALUE) { // just started
				startDx = dx;

				if (dx < 0) {
					bRight_click();
				} else {
					bLeft_click();
				}
			} else { // more
				// more to the left
				while (dx < startDx - chapterSwipeCellWidth) {
					startDx -= chapterSwipeCellWidth;
					bRight_click();
				}

				while (dx > startDx + chapterSwipeCellWidth) {
					startDx += chapterSwipeCellWidth;
					bLeft_click();
				}
			}
		}

		@Override
		public void onTwofingerDragY(final float dy) {
			if (!moreSwipeYAllowed) return;

			if (dy < 0) {
				App.trackEvent("text_twofinger_up");
				setFullScreen(true);
				leftDrawer.getHandle().setFullScreen(true);
				moreSwipeYAllowed = false;
			} else {
				App.trackEvent("text_twofinger_down");
				setFullScreen(false);
				leftDrawer.getHandle().setFullScreen(false);
				moreSwipeYAllowed = false;
			}
		}

		@Override
		public void onTwofingerEnd(final TwofingerLinearLayout.Mode mode) {
			startFontSize = 0;
			startDx = Float.MIN_VALUE;
			moreSwipeYAllowed = true;
		}
	};

	DrawerLayout drawerLayout;
	LeftDrawer.Text leftDrawer;

	FrameLayout overlayContainer;
	ViewGroup root;
	Toolbar toolbar;
	VersesView lsSplit0;
	VersesView lsSplit1;
	TextView tSplitEmpty;
	TwofingerLinearLayout splitRoot;
	LabeledSplitHandleButton splitHandleButton;
	GotoButton bGoto;
	ImageButton bLeft;
	ImageButton bRight;
	TextView bVersion;
	Floater floater;

	Book activeBook;
	int chapter_1 = 0;
	boolean fullScreen;
	Toast fullScreenToast;

	History history;
	NfcAdapter nfcAdapter;
	ActionMode actionMode;
	boolean dictionaryMode;
	TextAppearancePanel textAppearancePanel;

	// temporary states
	Boolean hasEsvsbAsal;
	Version activeSplitVersion;
	String activeSplitVersionId;

	final CallbackSpan.OnClickListener<Object> parallelListener = (widget, data) -> {
		if (data instanceof String) {
			final int ari = jumpTo((String) data);
			if (ari != 0) {
				history.add(ari);
			}
		} else if (data instanceof Integer) {
			final int ari = (Integer) data;
			jumpToAri(ari);
			history.add(ari);
		}
	};

	final CallbackSpan.OnClickListener<SingleViewVerseAdapter.DictionaryLinkInfo> dictionaryListener = (widget, data) -> {
		final ContentResolver cr = getContentResolver();
		final Uri uri = Uri.parse("content://org.sabda.kamus.provider/define").buildUpon()
				.appendQueryParameter("key", data.key)
				.appendQueryParameter("mode", "snippet")
				.build();

		final Cursor c = cr.query(uri, null, null, null, null);
		if (c == null) {
			OtherAppIntegration.askToInstallDictionary(this);
		} else {
			try {
				if (c.getCount() == 0) {
					new MaterialDialog.Builder(this)
							.content(R.string.dict_no_results)
							.positiveText(R.string.ok)
							.show();
				} else {
					c.moveToNext();
					final Spanned rendered = Html.fromHtml(c.getString(c.getColumnIndexOrThrow("definition")));
					final SpannableStringBuilder sb = rendered instanceof SpannableStringBuilder ? (SpannableStringBuilder) rendered : new SpannableStringBuilder(rendered);

					// remove links
					for (final URLSpan span : sb.getSpans(0, sb.length(), URLSpan.class)) {
						sb.removeSpan(span);
					}

					new MaterialDialog.Builder(this)
							.title(data.orig_text)
							.content(sb)
							.positiveText(R.string.dict_open_full)
							.callback(new MaterialDialog.ButtonCallback() {
								@Override
								public void onPositive(final MaterialDialog dialog) {
									final Intent intent = new Intent("org.sabda.kamus.action.VIEW");
									intent.putExtra("key", data.key);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

									try {
										startActivity(intent);
									} catch (ActivityNotFoundException e) {
										OtherAppIntegration.askToInstallDictionary(IsiActivity.this);
									}
								}
							})
							.show();
				}
			} finally {
				c.close();
			}
		}
	};

	final ViewTreeObserver.OnGlobalLayoutListener splitRoot_globalLayout = new ViewTreeObserver.OnGlobalLayoutListener() {
		Point lastSize;

		@Override
		public void onGlobalLayout() {
			if (lastSize != null && lastSize.x == splitRoot.getWidth() && lastSize.y == splitRoot.getHeight()) {
				return; // no need to layout now
			}

			if (activeSplitVersion == null) {
				return; // we are not splitting
			}

			configureSplitSizes();

			if (lastSize == null) {
				lastSize = new Point();
			}
			lastSize.x = splitRoot.getWidth();
			lastSize.y = splitRoot.getHeight();
		}

	};

	static class IntentResult {
		public int ari;
		public boolean selectVerse;
		public int selectVerseCount;

		public IntentResult(final int ari) {
			this.ari = ari;
		}
	}

	public static Intent createIntent() {
		return new Intent(App.context, IsiActivity.class);
	}

	public static Intent createIntent(int ari) {
		Intent res = new Intent(App.context, IsiActivity.class);
		res.setAction("yuku.alkitab.action.VIEW");
		res.putExtra("ari", ari);
		return res;
	}

	final BroadcastReceiver reloadAttributeMapReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			reloadBothAttributeMaps();
		}
	};

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_isi);

		AdView ad = (AdView) findViewById(R.id.adView);
		ad.loadAd(new AdRequest.Builder().build());


		drawerLayout = V.get(this, R.id.drawerLayout);
		leftDrawer = V.get(this, R.id.left_drawer);
		leftDrawer.configure(this, drawerLayout);

		toolbar = V.get(this, R.id.toolbar);
		setSupportActionBar(toolbar);
		setTitle("");

		final ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

		bGoto = V.get(this, R.id.bGoto);
		bLeft = V.get(this, R.id.bLeft);
		bRight = V.get(this, R.id.bRight);
		bVersion = V.get(this, R.id.bVersion);

		overlayContainer = V.get(this, R.id.overlayContainer);
		root = V.get(this, R.id.root);
		lsSplit0 = V.get(this, R.id.lsSplit0);
		lsSplit1 = V.get(this, R.id.lsSplit1);
		tSplitEmpty = V.get(this, R.id.tSplitEmpty);
		splitRoot = V.get(this, R.id.splitRoot);
		splitRoot.getViewTreeObserver().addOnGlobalLayoutListener(splitRoot_globalLayout);

		splitHandleButton = V.get(this, R.id.splitHandleButton);
		floater = V.get(this, R.id.floater);

		// If layout is changed, updateToolbarLocation must be updated as well. This will be called in DEBUG to make sure
		// updateToolbarLocation is also updated when layout is updated.
		if (BuildConfig.DEBUG) {
			if (root.getChildCount() != 2 || root.getChildAt(0).getId() != R.id.toolbar || root.getChildAt(1).getId() != R.id.splitRoot) {
				throw new RuntimeException("Layout changed and this is no longer compatible with updateToolbarLocation");
			}
		}

		updateToolbarLocation();

		lsSplit0.setName("lsSplit0");
		lsSplit1.setName("lsSplit1");

		splitRoot.setListener(splitRoot_listener);

		bGoto.setOnClickListener(v -> bGoto_click());
		bGoto.setOnLongClickListener(v -> {
			bGoto_longClick();
			return true;
		});
		bGoto.setFloaterDragListener(bGoto_floaterDrag);

		bLeft.setOnClickListener(v -> bLeft_click());
		bRight.setOnClickListener(v -> bRight_click());
		bVersion.setOnClickListener(v -> bVersion_click());

		floater.setListener(floater_listener);

		lsSplit0.setOnKeyListener((v, keyCode, event) -> {
			int action = event.getAction();
			if (action == KeyEvent.ACTION_DOWN) {
				return press(keyCode);
			} else if (action == KeyEvent.ACTION_MULTIPLE) {
				return press(keyCode);
			}
			return false;
		});

		// listeners
		lsSplit0.setParallelListener(parallelListener);
		lsSplit0.setAttributeListener(new AttributeListener()); // have to be distinct from lsSplit1
		lsSplit0.setInlineLinkSpanFactory(new VerseInlineLinkSpanFactory(lsSplit0));
		lsSplit0.setSelectedVersesListener(lsSplit0_selectedVerses);
		lsSplit0.setOnVerseScrollListener(lsSplit0_verseScroll);
		lsSplit0.setDictionaryListener(dictionaryListener);

		// additional setup for split1
		lsSplit1.setVerseSelectionMode(VersesView.VerseSelectionMode.multiple);
		lsSplit1.setEmptyView(tSplitEmpty);
		lsSplit1.setParallelListener(parallelListener);
		lsSplit1.setAttributeListener(new AttributeListener()); // have to be distinct from lsSplit0
		lsSplit1.setInlineLinkSpanFactory(new VerseInlineLinkSpanFactory(lsSplit1));
		lsSplit1.setSelectedVersesListener(lsSplit1_selectedVerses);
		lsSplit1.setOnVerseScrollListener(lsSplit1_verseScroll);
		lsSplit1.setDictionaryListener(dictionaryListener);

		// for splitting
		splitHandleButton.setListener(splitHandleButton_listener);
		splitHandleButton.setButtonPressListener(splitHandleButton_labelPressed);

		// migrate old history?
		History.migrateOldHistoryWhenNeeded();

		history = History.getInstance();

		initNfcIfAvailable();

		final IntentResult intentResult = processIntent(getIntent(), "onCreate");
		final int openingAri;
		final boolean selectVerse;
		final int selectVerseCount;

		if (intentResult == null) {
			// restore the last (version; book; chapter and verse).
			final int lastBookId = Preferences.getInt(Prefkey.lastBookId, 0);
			final int lastChapter = Preferences.getInt(Prefkey.lastChapter, 0);
			final int lastVerse = Preferences.getInt(Prefkey.lastVerse, 0);
			openingAri = Ari.encode(lastBookId, lastChapter, lastVerse);
			selectVerse = false;
			selectVerseCount = 1;
			Log.d(TAG, "Going to the last: bookId=" + lastBookId + " chapter=" + lastChapter + " verse=" + lastVerse);
		} else {
			openingAri = intentResult.ari;
			selectVerse = intentResult.selectVerse;
			selectVerseCount = intentResult.selectVerseCount;
		}

		final String lastVersionId = Preferences.getString(Prefkey.lastVersionId);
		final MVersion mv = getVersionFromVersionId(lastVersionId);

		if (mv != null) {
			loadVersion(mv, false);
		} else {
			loadVersion(S.getMVersionInternal(), false);
		}

		{ // load book
			final Book book = S.activeVersion.getBook(Ari.toBook(openingAri));
			if (book != null) {
				this.activeBook = book;
			} else { // can't load last book or bookId 0
				this.activeBook = S.activeVersion.getFirstBook();
			}

			if (this.activeBook == null) { // version failed to load, so books are also failed to load. Fallback!
				S.activeVersion = VersionImpl.getInternalVersion();
				this.activeBook = S.activeVersion.getFirstBook();
			}
		}

		// load chapter and verse
		display(Ari.toChapter(openingAri), Ari.toVerse(openingAri));

		if (intentResult != null) { // also add to history if not opening the last seen verse
			history.add(openingAri);
		}

		{ // load last split version. This must be after load book, chapter, and verse.
			final String lastSplitVersionId = Preferences.getString(Prefkey.lastSplitVersionId, null);
			if (lastSplitVersionId != null) {
				final String splitOrientation = Preferences.getString(Prefkey.lastSplitOrientation);
				if (LabeledSplitHandleButton.Orientation.horizontal.name().equals(splitOrientation)) {
					splitHandleButton.setOrientation(LabeledSplitHandleButton.Orientation.horizontal);
				} else {
					splitHandleButton.setOrientation(LabeledSplitHandleButton.Orientation.vertical);
				}

				final MVersion splitMv = getVersionFromVersionId(lastSplitVersionId);
				final MVersion splitMvActual = splitMv == null ? S.getMVersionInternal() : splitMv;

				if (loadSplitVersion(splitMvActual)) {
					openSplitDisplay();
					displaySplitFollowingMaster(Ari.toVerse(openingAri));
				}
			}
		}

		if (selectVerse) {
			for (int i = 0; i < selectVerseCount; i++) {
				final int verse_1 = Ari.toVerse(openingAri) + i;
				callAttentionForVerseToBothSplits(verse_1);
			}
		}

		App.getLbm().registerReceiver(reloadAttributeMapReceiver, new IntentFilter(ACTION_ATTRIBUTE_MAP_CHANGED));

		Announce.checkAnnouncements();

		App.getLbm().registerReceiver(needsRestartReceiver, new IntentFilter(ACTION_NEEDS_RESTART));
	}

	void callAttentionForVerseToBothSplits(final int verse_1) {
		lsSplit0.callAttentionForVerse(verse_1);
		if (activeSplitVersion != null) {
			lsSplit1.callAttentionForVerse(verse_1);
		}
	}

	final BroadcastReceiver needsRestartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			needsRestart = true;
		}
	};

	@Override protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		processIntent(intent, "onNewIntent");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		App.getLbm().unregisterReceiver(reloadAttributeMapReceiver);

		App.getLbm().unregisterReceiver(needsRestartReceiver);
	}

	/**
	 * @return non-null if the intent is handled by any of the intent handler (e.g. nfc or VIEW)
	 */
	private IntentResult processIntent(Intent intent, String via) {
		Log.d(TAG, "Got intent via " + via);
		Log.d(TAG, "  action: " + intent.getAction());
		Log.d(TAG, "  data uri: " + intent.getData());
		Log.d(TAG, "  component: " + intent.getComponent());
		Log.d(TAG, "  flags: 0x" + Integer.toHexString(intent.getFlags()));
		Log.d(TAG, "  mime: " + intent.getType());
		Bundle extras = intent.getExtras();
		Log.d(TAG, "  extras: " + (extras == null? "null": extras.size()));
		if (extras != null) {
			for (String key: extras.keySet()) {
				Log.d(TAG, "    " + key + " = " + extras.get(key));
			}
		}

		{
			final IntentResult result = tryGetIntentResultFromBeam(intent);
			if (result != null) return result;
		}

		{
			final IntentResult result = tryGetIntentResultFromView(intent);
			if (result != null) return result;
		}

		return null;
	}

	/** did we get here from VIEW intent? */
	private IntentResult tryGetIntentResultFromView(Intent intent) {
		if (!U.equals(intent.getAction(), "yuku.alkitab.action.VIEW")) return null;

		final boolean selectVerse = intent.getBooleanExtra("selectVerse", false);
		final int selectVerseCount = intent.getIntExtra("selectVerseCount", 1);

		if (intent.hasExtra("ari")) {
			int ari = intent.getIntExtra("ari", 0);
			if (ari != 0) {
				final IntentResult res = new IntentResult(ari);
				res.selectVerse = selectVerse;
				res.selectVerseCount = selectVerseCount;
				return res;
			} else {
				new AlertDialogWrapper.Builder(this)
						.setMessage("Invalid ari: " + ari)
						.setPositiveButton(R.string.ok, null)
						.show();
				return null;
			}
		} else if (intent.hasExtra("lid")) {
			int lid = intent.getIntExtra("lid", 0);
			int ari = LidToAri.lidToAri(lid);
			if (ari != 0) {
				jumpToAri(ari);
				history.add(ari);
				final IntentResult res = new IntentResult(ari);
				res.selectVerse = selectVerse;
				res.selectVerseCount = selectVerseCount;
				return res;
			} else {
				new AlertDialogWrapper.Builder(this)
						.setMessage("Invalid lid: " + lid)
						.setPositiveButton(R.string.ok, null)
						.show();
				return null;
			}
		} else {
			return null;
		}
	}

	private void initNfcIfAvailable() {
		nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
		if (nfcAdapter != null) {
			nfcAdapter.setNdefPushMessageCallback(event -> {
				JSONObject obj = new JSONObject();
				try {
					obj.put("ari", Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, lsSplit0.getVerseBasedOnScroll()));
				} catch (JSONException e) { // won't happen
				}
				byte[] payload = obj.toString().getBytes();
				NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "application/vnd.yuku.alkitab.nfc.beam".getBytes(), new byte[0], payload);
				return new NdefMessage(new NdefRecord[] {
						record,
						NdefRecord.createApplicationRecord(getPackageName()),
				});
			}, this);
		}
	}

	@Override protected void onPause() {
		super.onPause();
		disableNfcForegroundDispatchIfAvailable();
	}

	private void disableNfcForegroundDispatchIfAvailable() {
		if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
	}

	@Override protected void onResume() {
		super.onResume();
		enableNfcForegroundDispatchIfAvailable();
	}

	private void enableNfcForegroundDispatchIfAvailable() {
		if (nfcAdapter != null) {
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, IsiActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
			try {
				ndef.addDataType("application/vnd.yuku.alkitab.nfc.beam");
			} catch (IntentFilter.MalformedMimeTypeException e) {
				throw new RuntimeException("fail mime type", e);
			}
			IntentFilter[] intentFiltersArray = new IntentFilter[] {ndef, };
			nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
		}
	}

	private IntentResult tryGetIntentResultFromBeam(Intent intent) {
		String action = intent.getAction();
		if (!U.equals(action, NfcAdapter.ACTION_NDEF_DISCOVERED)) return null;

		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		if (rawMsgs == null || rawMsgs.length <= 0) return null;

		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		NdefRecord[] records = msg.getRecords();
		if (records.length <= 0) return null;

		String json = new String(records[0].getPayload());
		try {
			JSONObject obj = new JSONObject(json);
			final int ari = obj.optInt("ari", -1);
			if (ari == -1) return null;

			return new IntentResult(ari);
		} catch (JSONException e) {
			Log.e(TAG, "Malformed json from nfc", e);
			return null;
		}
	}

	MVersion getVersionFromVersionId(String versionId) {
		if (versionId == null || MVersionInternal.getVersionInternalId().equals(versionId)) {
			return null; // internal is made the same as null
		}

		// let's look at yes versions
		for (MVersionDb mvDb: S.getDb().listAllVersions()) {
			if (mvDb.getVersionId().equals(versionId)) {
				if (mvDb.hasDataFile()) {
					return mvDb;
				} else {
					return null; // this is the one that should have been chosen, but the data file is not available, so let's fallback.
				}
			}
		}

		return null; // not known
	}

	boolean loadVersion(final MVersion mv, boolean display) {
		try {
			final Version version = mv.getVersion();

			if (version == null) {
				throw new RuntimeException(); // caught below
			}

			if (this.activeBook != null) { // we already have some other version loaded, so make the new version open the same book
				int bookId = this.activeBook.bookId;
				Book book = version.getBook(bookId);
				if (book != null) { // we load the new book succesfully
					this.activeBook = book;
				} else { // too bad, this book was not found, get any book
					this.activeBook = version.getFirstBook();
				}
			}

			S.activeVersion = version;
			S.activeVersionId = mv.getVersionId();
			bVersion.setText(S.getVersionInitials(version));
			splitHandleButton.setLabel1("\u25b2 " + getSplitHandleVersionName(mv, version));

			if (display) {
				display(chapter_1, lsSplit0.getVerseBasedOnScroll(), false);
			}

			App.getLbm().sendBroadcast(new Intent(ACTION_ACTIVE_VERSION_CHANGED));

			return true;
		} catch (Throwable e) { // so we don't crash on the beginning of the app
			Log.e(TAG, "Error opening main version", e);

			new AlertDialogWrapper.Builder(IsiActivity.this)
					.setMessage(getString(R.string.version_error_opening, mv.longName))
					.setPositiveButton(R.string.ok, null)
					.show();

			return false;
		}
	}

	boolean loadSplitVersion(final MVersion mv) {
		try {
			final Version version = mv.getVersion();

			if (version == null) {
				throw new RuntimeException(); // caught below
			}

			activeSplitVersion = version;
			activeSplitVersionId = mv.getVersionId();
			splitHandleButton.setLabel2(getSplitHandleVersionName(mv, version) + " \u25bc");

			configureTextAppearancePanelForSplitVersion();

			return true;
		} catch (Throwable e) { // so we don't crash on the beginning of the app
			Log.e(TAG, "Error opening split version", e);

			new AlertDialogWrapper.Builder(IsiActivity.this)
					.setMessage(getString(R.string.version_error_opening, mv.longName))
					.setPositiveButton(R.string.ok, null)
					.show();

			return false;
		}
	}

	private void configureTextAppearancePanelForSplitVersion() {
		if (textAppearancePanel != null) {
			if (activeSplitVersion == null) {
				textAppearancePanel.clearSplitVersion();
			} else {
				textAppearancePanel.setSplitVersion(activeSplitVersionId, activeSplitVersion.getLongName());
			}
		}
	}

	String getSplitHandleVersionName(MVersion mv, Version version) {
		String shortName = version.getShortName();
		if (shortName != null) {
			return shortName;
		} else {
			// try to get it from the model
			if (mv.shortName != null) {
				return mv.shortName;
			}

			return version.getLongName(); // this will not be null
		}
	}

	boolean press(int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			bLeft_click();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			bRight_click();
			return true;
		}

		VersesView.PressResult pressResult = lsSplit0.press(keyCode);
		switch (pressResult.kind) {
			case left:
				bLeft_click();
				return true;
			case right:
				bRight_click();
				return true;
			case consumed:
				if (activeSplitVersion != null) {
					lsSplit1.scrollToVerse(pressResult.targetVerse_1);
				}
				return true;
			default:
				return false;
		}
	}

	/**
	 * Jump to a given verse reference in string format.
	 * @return ari of the parsed reference
	 */
	int jumpTo(String reference) {
		if (reference.trim().length() == 0) {
			return 0;
		}

		Log.d(TAG, "going to jump to " + reference);

		Jumper jumper = new Jumper(reference);
		if (! jumper.getParseSucceeded()) {
			new MaterialDialog.Builder(this)
					.content(R.string.alamat_tidak_sah_alamat, reference)
					.positiveText(R.string.ok)
					.show();
			return 0;
		}

		int bookId = jumper.getBookId(S.activeVersion.getConsecutiveBooks());
		Book selected;
		if (bookId != -1) {
			Book book = S.activeVersion.getBook(bookId);
			if (book != null) {
				selected = book;
			} else {
				// not avail, just fallback
				selected = this.activeBook;
			}
		} else {
			selected = this.activeBook;
		}

		// set book
		this.activeBook = selected;

		int chapter = jumper.getChapter();
		int verse = jumper.getVerse();
		int ari_cv;
		if (chapter == -1 && verse == -1) {
			ari_cv = display(1, 1);
		} else {
			ari_cv = display(chapter, verse);
		}

		return Ari.encode(selected.bookId, ari_cv);
	}

	/**
	 * Jump to a given ari
	 */
	void jumpToAri(final int ari) {
		if (ari == 0) return;

		final int bookId = Ari.toBook(ari);
		final Book book = S.activeVersion.getBook(bookId);

		if (book == null) {
			Log.w(TAG, "bookId=" + bookId + " not found for ari=" + ari);
			return;
		}

		this.activeBook = book;
		final int ari_cv = display(Ari.toChapter(ari), Ari.toVerse(ari));

		// call attention to the verse only if the displayed verse is equal to the requested verse
		if (ari == Ari.encode(this.activeBook.bookId, ari_cv)) {
			callAttentionForVerseToBothSplits(Ari.toVerse(ari));
		}
	}

	private CharSequence referenceFromSelectedVerses(IntArrayList selectedVerses, Book book) {
		if (selectedVerses.size() == 0) {
			// should not be possible. So we don't do anything.
			return book.reference(this.chapter_1);
		} else if (selectedVerses.size() == 1) {
			return book.reference(this.chapter_1, selectedVerses.get(0));
		} else {
			return book.reference(this.chapter_1, selectedVerses);
		}
	}

	/**
	 * Construct text for copying or sharing (in plain text).
	 * @param isSplitVersion whether take the verse text from the main or from the split version.
	 * @return [0] text for copy/share, [1] text to be submitted to the share url service
	 */
	String[] prepareTextForCopyShare(IntArrayList selectedVerses_1, CharSequence reference, boolean isSplitVersion) {
		final StringBuilder res0 = new StringBuilder();
		final StringBuilder res1 = new StringBuilder();

		res0.append(reference);

		if (Preferences.getBoolean(getString(R.string.pref_copyWithVersionName_key), getResources().getBoolean(R.bool.pref_copyWithVersionName_default))) {
			final Version version = isSplitVersion ? activeSplitVersion : S.activeVersion;
			final String versionShortName = version.getShortName();
			if (versionShortName != null) {
				res0.append(" (").append(versionShortName).append(")");
			}
		}

		if (Preferences.getBoolean(getString(R.string.pref_copyWithVerseNumbers_key), false) && selectedVerses_1.size() > 1) {
			res0.append('\n');

			// append each selected verse with verse number prepended
			for (int i = 0, len = selectedVerses_1.size(); i < len; i++) {
				final int verse_1 = selectedVerses_1.get(i);
				final String verseText = isSplitVersion ? lsSplit1.getVerseText(verse_1) : lsSplit0.getVerseText(verse_1);

				if (verseText != null) {
					final String verseTextPlain = U.removeSpecialCodes(verseText);

					res0.append(verse_1);
					res1.append(verse_1);
					res0.append(' ');
					res1.append(' ');

					res0.append(verseTextPlain);
					res1.append(verseText);

					if (i != len - 1) {
						res0.append('\n');
						res1.append('\n');
					}
				}
			}
		} else {
			res0.append("  ");

			// append each selected verse without verse number prepended
			for (int i = 0; i < selectedVerses_1.size(); i++) {
				final int verse_1 = selectedVerses_1.get(i);
				final String verseText = isSplitVersion ? lsSplit1.getVerseText(verse_1) : lsSplit0.getVerseText(verse_1);

				if (verseText != null) {
					final String verseTextPlain = U.removeSpecialCodes(verseText);

					if (i != 0) {
						res0.append('\n');
						res1.append('\n');
					}
					res0.append(verseTextPlain);
					res1.append(verseText);
				}
			}
		}

		return Array(res0.toString(), res1.toString());
	}

	private void applyPreferences() {
		// make sure S applied variables are set first
		S.calculateAppliedValuesBasedOnPreferences();

		{ // apply background color, and clear window background to prevent overdraw
			getWindow().setBackgroundDrawableResource(android.R.color.transparent);
			final int backgroundColor = S.applied.backgroundColor;
			root.setBackgroundColor(backgroundColor);
			lsSplit0.setCacheColorHint(backgroundColor);
			lsSplit1.setCacheColorHint(backgroundColor);

			// ensure scrollbar is visible on Material devices
			if (Build.VERSION.SDK_INT >= 21) {
				final Drawable thumb;
				if (ColorUtils.calculateLuminance(backgroundColor) > 0.5) {
					thumb = getResources().getDrawable(R.drawable.scrollbar_handle_material_for_light, null);
				} else {
					thumb = getResources().getDrawable(R.drawable.scrollbar_handle_material_for_dark, null);
				}
				ScrollbarSetter.setVerticalThumb(lsSplit0, thumb);
				ScrollbarSetter.setVerticalThumb(lsSplit1, thumb);
			}
		}

		// necessary
		lsSplit0.invalidateViews();
		lsSplit1.invalidateViews();

		SettingsActivity.setPaddingBasedOnPreferences(lsSplit0);
		SettingsActivity.setPaddingBasedOnPreferences(lsSplit1);
	}

	@Override protected void onStop() {
		super.onStop();

		Preferences.hold();
		try {
			Preferences.setInt(Prefkey.lastBookId, this.activeBook.bookId);
			Preferences.setInt(Prefkey.lastChapter, chapter_1);
			Preferences.setInt(Prefkey.lastVerse, lsSplit0.getVerseBasedOnScroll());
			Preferences.setString(Prefkey.lastVersionId, S.activeVersionId);
			if (activeSplitVersion == null) {
				Preferences.remove(Prefkey.lastSplitVersionId);
			} else {
				Preferences.setString(Prefkey.lastSplitVersionId, activeSplitVersionId);
				Preferences.setString(Prefkey.lastSplitOrientation, splitHandleButton.getOrientation().name());
			}
		} finally {
			Preferences.unhold();
		}

		history.save();
	}

	@Override protected void onStart() {
		super.onStart();

		applyPreferences();

		getWindow().getDecorView().setKeepScreenOn(Preferences.getBoolean(getString(R.string.pref_keepScreenOn_key), getResources().getBoolean(R.bool.pref_keepScreenOn_default)));

		if (needsRestart) {
			needsRestart = false;

			final Intent originalIntent = getIntent();
			finish();
			startActivity(originalIntent);
		}
	}

	@Override public void onBackPressed() {
		if (textAppearancePanel != null) {
			textAppearancePanel.hide();
			textAppearancePanel = null;
		} else if (fullScreen) {
			setFullScreen(false);
			leftDrawer.getHandle().setFullScreen(false);
		} else {
//			super.onBackPressed();
			showExitDialog();
		}
	}

	void bGoto_click() {
		App.trackEvent("nav_goto_button_click");
		startActivityForResult(GotoActivity.createIntent(this.activeBook.bookId, this.chapter_1, lsSplit0.getVerseBasedOnScroll()), REQCODE_goto);
	}

	void bGoto_longClick() {
		App.trackEvent("nav_goto_button_long_click");
		if (history.getSize() > 0) {
			new MaterialDialog.Builder(this)
					.adapter(historyAdapter, (materialDialog, view, position, charSequence) -> {
						materialDialog.dismiss();
						final int ari = history.getAri(position);
						jumpToAri(ari);
						history.add(ari);
						Preferences.setBoolean(Prefkey.history_button_understood, true);
					})
					.autoDismiss(true)
					.show();
		} else {
			Snackbar.make(root, R.string.recentverses_not_available, Snackbar.LENGTH_SHORT).show();
		}
	}

	private ListAdapter historyAdapter = new EasyAdapter() {

		private final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(App.context);
		private final java.text.DateFormat mediumDateFormat = DateFormat.getMediumDateFormat(App.context);

		final String thisCreatorId = U.getInstallationId();
		int defaultTextColor;

		@Override
		public View newView(final int position, final ViewGroup parent) {
			final View res = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
			final TextView textView = (TextView) res;
			defaultTextColor = textView.getCurrentTextColor();
			return res;
		}

		@Override
		public void bindView(final View view, final int position, final ViewGroup parent) {
			final TextView textView = (TextView) view;

			int ari = history.getAri(position);
			SpannableStringBuilder sb = new SpannableStringBuilder();
			sb.append(S.activeVersion.reference(ari));
			sb.append("  ");
			int sb_len = sb.length();
			sb.append(formatTimestamp(history.getTimestamp(position)));
			sb.setSpan(new ForegroundColorSpan(0xffaaaaaa), sb_len, sb.length(), 0);
			sb.setSpan(new RelativeSizeSpan(0.7f), sb_len, sb.length(), 0);

			textView.setText(sb);

			if (thisCreatorId.equals(history.getCreatorId(position))) {
				textView.setTextColor(defaultTextColor);
			} else {
				textView.setTextColor(getResources().getColor(R.color.escape));
			}
		}

		private CharSequence formatTimestamp(final long timestamp) {
			{
				long now = System.currentTimeMillis();
				long delta = now - timestamp;
				if (delta <= 200000) {
					return getString(R.string.recentverses_just_now);
				} else if (delta <= 3600000) {
					return getString(R.string.recentverses_min_plural_ago, Math.round(delta / 60000.0));
				}
			}

			{
				Calendar now = GregorianCalendar.getInstance();
				Calendar that = GregorianCalendar.getInstance();
				that.setTimeInMillis(timestamp);
				if (now.get(Calendar.YEAR) == that.get(Calendar.YEAR)) {
					if (now.get(Calendar.DAY_OF_YEAR) == that.get(Calendar.DAY_OF_YEAR)) {
						return getString(R.string.recentverses_today_time, timeFormat.format(that.getTime()));
					} else if (now.get(Calendar.DAY_OF_YEAR) == that.get(Calendar.DAY_OF_YEAR) + 1) {
						return getString(R.string.recentverses_yesterday_time, timeFormat.format(that.getTime()));
					}
				}

				return mediumDateFormat.format(that.getTime());
			}
		}

		@Override
		public int getCount() {
			return history.getSize();
		}
	};

	public void buildMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.activity_isi, menu);
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		buildMenu(menu);
		return true;
	}

	@Override public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu != null) {
			buildMenu(menu);
		}

		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				leftDrawer.toggleDrawer();
				return true;
			case R.id.menuSearch:
				App.trackEvent("nav_search_click");
				menuSearch_click();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@TargetApi(19)
	void setFullScreen(boolean yes) {
		if (fullScreen == yes) return; // no change

		final View decorView = getWindow().getDecorView();

		if (yes) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getSupportActionBar().hide();

			if (Build.VERSION.SDK_INT >= 19) {
				decorView.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_IMMERSIVE
				);
			}
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getSupportActionBar().show();

			if (Build.VERSION.SDK_INT >= 19) {
				decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}

		fullScreen = yes;

		updateToolbarLocation();
	}

	void updateToolbarLocation() {
		// 3 kinds of possible layout:
		// - fullscreen
		// - not fullscreen, toolbar at bottom
		// - not fullscreen, toolbar at top

		// root contains exactly 2 children: toolbar and splitRoot. This is checked in DEBUG in onCreate.
		// Need to move toolbar and splitRoot in order to accomplish this.

		if (!fullScreen) {
			root.removeView(toolbar);
			root.removeView(splitRoot);

			if (Preferences.getBoolean(R.string.pref_bottomToolbarOnText_key, R.bool.pref_bottomToolbarOnText_default)) {
				root.addView(splitRoot);
				root.addView(toolbar);
			} else {
				root.addView(toolbar);
				root.addView(splitRoot);
			}
		}
	}

	@Override
	public void onWindowFocusChanged(final boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		final View decorView = getWindow().getDecorView();
		Log.d(TAG, "@@onWindowFocusChanged bef hasFocus=" + hasFocus + " 0x" + Integer.toHexString(decorView.getSystemUiVisibility()));

		if (hasFocus && fullScreen) {
			if (Build.VERSION.SDK_INT >= 19) {
				decorView.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_IMMERSIVE
				);
			}
		}

		Log.d(TAG, "@@onWindowFocusChanged aft hasFocus=" + hasFocus + " 0x" + Integer.toHexString(decorView.getSystemUiVisibility()));
	}

	void setShowTextAppearancePanel(boolean yes) {
		if (yes) {
			if (textAppearancePanel == null) { // not showing yet
				textAppearancePanel = new TextAppearancePanel(this, overlayContainer, new TextAppearancePanel.Listener() {
					@Override public void onValueChanged() {
						applyPreferences();
					}

					@Override
					public void onCloseButtonClick() {
						textAppearancePanel.hide();
						textAppearancePanel = null;
					}
				}, REQCODE_textAppearanceGetFonts, REQCODE_textAppearanceCustomColors);
				configureTextAppearancePanelForSplitVersion();
				textAppearancePanel.show();
			}
		} else {
			if (textAppearancePanel != null) {
				textAppearancePanel.hide();
				textAppearancePanel = null;
			}
		}
	}

	void setNightMode(boolean yes) {
		final boolean previousValue = Preferences.getBoolean(Prefkey.is_night_mode, false);
		if (previousValue == yes) return;

		Preferences.setBoolean(Prefkey.is_night_mode, yes);

		applyPreferences();
		applyActionBarAndStatusBarColors();

		if (textAppearancePanel != null) {
			textAppearancePanel.displayValues();
		}

		App.getLbm().sendBroadcast(new Intent(ACTION_NIGHT_MODE_CHANGED));
	}

	void openVersionsDialog() {
		S.openVersionsDialog(this, false, S.activeVersionId, mv -> loadVersion(mv, true));
	}

	void openSplitVersionsDialog() {
		S.openVersionsDialog(this, true, activeSplitVersionId, mv -> {
			if (mv == null) { // closing split version
				disableSplitVersion();
			} else {
				boolean ok = loadSplitVersion(mv);
				if (ok) {
					openSplitDisplay();
					displaySplitFollowingMaster();
				} else {
					disableSplitVersion();
				}
			}
		});
	}

	void disableSplitVersion() {
		activeSplitVersion = null;
		activeSplitVersionId = null;
		closeSplitDisplay();

		configureTextAppearancePanelForSplitVersion();
	}

	void openSplitDisplay() {
		if (splitHandleButton.getVisibility() == View.VISIBLE) {
			return; // it's already split, no need to do anything
		}

		configureSplitSizes();

		bVersion.setVisibility(View.GONE);
		if (actionMode != null) actionMode.invalidate();
		leftDrawer.getHandle().setSplitVersion(true);
	}

	void configureSplitSizes() {
		splitHandleButton.setVisibility(View.VISIBLE);

		float prop = Preferences.getFloat(Prefkey.lastSplitProp, Float.MIN_VALUE);
		if (prop == Float.MIN_VALUE || prop < 0.f || prop > 1.f) {
			prop = 0.5f; // guard against invalid values
		}

		final int splitHandleThickness = getResources().getDimensionPixelSize(R.dimen.split_handle_thickness);
		if (splitHandleButton.getOrientation() == LabeledSplitHandleButton.Orientation.vertical) {
			splitRoot.setOrientation(LinearLayout.VERTICAL);

			final int totalHeight = splitRoot.getHeight();
			final int masterHeight = (int) ((totalHeight - splitHandleThickness) * prop);

			{ // divide the screen space
				final ViewGroup.LayoutParams lp = lsSplit0.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = masterHeight;
				lsSplit0.setLayoutParams(lp);
			}

			// no need to set height, because it has been set to match_parent, so it takes the remaining space.
			lsSplit1.setVisibility(View.VISIBLE);

			{
				final ViewGroup.LayoutParams lp = splitHandleButton.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = splitHandleThickness;
				splitHandleButton.setLayoutParams(lp);
			}
		} else {
			splitRoot.setOrientation(LinearLayout.HORIZONTAL);

			final int totalWidth = splitRoot.getWidth();
			final int masterWidth = (int) ((totalWidth - splitHandleThickness) * prop);

			{ // divide the screen space
				final ViewGroup.LayoutParams lp = lsSplit0.getLayoutParams();
				lp.width = masterWidth;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				lsSplit0.setLayoutParams(lp);
			}

			// no need to set width, because it has been set to match_parent, so it takes the remaining space.
			lsSplit1.setVisibility(View.VISIBLE);

			{
				final ViewGroup.LayoutParams lp = splitHandleButton.getLayoutParams();
				lp.width = splitHandleThickness;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				splitHandleButton.setLayoutParams(lp);
			}
		}
	}

	void closeSplitDisplay() {
		if (splitHandleButton.getVisibility() == View.GONE) {
			return; // it's already not split, no need to do anything
		}

		splitHandleButton.setVisibility(View.GONE);
		lsSplit1.setVisibility(View.GONE);

		{
			final ViewGroup.LayoutParams lp = lsSplit0.getLayoutParams();
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
			lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
			lsSplit0.setLayoutParams(lp);
		}

		bVersion.setVisibility(View.VISIBLE);
		if (actionMode != null) actionMode.invalidate();
		leftDrawer.getHandle().setSplitVersion(false);
	}

	private void menuSearch_click() {
		startActivity(SearchActivity.createIntent(this.activeBook.bookId));
	}

	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQCODE_goto && resultCode == RESULT_OK) {
			GotoActivity.Result result = GotoActivity.obtainResult(data);
			if (result != null) {
				final int ari_cv;

				if (result.bookId == -1) {
					// stay on the same book
					ari_cv = display(result.chapter_1, result.verse_1);

					// call attention to the verse only if the displayed verse is equal to the requested verse
					if (Ari.encode(0, result.chapter_1, result.verse_1) == ari_cv) {
						callAttentionForVerseToBothSplits(result.verse_1);
					}
				} else {
					// change book
					final Book book = S.activeVersion.getBook(result.bookId);
					if (book != null) {
						this.activeBook = book;
					} else { // no book, just chapter and verse.
						result.bookId = this.activeBook.bookId;
					}

					ari_cv = display(result.chapter_1, result.verse_1);

					// select the verse only if the displayed verse is equal to the requested verse
					if (Ari.encode(result.bookId, result.chapter_1, result.verse_1) == Ari.encode(this.activeBook.bookId, ari_cv)) {
						callAttentionForVerseToBothSplits(result.verse_1);
					}
				}

				if (result.verse_1 == 0 && Ari.toVerse(ari_cv) == 1) {
					// verse 0 requested, but display method causes it to show verse_1 1.
					// However we want to store verse_1 0 on the history.
					history.add(Ari.encode(this.activeBook.bookId, Ari.toChapter(ari_cv), 0));
				} else {
					history.add(Ari.encode(this.activeBook.bookId, ari_cv));
				}
			}
		} else if (requestCode == REQCODE_share && resultCode == RESULT_OK) {
			ShareActivity.Result result = ShareActivity.obtainResult(data);
			if (result != null && result.chosenIntent != null) {
				Intent chosenIntent = result.chosenIntent;
				final String packageName = chosenIntent.getComponent().getPackageName();
				if (U.equals(packageName, "com.facebook.katana")) {
					String verseUrl = chosenIntent.getStringExtra(EXTRA_verseUrl);
					if (verseUrl != null) {
						chosenIntent.putExtra(Intent.EXTRA_TEXT, verseUrl); // change text to url
					}
				} else if (U.equals(packageName, "com.whatsapp")) {
					chosenIntent.removeExtra(Intent.EXTRA_SUBJECT);
				}
				startActivity(chosenIntent);
			}
		} else if (requestCode == REQCODE_textAppearanceGetFonts) {
			if (textAppearancePanel != null) textAppearancePanel.onActivityResult(requestCode, resultCode, data);
		} else if (requestCode == REQCODE_textAppearanceCustomColors) {
			if (textAppearancePanel != null) textAppearancePanel.onActivityResult(requestCode, resultCode, data);
		} else if (requestCode == REQCODE_edit_note_1 && resultCode == RESULT_OK) {
			reloadBothAttributeMaps();
		} else if (requestCode == REQCODE_edit_note_2 && resultCode == RESULT_OK) {
			lsSplit0.uncheckAllVerses(true);
			reloadBothAttributeMaps();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Display specified chapter and verse of the active book. By default all checked verses will be unchecked.
	 * @return Ari that contains only chapter and verse. Book always set to 0.
	 */
	int display(int chapter_1, int verse_1) {
		return display(chapter_1, verse_1, true);
	}

	/**
	 * Display specified chapter and verse of the active book.
	 * @param uncheckAllVerses whether we want to always make all verses unchecked after this operation.
	 * @return Ari that contains only chapter and verse. Book always set to 0.
	 */
	int display(int chapter_1, int verse_1, boolean uncheckAllVerses) {
		int current_chapter_1 = this.chapter_1;

		if (chapter_1 < 1) chapter_1 = 1;
		if (chapter_1 > this.activeBook.chapter_count) chapter_1 = this.activeBook.chapter_count;

		if (verse_1 < 1) verse_1 = 1;
		if (verse_1 > this.activeBook.verse_counts[chapter_1 - 1]) verse_1 = this.activeBook.verse_counts[chapter_1 - 1];

		{ // main
			this.uncheckVersesWhenActionModeDestroyed = false;
			try {
				boolean ok = loadChapterToVersesView(lsSplit0, S.activeVersion, S.activeVersionId, this.activeBook, chapter_1, current_chapter_1, uncheckAllVerses);
				if (!ok) return 0;
			} finally {
				this.uncheckVersesWhenActionModeDestroyed = true;
			}

			// tell activity
			this.chapter_1 = chapter_1;

			lsSplit0.scrollToVerse(verse_1);
		}

		displaySplitFollowingMaster(verse_1);

		// set goto button text
		final String reference = this.activeBook.reference(chapter_1);
		if (Preferences.getBoolean(Prefkey.history_button_understood, false) || history.getSize() == 0) {
			bGoto.setText(reference);
		} else {
			// TODO show something to indicate user can long press on goto button
			bGoto.setText(reference);
		}

		if (fullScreen) {
			if (fullScreenToast == null) {
				fullScreenToast = Toast.makeText(this, reference, Toast.LENGTH_SHORT);
				fullScreenToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
			} else {
				fullScreenToast.setText(reference);
			}
			fullScreenToast.show();
		}

		if (dictionaryMode) {
			finishDictionaryMode();
		}

		return Ari.encode(0, chapter_1, verse_1);
	}

	void displaySplitFollowingMaster() {
		displaySplitFollowingMaster(lsSplit0.getVerseBasedOnScroll());
	}

	private void displaySplitFollowingMaster(int verse_1) {
		if (activeSplitVersion != null) { // split1
			final Book splitBook = activeSplitVersion.getBook(this.activeBook.bookId);
			if (splitBook == null) {
				tSplitEmpty.setText(getString(R.string.split_version_cant_display_verse, this.activeBook.reference(this.chapter_1), activeSplitVersion.getLongName()));
				tSplitEmpty.setTextColor(S.applied.fontColor);
				lsSplit1.setDataEmpty();
			} else {
				this.uncheckVersesWhenActionModeDestroyed = false;
				try {
					loadChapterToVersesView(lsSplit1, activeSplitVersion, activeSplitVersionId, splitBook, this.chapter_1, this.chapter_1, true);
				} finally {
					this.uncheckVersesWhenActionModeDestroyed = true;
				}
				lsSplit1.scrollToVerse(verse_1);
			}
		}
	}

	static boolean loadChapterToVersesView(VersesView versesView, Version version, String versionId, Book book, int chapter_1, int current_chapter_1, boolean uncheckAllVerses) {
		final SingleChapterVerses verses = version.loadChapterText(book, chapter_1);
		if (verses == null) {
			return false;
		}

		//# max is set to 30 (one chapter has max of 30 blocks. Already almost impossible)
		int max = 30;
		int[] pericope_aris = new int[max];
		PericopeBlock[] pericope_blocks = new PericopeBlock[max];
		int nblock = version.loadPericope(book.bookId, chapter_1, pericope_aris, pericope_blocks, max);

		boolean retainSelectedVerses = (!uncheckAllVerses && chapter_1 == current_chapter_1);
		versesView.setDataWithRetainSelectedVerses(retainSelectedVerses, Ari.encode(book.bookId, chapter_1, 0), pericope_aris, pericope_blocks, nblock, verses, version, versionId);

		return true;
	}

	@Override public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (press(keyCode)) return true;
		return super.onKeyDown(keyCode, event);
	}

	@Override public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (press(keyCode)) return true;
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override public boolean onKeyUp(int keyCode, KeyEvent event) {
		final String volumeButtonsForNavigation = Preferences.getString(getString(R.string.pref_volumeButtonNavigation_key), getString(R.string.pref_volumeButtonNavigation_default));
		if (! U.equals(volumeButtonsForNavigation, "default")) { // consume here
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) return true;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected LeftDrawer getLeftDrawer() {
		return leftDrawer;
	}

	void bLeft_click() {
		App.trackEvent("nav_left_click");
		final Book currentBook = this.activeBook;
		if (chapter_1 == 1) {
			// we are in the beginning of the book, so go to prev book
			int tryBookId = currentBook.bookId - 1;
			while (tryBookId >= 0) {
				Book newBook = S.activeVersion.getBook(tryBookId);
				if (newBook != null) {
					this.activeBook = newBook;
					int newChapter_1 = newBook.chapter_count; // to the last chapter
					display(newChapter_1, 1);
					break;
				}
				tryBookId--;
			}
			// whileelse: now is already Genesis 1. No need to do anything
		} else {
			int newChapter = chapter_1 - 1;
			display(newChapter, 1);
		}
	}

	void bRight_click() {
		App.trackEvent("nav_right_click");
		final Book currentBook = this.activeBook;
		if (chapter_1 >= currentBook.chapter_count) {
			final int maxBookId = S.activeVersion.getMaxBookIdPlusOne();
			int tryBookId = currentBook.bookId + 1;
			while (tryBookId < maxBookId) {
				final Book newBook = S.activeVersion.getBook(tryBookId);
				if (newBook != null) {
					this.activeBook = newBook;
					display(1, 1);
					break;
				}
				tryBookId++;
			}
			// whileelse: now is already Revelation (or the last book) at the last chapter. No need to do anything
		} else {
			int newChapter = chapter_1 + 1;
			display(newChapter, 1);
		}
	}

	void bVersion_click() {
		App.trackEvent("nav_version_click");
		openVersionsDialog();
	}

	@Override public boolean onSearchRequested() {
		menuSearch_click();

		return true;
	}

	@Override public void onVerseSelected(XrefDialog dialog, int arif_source, int ari_target) {
		final int ari_source = arif_source >>> 8;

		dialog.dismiss();
		jumpToAri(ari_target);

		// add both xref source and target, so user can go back to source easily
		history.add(ari_source);
		history.add(ari_target);
	}

	class AttributeListener implements VersesView.AttributeListener {
		void openBookmarkDialog(final long _id) {
			final TypeBookmarkDialog dialog = TypeBookmarkDialog.EditExisting(IsiActivity.this, _id);
			dialog.setListener(() -> {
				lsSplit0.reloadAttributeMap();

				if (activeSplitVersion != null) {
					lsSplit1.reloadAttributeMap();
				}
			});
			dialog.show();
		}

		@Override
		public void onBookmarkAttributeClick(final Version version, final String versionId, final int ari) {
			final List<Marker> markers = S.getDb().listMarkersForAriKind(ari, Marker.Kind.bookmark);
			if (markers.size() == 1) {
				openBookmarkDialog(markers.get(0)._id);
			} else {
				final MaterialDialog dialog = new MaterialDialog.Builder(IsiActivity.this)
						.title(R.string.edit_bookmark)
						.adapter(new MultipleMarkerSelectAdapter(version, versionId, markers, Marker.Kind.bookmark), (materialDialog, view, which, text) -> {
							openBookmarkDialog(markers.get(which)._id);
							materialDialog.dismiss();
						})
						.show();

				final ListView listView = dialog.getListView();
				assert listView != null;
				listView.setDrawSelectorOnTop(true);
			}
		}

		void openNoteDialog(final long _id) {
			startActivityForResult(NoteActivity.createEditExistingIntent(_id), REQCODE_edit_note_1);
		}

		@Override
		public void onNoteAttributeClick(final Version version, final String versionId, final int ari) {
			final List<Marker> markers = S.getDb().listMarkersForAriKind(ari, Marker.Kind.note);
			if (markers.size() == 1) {
				openNoteDialog(markers.get(0)._id);
			} else {
				final MaterialDialog dialog = new MaterialDialog.Builder(IsiActivity.this)
						.title(R.string.edit_note)
						.adapter(new MultipleMarkerSelectAdapter(version, versionId, markers, Marker.Kind.note), (materialDialog, view, which, text) -> {
							openNoteDialog(markers.get(which)._id);
							materialDialog.dismiss();
						})
						.show();

				final ListView listView = dialog.getListView();
				assert listView != null;
				listView.setDrawSelectorOnTop(true);
			}
		}

		class MultipleMarkerSelectAdapter extends EasyAdapter {
			final Version version;
			final float textSizeMult;
			final List<Marker> markers;
			final Marker.Kind kind;

			public MultipleMarkerSelectAdapter(final Version version, final String versionId, final List<Marker> markers, final Marker.Kind kind) {
				this.version = version;
				this.textSizeMult = S.getDb().getPerVersionSettings(versionId).fontSizeMultiplier;
				this.markers = markers;
				this.kind = kind;
			}

			@Override
			public Marker getItem(final int position) {
				return markers.get(position);
			}

			@Override
			public View newView(final int position, final ViewGroup parent) {
				return getLayoutInflater().inflate(R.layout.item_marker, parent, false);
			}

			@Override
			public void bindView(final View view, final int position, final ViewGroup parent) {
				final TextView lDate = V.get(view, R.id.lDate);
				final TextView lCaption = V.get(view, R.id.lCaption);
				final TextView lSnippet = V.get(view, R.id.lSnippet);
				final FlowLayout panelLabels = V.get(view, R.id.panelLabels);

				final Marker marker = getItem(position);

				{
					final Date addTime = marker.createTime;
					final Date modifyTime = marker.modifyTime;

					if (addTime.equals(modifyTime)) {
						lDate.setText(Sqlitil.toLocaleDateMedium(addTime));
					} else {
						lDate.setText(getString(R.string.create_edited_modified_time, Sqlitil.toLocaleDateMedium(addTime), Sqlitil.toLocaleDateMedium(modifyTime)));
					}

					Appearances.applyMarkerDateTextAppearance(lDate, textSizeMult);
				}

				final int ari = marker.ari;
				final String reference = version.reference(ari);
				final String caption = marker.caption;

				if (kind == Marker.Kind.bookmark) {
					lCaption.setText(caption);
					Appearances.applyMarkerTitleTextAppearance(lCaption, textSizeMult);

					lSnippet.setVisibility(View.GONE);

					final List<Label> labels = S.getDb().listLabelsByMarker(marker);
					if (labels.size() != 0) {
						panelLabels.setVisibility(View.VISIBLE);
						panelLabels.removeAllViews();
						for (Label label : labels) {
							panelLabels.addView(MarkerListActivity.getLabelView(getLayoutInflater(), panelLabels, label));
						}
					} else {
						panelLabels.setVisibility(View.GONE);
					}

				} else if (kind == Marker.Kind.note) {
					lCaption.setText(reference);
					Appearances.applyMarkerTitleTextAppearance(lCaption, textSizeMult);
					lSnippet.setText(caption);
					Appearances.applyTextAppearance(lSnippet, textSizeMult);
				}

				view.setBackgroundColor(S.applied.backgroundColor);
			}

			@Override
			public int getCount() {
				return markers.size();
			}
		}

		@Override
		public void onProgressMarkAttributeClick(final Version version, final String versionId, final int preset_id) {
			final ProgressMark progressMark = S.getDb().getProgressMarkByPresetId(preset_id);

			ProgressMarkRenameDialog.show(IsiActivity.this, progressMark, new ProgressMarkRenameDialog.Listener() {
				@Override
				public void onOked() {
					lsSplit0.uncheckAllVerses(true);
				}

				@Override
				public void onDeleted() {
					lsSplit0.uncheckAllVerses(true);
				}
			});
		}

		@Override
		public void onHasMapsAttributeClick(final Version version, final String versionId, final int ari) {
			String locale = null;

			if (this == lsSplit0.getAttributeListener()) {
				locale = S.activeVersion.getLocale();
			} else if (this == lsSplit1.getAttributeListener()) {
				locale = activeSplitVersion.getLocale();
			}

			try {
				final Intent intent = new Intent("palki.maps.action.SHOW_MAPS_DIALOG");
				intent.putExtra("ari", ari);

				if (locale != null) {
					intent.putExtra("locale", locale);
				}

				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				new MaterialDialog.Builder(IsiActivity.this)
						.content(R.string.maps_could_not_open)
						.positiveText(R.string.ok)
						.show();
			}
		}
	}

	class VerseInlineLinkSpanFactory implements VerseInlineLinkSpan.Factory {
		private final Object source;

		VerseInlineLinkSpanFactory(final Object source) {
			this.source = source;
		}

		@Override
		public VerseInlineLinkSpan create(final VerseInlineLinkSpan.Type type, final int arif) {
			return new VerseInlineLinkSpan(type, arif, source) {
				@Override
				public void onClick(final Type type, final int arif, final Object source) {
					if (type == Type.xref) {
						final XrefDialog dialog = XrefDialog.newInstance(arif);

						// TODO setSourceVersion here is not restored when dialog is restored
						if (source == lsSplit0) { // use activeVersion
							dialog.setSourceVersion(S.activeVersion, S.activeVersionId);
						} else if (source == lsSplit1) { // use activeSplitVersion
							dialog.setSourceVersion(activeSplitVersion, activeSplitVersionId);
						}

						FragmentManager fm = getSupportFragmentManager();
						dialog.show(fm, XrefDialog.class.getSimpleName());
					} else if (type == Type.footnote) {
						FootnoteEntry fe = null;
						if (source == lsSplit0) { // use activeVersion
							fe = S.activeVersion.getFootnoteEntry(arif);
						} else if (source == lsSplit1) { // use activeSplitVersion
							fe = activeSplitVersion.getFootnoteEntry(arif);
						}

						if (fe != null) {
							final SpannableStringBuilder footnoteText = new SpannableStringBuilder();
							VerseRenderer.appendSuperscriptNumber(footnoteText, arif & 0xff);
							footnoteText.append(" ");

							new AlertDialogWrapper.Builder(IsiActivity.this)
									.setMessage(FormattedTextRenderer.render(fe.content, footnoteText))
									.setPositiveButton(R.string.ok, null)
									.show();
						} else {
							new AlertDialogWrapper.Builder(IsiActivity.this)
									.setMessage(String.format(Locale.US, "Error: footnote arif 0x%08x couldn't be loaded", arif))
									.setPositiveButton(R.string.ok, null)
									.show();
						}
					} else {
						new AlertDialogWrapper.Builder(IsiActivity.this)
								.setMessage("Error: Unknown inline link type: " + type)
								.setPositiveButton("OK", null)
								.show();
					}
				}
			};
		}
	}

	VersesView.SelectedVersesListener lsSplit0_selectedVerses = new VersesView.DefaultSelectedVersesListener() {
		@Override public void onSomeVersesSelected(VersesView v) {
			if (activeSplitVersion != null) {
				// synchronize the selection with the split view
				IntArrayList selectedVerses = v.getSelectedVerses_1();
				lsSplit1.checkVerses(selectedVerses, false);
			}

			if (actionMode == null) {
				actionMode = startSupportActionMode(actionMode_callback);
			}

			if (actionMode != null) {
				actionMode.invalidate();
			}
		}

		@Override public void onNoVersesSelected(VersesView v) {
			if (activeSplitVersion != null) {
				// synchronize the selection with the split view
				lsSplit1.uncheckAllVerses(false);
			}

			if (actionMode != null) {
				actionMode.finish();
				actionMode = null;
			}
		}
	};

	VersesView.SelectedVersesListener lsSplit1_selectedVerses = new VersesView.DefaultSelectedVersesListener() {
		@Override public void onSomeVersesSelected(VersesView v) {
			// synchronize the selection with the main view
			IntArrayList selectedVerses = v.getSelectedVerses_1();
			lsSplit0.checkVerses(selectedVerses, true);
		}

		@Override public void onNoVersesSelected(VersesView v) {
			lsSplit0.uncheckAllVerses(true);
		}
	};

	VersesView.OnVerseScrollListener lsSplit0_verseScroll = new VersesView.OnVerseScrollListener() {
		@Override public void onVerseScroll(VersesView v, boolean isPericope, int verse_1, float prop) {

			if (!isPericope && activeSplitVersion != null) {
				lsSplit1.scrollToVerse(verse_1, prop);
			}
		}

		@Override public void onScrollToTop(VersesView v) {
			if (activeSplitVersion != null) {
				lsSplit1.scrollToTop();
			}
		}
	};

	VersesView.OnVerseScrollListener lsSplit1_verseScroll = new VersesView.OnVerseScrollListener() {
		@Override public void onVerseScroll(VersesView v, boolean isPericope, int verse_1, float prop) {
			if (!isPericope) {
				lsSplit0.scrollToVerse(verse_1, prop);
			}
		}

		@Override public void onScrollToTop(VersesView v) {
			lsSplit0.scrollToTop();
		}
	};

	ActionMode.Callback actionMode_callback = new ActionMode.Callback() {
		private static final int MENU_GROUP_EXTENSIONS = Menu.FIRST + 1;
		private static final int MENU_EXTENSIONS_FIRST_ID = 0x1000;

		List<ExtensionManager.Info> extensions;

		@Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			getMenuInflater().inflate(R.menu.context_isi, menu);

			Log.d(TAG, "@@onCreateActionMode");
			
			/* The following "esvsbasal" thing is a personal thing by yuku that doesn't matter to anyone else.
			 * Please ignore it and leave it intact. */
			if (hasEsvsbAsal == null) {
				try {
					getPackageManager().getApplicationInfo("yuku.esvsbasal", 0);
					hasEsvsbAsal = true;
				} catch (PackageManager.NameNotFoundException e) {
					hasEsvsbAsal = false;
				}
			}

			if (hasEsvsbAsal) {
				MenuItem esvsb = menu.findItem(R.id.menuEsvsb);
				if (esvsb != null) esvsb.setVisible(true);
			}

			// show book name and chapter
			final String reference = activeBook.reference(chapter_1);
			mode.setTitle(reference);

			return true;
		}

		@Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			final MenuItem menuAddBookmark = menu.findItem(R.id.menuAddBookmark);
			final MenuItem menuAddNote = menu.findItem(R.id.menuAddNote);
			final MenuItem menuCompare = menu.findItem(R.id.menuCompare);

			final IntArrayList selected = lsSplit0.getSelectedVerses_1();
			final boolean single = selected.size() == 1;

			boolean contiguous = true;
			if (!single) {
				int next = selected.get(0) + 1;
				for (int i = 1, len = selected.size(); i < len; i++) {
					final int cur = selected.get(i);
					if (next != cur) {
						contiguous = false;
						break;
					}
					next = cur + 1;
				}
			}

			menuAddBookmark.setVisible(contiguous);
			menuAddNote.setVisible(contiguous);
			menuCompare.setVisible(single);

			// just "copy" or ("copy primary" "copy secondary" "copy both")
			// same with "share".
			final MenuItem menuCopy = menu.findItem(R.id.menuCopy);
			final MenuItem menuCopySplit0 = menu.findItem(R.id.menuCopySplit0);
			final MenuItem menuCopySplit1 = menu.findItem(R.id.menuCopySplit1);
			final MenuItem menuCopyBothSplits = menu.findItem(R.id.menuCopyBothSplits);
			final MenuItem menuShare = menu.findItem(R.id.menuShare);
			final MenuItem menuShareSplit0 = menu.findItem(R.id.menuShareSplit0);
			final MenuItem menuShareSplit1 = menu.findItem(R.id.menuShareSplit1);
			final MenuItem menuShareBothSplits = menu.findItem(R.id.menuShareBothSplits);

			final boolean split = activeSplitVersion != null;

			menuCopy.setVisible(!split);
			menuCopySplit0.setVisible(split);
			menuCopySplit1.setVisible(split);
			menuCopyBothSplits.setVisible(split);
			menuShare.setVisible(!split);
			menuShareSplit0.setVisible(split);
			menuShareSplit1.setVisible(split);
			menuShareBothSplits.setVisible(split);

			// show selected verses
			if (single) {
				mode.setSubtitle(R.string.verse_select_one_verse_selected);
			} else {
				mode.setSubtitle(getString(R.string.verse_select_multiple_verse_selected, selected.size()));
			}

			final MenuItem menuGuide = menu.findItem(R.id.menuGuide);
			final MenuItem menuCommentary = menu.findItem(R.id.menuCommentary);
			final MenuItem menuDictionary = menu.findItem(R.id.menuDictionary);

			// force-show these items on sw600dp, otherwise never show
			final int showAsAction = getResources().getConfiguration().screenWidthDp >= 600 ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER;
			menuGuide.setShowAsActionFlags(showAsAction);
			menuCommentary.setShowAsActionFlags(showAsAction);
			menuDictionary.setShowAsActionFlags(showAsAction);

			// set visibility according to appconfig
			final AppConfig c = AppConfig.get();
			menuGuide.setVisible(c.menuGuide);
			menuCommentary.setVisible(c.menuCommentary);

			// do not show dictionary item if not needed because of auto-lookup from
			menuDictionary.setVisible(c.menuDictionary
							&& !Preferences.getBoolean(getString(R.string.pref_autoDictionaryAnalyze_key), getResources().getBoolean(R.bool.pref_autoDictionaryAnalyze_default))
			);

			{ // extensions
				extensions = ExtensionManager.getExtensions();

				menu.removeGroup(MENU_GROUP_EXTENSIONS);

				for (int i = 0; i < extensions.size(); i++) {
					final ExtensionManager.Info extension = extensions.get(i);
					if (single || (/* not single */ extension.supportsMultipleVerses)) {
						menu.add(MENU_GROUP_EXTENSIONS, MENU_EXTENSIONS_FIRST_ID + i, 0, extension.label);
					}
				}
			}

			return true;
		}

		@Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			final IntArrayList selected = lsSplit0.getSelectedVerses_1();

			if (selected.size() == 0) return true;

			final CharSequence reference = referenceFromSelectedVerses(selected, activeBook);

			final int itemId = item.getItemId();

			switch (itemId) {
				case R.id.menuCopy:
				case R.id.menuCopySplit0:
				case R.id.menuCopySplit1:
				case R.id.menuCopyBothSplits: { // copy, can be multiple verses
					final String[] t;

					if (itemId == R.id.menuCopy || itemId == R.id.menuCopySplit0 || itemId == R.id.menuCopyBothSplits) {
						t = prepareTextForCopyShare(selected, reference, false);
					} else { // menuCopySplit1
						t = prepareTextForCopyShare(selected, reference, true);
					}

					if (itemId == R.id.menuCopyBothSplits && activeSplitVersion != null) { // put guard on activeSplitVersion
						appendSplitTextForCopyShare(t);
					}

					final String textToCopy = t[0];
					final String textToSubmit = t[1];

					ShareUrl.make(IsiActivity.this, !Preferences.getBoolean(getString(R.string.pref_copyWithShareUrl_key), getResources().getBoolean(R.bool.pref_copyWithShareUrl_default)), textToSubmit, Ari.encode(activeBook.bookId, chapter_1, 0), selected, reference.toString(), S.activeVersion, MVersionDb.presetNameFromVersionId(S.activeVersionId), new ShareUrl.Callback() {
						@Override
						public void onSuccess(final String shareUrl) {
							U.copyToClipboard(textToCopy + "\n\n" + shareUrl);
						}

						@Override
						public void onUserCancel() {
							U.copyToClipboard(textToCopy);
						}

						@Override
						public void onError(final Exception e) {
							U.copyToClipboard(textToCopy);
						}

						@Override
						public void onFinally() {
							lsSplit0.uncheckAllVerses(true);

							Snackbar.make(root, getString(R.string.alamat_sudah_disalin, reference), Snackbar.LENGTH_SHORT).show();
							mode.finish();
						}
					});
				} return true;
				case R.id.menuShare:
				case R.id.menuShareSplit0:
				case R.id.menuShareSplit1:
				case R.id.menuShareBothSplits: { // share, can be multiple verses
					final String[] t;

					if (itemId == R.id.menuShare || itemId == R.id.menuShareSplit0 || itemId == R.id.menuShareBothSplits) {
						t = prepareTextForCopyShare(selected, reference, false);
					} else { // menuShareSplit1
						t = prepareTextForCopyShare(selected, reference, true);
					}

					if (itemId == R.id.menuShareBothSplits && activeSplitVersion != null) { // put guard on activeSplitVersion
						appendSplitTextForCopyShare(t);
					}

					final String textToShare = t[0];
					final String textToSubmit = t[1];

					final Intent intent = ShareCompat.IntentBuilder.from(IsiActivity.this)
							.setType("text/plain")
							.setSubject(reference.toString())
							.getIntent();

					ShareUrl.make(IsiActivity.this, !Preferences.getBoolean(getString(R.string.pref_copyWithShareUrl_key), getResources().getBoolean(R.bool.pref_copyWithShareUrl_default)), textToSubmit, Ari.encode(activeBook.bookId, chapter_1, 0), selected, reference.toString(), S.activeVersion, MVersionDb.presetNameFromVersionId(S.activeVersionId), new ShareUrl.Callback() {
						@Override
						public void onSuccess(final String shareUrl) {
							intent.putExtra(Intent.EXTRA_TEXT, textToShare + "\n\n" + shareUrl);
							intent.putExtra(EXTRA_verseUrl, shareUrl);
						}

						@Override
						public void onUserCancel() {
							intent.putExtra(Intent.EXTRA_TEXT, textToShare);
						}

						@Override
						public void onError(final Exception e) {
							intent.putExtra(Intent.EXTRA_TEXT, textToShare);
						}

						@Override
						public void onFinally() {
							startActivityForResult(ShareActivity.createIntent(intent, getString(R.string.bagikan_alamat, reference)), REQCODE_share);

							lsSplit0.uncheckAllVerses(true);
							mode.finish();
						}
					});
				} return true;
				case R.id.menuCompare: {
					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, selected.get(0));
					final VersesDialog dialog = VersesDialog.newCompareInstance(ari);
					dialog.show(getSupportFragmentManager(), "compare_dialog");
					dialog.setListener(new VersesDialog.VersesDialogListener() {
						@Override
						public void onComparedVerseSelected(final VersesDialog dialog, final int ari, final MVersion mversion) {
							loadVersion(mversion, true);
							dialog.dismiss();
						}
					});
				} return true;
				case R.id.menuAddBookmark: {
					// contract: this menu only appears when contiguous verses are selected
					if (selected.get(selected.size() - 1) - selected.get(0) != selected.size() - 1) {
						throw new RuntimeException("Non contiguous verses when adding bookmark: " + selected);
					}

					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, selected.get(0));
					final int verseCount = selected.size();

					// always create a new bookmark
					TypeBookmarkDialog dialog = TypeBookmarkDialog.NewBookmark(IsiActivity.this, ari, verseCount);
					dialog.setListener(new TypeBookmarkDialog.Listener() {
						@Override public void onModifiedOrDeleted() {
							lsSplit0.uncheckAllVerses(true);
							reloadBothAttributeMaps();
						}
					});
					dialog.show();

					mode.finish();
				} return true;
				case R.id.menuAddNote: {
					// contract: this menu only appears when contiguous verses are selected
					if (selected.get(selected.size() - 1) - selected.get(0) != selected.size() - 1) {
						throw new RuntimeException("Non contiguous verses when adding note: " + selected);
					}

					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, selected.get(0));
					final int verseCount = selected.size();

					// always create a new note
					startActivityForResult(NoteActivity.createNewNoteIntent(S.activeVersion.referenceWithVerseCount(ari, verseCount), ari, verseCount), REQCODE_edit_note_2);
					mode.finish();
				} return true;
				case R.id.menuAddHighlight: {
					final int ariBc = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, 0);
					int colorRgb = S.getDb().getHighlightColorRgb(ariBc, selected);

					final TypeHighlightDialog.Listener listener = new TypeHighlightDialog.Listener() {
						@Override
						public void onOk(int colorRgb) {
							lsSplit0.uncheckAllVerses(true);
							reloadBothAttributeMaps();
						}
					};

					if (selected.size() == 1) {
						final VerseRenderer.FormattedTextResult ftr = new VerseRenderer.FormattedTextResult();
						final int ari = Ari.encodeWithBc(ariBc, selected.get(0));
						final String rawVerseText = S.activeVersion.loadVerseText(ari);
						final Highlights.Info info = S.getDb().getHighlightColorRgb(ari);

						VerseRenderer.render(null, null, ari, rawVerseText, "" + Ari.toVerse(ari), null, false, false, null, ftr);
						new TypeHighlightDialog(IsiActivity.this, ari, listener, colorRgb, info, reference, ftr.result);
					} else {
						new TypeHighlightDialog(IsiActivity.this, ariBc, selected, listener, colorRgb, reference);
					}
					mode.finish();
				} return true;
				case R.id.menuEsvsb: {
					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, selected.get(0));

					try {
						Intent intent = new Intent("yuku.esvsbasal.action.GOTO");
						intent.putExtra("ari", ari);
						startActivity(intent);
					} catch (Exception e) {
						Log.e(TAG, "ESVSB starting", e);
					}
				} return true;
				case R.id.menuGuide: {
					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, 0);

					try {
						getPackageManager().getPackageInfo("org.sabda.pedia", 0);

						final Intent intent = new Intent("org.sabda.pedia.action.VIEW");
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("ari", ari);
						startActivity(intent);
					} catch (PackageManager.NameNotFoundException e) {
						OtherAppIntegration.openMarket(IsiActivity.this, "org.sabda.pedia");
					}
				} return true;
				case R.id.menuCommentary: {
					final int ari = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, selected.get(0));

					try {
						getPackageManager().getPackageInfo("org.sabda.tafsiran", 0);

						final Intent intent = new Intent("org.sabda.tafsiran.action.VIEW");
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("ari", ari);
						startActivity(intent);
					} catch (PackageManager.NameNotFoundException e) {
						OtherAppIntegration.openMarket(IsiActivity.this, "org.sabda.tafsiran");
					}
				} return true;
				case R.id.menuDictionary: {
					final int ariBc = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, 0);
					final SparseBooleanArray aris = new SparseBooleanArray();
					for (int i = 0, len = selected.size(); i < len; i++) {
						final int verse_1 = selected.get(i);
						final int ari = Ari.encodeWithBc(ariBc, verse_1);
						aris.put(ari, true);
					}

					startDictionaryMode(aris);
				} return true;
				default: if (itemId >= MENU_EXTENSIONS_FIRST_ID && itemId < MENU_EXTENSIONS_FIRST_ID + extensions.size()) {
					final ExtensionManager.Info extension = extensions.get(itemId - MENU_EXTENSIONS_FIRST_ID);

					final Intent intent = new Intent(ExtensionManager.ACTION_SHOW_VERSE_INFO);
					intent.setComponent(new ComponentName(extension.activityInfo.packageName, extension.activityInfo.name));
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					// prepare extra "aris"
					final int[] aris = new int[selected.size()];
					final int ariBc = Ari.encode(IsiActivity.this.activeBook.bookId, IsiActivity.this.chapter_1, 0);
					for (int i = 0, len = selected.size(); i < len; i++) {
						final int verse_1 = selected.get(i);
						final int ari = Ari.encodeWithBc(ariBc, verse_1);
						aris[i] = ari;
					}
					intent.putExtra("aris", aris);

					if (extension.includeVerseText) {
						// prepare extra "verseTexts"
						final String[] verseTexts = new String[selected.size()];
						for (int i = 0, len = selected.size(); i < len; i++) {
							final int verse_1 = selected.get(i);

							final String verseText = lsSplit0.getVerseText(verse_1);
							if (extension.includeVerseTextFormatting) {
								verseTexts[i] = verseText;
							} else {
								verseTexts[i] = U.removeSpecialCodes(verseText);
							}
						}
						intent.putExtra("verseTexts", verseTexts);
					}

					try {
						startActivity(intent);
					} catch (ActivityNotFoundException e) {
						new MaterialDialog.Builder(IsiActivity.this)
								.content("Error ANFE starting extension\n\n" + extension.activityInfo.packageName + "/" + extension.activityInfo.name)
								.positiveText(R.string.ok)
								.show();
					}

					return true;
				}
			}
			return false;
		}

		/**
		 * @param t [0] is text to copy, [1] is text to submit
		 */
		void appendSplitTextForCopyShare(final String[] t) {
			final Book splitBook = activeSplitVersion.getBook(activeBook.bookId);
			if (splitBook != null) {
				IntArrayList selectedSplit = lsSplit1.getSelectedVerses_1();
				CharSequence referenceSplit = referenceFromSelectedVerses(selectedSplit, splitBook);
				final String[] a = prepareTextForCopyShare(selectedSplit, referenceSplit, true);
				t[0] += "\n\n" + a[0];
				t[1] += "\n\n" + a[1];
			}
		}

		@Override public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;

			// FIXME even with this guard, verses are still unchecked when switching version while both Fullscreen and Split is active.
			// This guard only fixes unchecking of verses when in fullscreen mode.
			if (uncheckVersesWhenActionModeDestroyed) {
				lsSplit0.uncheckAllVerses(true);
			}
		}
	};

	void reloadBothAttributeMaps() {
		lsSplit0.reloadAttributeMap();

		if (activeSplitVersion != null) {
			lsSplit1.reloadAttributeMap();
		}
	}

	final SplitHandleButton.SplitHandleButtonListener splitHandleButton_listener = new SplitHandleButton.SplitHandleButtonListener() {
		int first;
		int handle;
		int root;
		float prop; // proportion from top or left

		@Override public void onHandleDragStart() {
			splitRoot.setOnefingerEnabled(false);

			if (splitHandleButton.getOrientation() == SplitHandleButton.Orientation.vertical) {
				first = lsSplit0.getHeight();
				handle = splitHandleButton.getHeight();
				root = splitRoot.getHeight();
			} else {
				first = lsSplit0.getWidth();
				handle = splitHandleButton.getWidth();
				root = splitRoot.getWidth();
			}

			prop = Float.MIN_VALUE; // guard against glitches
		}

		@Override
		public void onHandleDragMoveX(final float dxSinceLast, final float dxSinceStart) {
			final int newW = (int) (first + dxSinceStart);
			final int maxW = root - handle;
			final ViewGroup.LayoutParams lp = lsSplit0.getLayoutParams();
			lp.width = newW < 0? 0: newW > maxW? maxW: newW;
			lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
			lsSplit0.setLayoutParams(lp);
			prop = (float) lp.width / maxW;
		}

		@Override public void onHandleDragMoveY(float dySinceLast, float dySinceStart) {
			final int newH = (int) (first + dySinceStart);
			final int maxH = root - handle;
			final ViewGroup.LayoutParams lp = lsSplit0.getLayoutParams();
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
			lp.height = newH < 0? 0: newH > maxH? maxH: newH;
			lsSplit0.setLayoutParams(lp);
			prop = (float) lp.height / maxH;
		}

		@Override public void onHandleDragStop() {
			splitRoot.setOnefingerEnabled(true);

			if (prop != Float.MIN_VALUE) {
				Preferences.setFloat(Prefkey.lastSplitProp, prop);
			}
		}
	};

	LabeledSplitHandleButton.ButtonPressListener splitHandleButton_labelPressed = which -> {
		switch (which) {
			case rotate:
				closeSplitDisplay();
				openSplitDisplay();
				break;
			case start:
				openVersionsDialog();
				break;
			case end:
				openSplitVersionsDialog();
				break;
		}
	};

	void startDictionaryMode(final SparseBooleanArray aris) {
		if (!OtherAppIntegration.hasIntegratedDictionaryApp()) {
			OtherAppIntegration.askToInstallDictionary(this);
			return;
		}

		dictionaryMode = true;
		lsSplit0.setDictionaryModeAris(aris);
		lsSplit1.setDictionaryModeAris(aris);
	}

	void finishDictionaryMode() {
		dictionaryMode = false;
		lsSplit0.setDictionaryModeAris(null);
		lsSplit1.setDictionaryModeAris(null);
	}

	@Override
	public void bMarkers_click() {
		startActivity(MarkersActivity.createIntent());
	}

	@Override
	public void bDisplay_click() {
		App.trackEvent("left_drawer_display_click");
		setShowTextAppearancePanel(textAppearancePanel == null);
	}

	@Override
	public void cFullScreen_checkedChange(final boolean isChecked) {
		App.trackEvent("left_drawer_full_screen_click");
		setFullScreen(isChecked);
	}

	@Override
	public void cNightMode_checkedChange(final boolean isChecked) {
		App.trackEvent("left_drawer_night_mode_click");
		setNightMode(isChecked);
	}

	@Override
	public void cSplitVersion_checkedChange(final SwitchCompat cSplitVersion, final boolean isChecked) {
		App.trackEvent("left_drawer_split_click");
		if (isChecked) {
			cSplitVersion.setChecked(false); // do it later, at the version chooser dialog
			openSplitVersionsDialog();
		} else {
			disableSplitVersion();
		}
	}

	@Override
	public void bProgressMarkList_click() {
		App.trackEvent("left_drawer_progress_mark_list_click");
		if (S.getDb().countAllProgressMarks() > 0) {
			final ProgressMarkListDialog dialog = new ProgressMarkListDialog();
			dialog.show(getSupportFragmentManager(), "dialog_progress_mark_list");
			leftDrawer.closeDrawer();
		} else {
			new AlertDialogWrapper.Builder(this)
					.setMessage(R.string.pm_activate_tutorial)
					.setPositiveButton(R.string.ok, null)
					.show();
		}
	}

	@Override
	public void bProgress_click(final int preset_id) {
		gotoProgressMark(preset_id);
	}

	@Override
	public void bCurrentReadingClose_click() {
		App.trackEvent("left_drawer_current_reading_close_click");

		CurrentReading.clear();
	}

	@Override
	public void bCurrentReadingReference_click() {
		App.trackEvent("left_drawer_current_reading_verse_reference_click");

		final int[] aris = CurrentReading.get();
		if (aris == null) {
			return;
		}

		final int ari_start = aris[0];
		jumpToAri(ari_start);
		history.add(ari_start);

		leftDrawer.closeDrawer();
	}

	private void gotoProgressMark(final int preset_id) {
		final ProgressMark progressMark = S.getDb().getProgressMarkByPresetId(preset_id);
		if (progressMark == null) {
			return;
		}

		final int ari = progressMark.ari;

		if (ari != 0) {
			App.trackEvent("left_drawer_progress_mark_pin_click_succeed");
			jumpToAri(ari);
			history.add(ari);
		} else {
			App.trackEvent("left_drawer_progress_mark_pin_click_failed");
			new AlertDialogWrapper.Builder(this)
					.setMessage(R.string.pm_activate_tutorial)
					.setPositiveButton(R.string.ok, null)
					.show();
		}
	}

	@Override
	public void onProgressMarkSelected(final int preset_id) {
		gotoProgressMark(preset_id);
	}

	public void showExitDialog() {
		// Toast.makeText(appContext, "BAck", Toast.LENGTH_LONG).show();
		AlertDialog.Builder alert = new AlertDialog.Builder(
				IsiActivity.this);
		alert.setTitle(getString(R.string.app_name));
		//alert.setIcon(R.drawable.app_icon);
		alert.setMessage("Are You Sure You Want To Quit?");
		alert.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
										int whichButton) {
						finish();
					}
				});

		alert.setNegativeButton("Rate App",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

						final String appName = getPackageName();//your application package name i.e play store application url
						try {
							startActivity(new Intent(Intent.ACTION_VIEW,
									Uri.parse("market://details?id="
											+ appName)));
						} catch (android.content.ActivityNotFoundException anfe) {
							startActivity(new Intent(
									Intent.ACTION_VIEW,
									Uri.parse("http://play.google.com/store/apps/details?id="
											+ appName)));
						}

					}
				});


		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {

			}
		});
		alert.show();
	}

}
