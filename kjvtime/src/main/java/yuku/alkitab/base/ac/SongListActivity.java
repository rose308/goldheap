package yuku.alkitab.base.ac;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import yuku.afw.App;
import yuku.afw.V;
import yuku.alkitab.base.S;
import yuku.alkitab.base.ac.base.BaseActivity;
import yuku.alkitab.base.util.SongBookUtil;
import yuku.alkitab.base.util.SongFilter;
import yuku.alkitab.debug.R;
import yuku.alkitab.model.SongInfo;

import java.util.ArrayList;
import java.util.List;

/*
 * Everytime we want to do a search, make sure 3 things:
 * 1. setProgressBarIndeterminateVisibility(true);
 * 2. set new search params
 * 3. loader.forceLoad()
 */
public class SongListActivity extends BaseActivity {
	public static final String TAG = SongListActivity.class.getSimpleName();
	
	private static final String EXTRA_bookName = "bookName";
	private static final String EXTRA_code = "code";
	private static final String EXTRA_searchState = "searchState";
	
	SearchView searchView;
	ListView lsSong;
	TextView bChangeBook;
	CheckBox cDeepSearch;
	View panelFilter;
	View circular_progress;

	SongAdapter adapter;
	SongLoader loader;

	PopupMenu popupChangeBook;

	boolean stillUsingInitialSearchState = false;

	public static class Result {
		public String bookName;
		public String code;
		public SearchState last_searchState;
	}
	
	public static class SearchState implements Parcelable {
		public String filter_string;
		public List<SongInfo> result;
		public int selectedPosition;
		public String bookName;
		public boolean deepSearch;
		
		public SearchState(String filter_string, List<SongInfo> result, int selectedPosition, String bookName, boolean deepSearch) {
			this.filter_string = filter_string;
			this.result = result;
			this.selectedPosition = selectedPosition;
			this.bookName = bookName;
			this.deepSearch = deepSearch;
		}
		
		SearchState(Parcel in) {
			filter_string = in.readString();
			in.readList(result = new ArrayList<>(), ((Object) this).getClass().getClassLoader());
			selectedPosition = in.readInt();
			bookName = in.readString();
			deepSearch = in.readByte() != 0;
		}

		@Override public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(filter_string);
			dest.writeList(result);
			dest.writeInt(selectedPosition);
			dest.writeString(bookName);
			dest.writeByte((byte) (deepSearch? 1: 0));
		}
		
		@Override public int describeContents() {
			return 0;
		}
		
	    public static final Creator<SearchState> CREATOR = new Creator<SearchState>() {
	        @Override public SearchState createFromParcel(Parcel in) {
	            return new SearchState(in);
	        }

	        @Override public SearchState[] newArray(int size) {
	            return new SearchState[size];
	        }
	    };
	}
	
	public static Intent createIntent(@Nullable SearchState searchState_optional) {
		Intent res = new Intent(App.context, SongListActivity.class);
		if (searchState_optional != null) res.putExtra(EXTRA_searchState, searchState_optional);
		return res;
	}
	
	public static Result obtainResult(Intent data) {
		if (data == null) return null;
		Result res = new Result();
		res.bookName = data.getStringExtra(EXTRA_bookName);
		res.code = data.getStringExtra(EXTRA_code);
		res.last_searchState = data.getParcelableExtra(EXTRA_searchState);
		return res;
	}
	
	@Override protected void onCreate(Bundle savedInstanceState) {
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_song_list);

		circular_progress = V.get(this, R.id.progress_circular);

		final Toolbar toolbar = V.get(this, R.id.toolbar);
		setSupportActionBar(toolbar); // must be done first before below lines
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationOnClickListener(v -> navigateUp());

		setSupportProgressBarIndeterminate(true);

		searchView = V.get(this, R.id.searchView);
		lsSong = V.get(this, R.id.lsSong);
		bChangeBook = V.get(this, R.id.bChangeBook);
		cDeepSearch = V.get(this, R.id.cDeepSearch);
		panelFilter = V.get(this, R.id.panelFilter);
		
		searchView.setSubmitButtonEnabled(false);
		searchView.setOnQueryTextListener(searchWidget_queryText);
		
		lsSong.setAdapter(adapter = new SongAdapter());
		lsSong.setOnItemClickListener(lsSong_itemClick);
		
		popupChangeBook = SongBookUtil.getSongBookPopupMenu(this, true, false, searchView);
		popupChangeBook.setOnMenuItemClickListener(SongBookUtil.getSongBookOnMenuItemClickListener(songBookSelected));
		
		bChangeBook.setOnClickListener(v -> popupChangeBook.show());
		cDeepSearch.setOnCheckedChangeListener((buttonView, isChecked) -> startSearch());
		
		loader = new SongLoader();

		// initial
		bChangeBook.setText(R.string.sn_bookselector_all);

        SearchState searchState = getIntent().getParcelableExtra(EXTRA_searchState);
        if (searchState != null) {
        	stillUsingInitialSearchState = true; { // prevent triggering
	        	searchView.setQuery(searchState.filter_string, false);
	        	adapter.setData(searchState.result);
	        	cDeepSearch.setChecked(searchState.deepSearch);
	    		lsSong.setSelection(searchState.selectedPosition);
	    		loader.setSelectedBookName(searchState.bookName);
	    		if (searchState.bookName == null) {
	    			bChangeBook.setText(R.string.sn_bookselector_all);
	    		} else {
					bChangeBook.setText(SongBookUtil.escapeSongBookName(searchState.bookName));
				}
			} stillUsingInitialSearchState = false;
        	setCustomProgressBarIndeterminateVisible(false); // somehow this is needed.
        } else {
        	startSearch();
        }
        
        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<List<SongInfo>>() {
        	@Override public Loader<List<SongInfo>> onCreateLoader(int id, Bundle args) {
        		return loader;
        	}
        	
        	@Override public void onLoadFinished(Loader<List<SongInfo>> loader, List<SongInfo> data) {
        		adapter.setData(data);
        		setCustomProgressBarIndeterminateVisible(false);
        	}
        	
        	@Override public void onLoaderReset(Loader<List<SongInfo>> loader) {
        		adapter.setData(null);
        		setCustomProgressBarIndeterminateVisible(false);
        	}
        });
	}

	void startSearch() {
		if (stillUsingInitialSearchState) return;
		setCustomProgressBarIndeterminateVisible(true);
		loader.setFilterString(searchView.getQuery().toString());
		loader.setDeepSearch(cDeepSearch.isChecked());
		loader.forceLoad();
	}
	
	void startSearchSettingBookName(String selectedBookName) {
		loader.setSelectedBookName(selectedBookName);
		startSearch();
	}

	final SongBookUtil.OnSongBookSelectedListener songBookSelected = new SongBookUtil.DefaultOnSongBookSelectedListener() {
		@Override
		public void onAllSelected() {
			bChangeBook.setText(R.string.sn_bookselector_all);
			startSearchSettingBookName(null);
		}

		@Override
		public void onSongBookSelected(final String name) {
			bChangeBook.setText(SongBookUtil.escapeSongBookName(name));
			startSearchSettingBookName(name);
		}
	};

	private AdapterView.OnItemClickListener lsSong_itemClick = new AdapterView.OnItemClickListener() {
		@Override public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			SongInfo songInfo = adapter.getItem(position);
			Intent data = new Intent();
			data.putExtra(EXTRA_bookName, songInfo.bookName);
			data.putExtra(EXTRA_code, songInfo.code);

			// do not pass to Binder more than 1000 songs, because it might exceed Binder data limit
			final List<SongInfo> adapterData = adapter.getData();
			if (adapterData.size() <= 1000) {
				data.putExtra(EXTRA_searchState, new SearchState(searchView.getQuery().toString(), adapterData, position, loader.getSelectedBookName(), cDeepSearch.isChecked()));
			}

			setResult(RESULT_OK, data);
			finish();
		}
	};

	private SearchView.OnQueryTextListener searchWidget_queryText = new SearchView.OnQueryTextListener() {
		@Override
		public boolean onQueryTextSubmit(final String query) {
			startSearch();
			return true;
		}

		@Override
		public boolean onQueryTextChange(final String newText) {
			startSearch();
			return true;
		}
	};

	public class SongAdapter extends BaseAdapter {
		List<SongInfo> list;
		
		@Override public int getCount() {
			return list == null? 0: list.size();
		}

		public List<SongInfo> getData() {
			return this.list;
		}

		public void setData(List<SongInfo> data) {
			this.list = data;
			notifyDataSetChanged();
		}

		@Override public SongInfo getItem(int position) {
			return list.get(position);
		}

		@Override public long getItemId(int position) {
			return position;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {
			View res = convertView != null? convertView: getLayoutInflater().inflate(R.layout.item_song, parent, false);
			
			TextView lTitle = V.get(res, R.id.lTitle);
			TextView lTitleOriginal = V.get(res, R.id.lTitleOriginal);
			TextView lBookName = V.get(res, R.id.lBookName);
			
			SongInfo songInfo = getItem(position);
			lTitle.setText(songInfo.code + ". " + songInfo.title);
			if (songInfo.title_original != null) {
				lTitleOriginal.setVisibility(View.VISIBLE);
				lTitleOriginal.setText(songInfo.title_original);
			} else {
				lTitleOriginal.setVisibility(View.GONE);
			}
			lBookName.setText(SongBookUtil.escapeSongBookName(songInfo.bookName));
			
			return res;
		}
	}
	
	static class SongLoader extends AsyncTaskLoader<List<SongInfo>> {
		public static final String TAG = SongLoader.class.getSimpleName();
		
		private String filter_string;
		private String selectedBookName;
		private boolean deepSearch;

		public SongLoader() {
			super(App.context);
		}
		
		public void setDeepSearch(boolean deepSearch) {
			this.deepSearch = deepSearch;
		}

		public void setFilterString(String s) {
			if (TextUtils.isEmpty(s) || s.trim().length() == 0) {
				filter_string = null;
			} else {
				filter_string = s.trim();
			}
		}
		
		public String getSelectedBookName() {
			return selectedBookName;
		}
		
		public void setSelectedBookName(String bookName) {
			this.selectedBookName = bookName;
		}

		@Override public List<SongInfo> loadInBackground() {
			List<SongInfo> res;
			if (!deepSearch) {
				List<SongInfo> songInfos = S.getSongDb().listSongInfosByBookName(getSelectedBookName());
				res = SongFilter.filterSongInfosByString(songInfos, filter_string);
			} else {
				res = S.getSongDb().listSongInfosByBookNameAndDeepFilter(getSelectedBookName(), filter_string);
			}
			return res;
		}
	}

	void setCustomProgressBarIndeterminateVisible(final boolean visible) {
		circular_progress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}
}
