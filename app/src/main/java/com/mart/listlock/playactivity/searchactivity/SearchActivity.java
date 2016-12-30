package com.mart.listlock.playactivity.searchactivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.mart.listlock.common.LogW;
import com.mart.listlock.common.SavedPreferences;
import com.mart.listlock.common.Utils;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.listlockactivity.ListLockActivity;
import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.playactivity.spotifyobjects.SongInfo;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;
import com.mart.listlock.R;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String LOG_TAG = SearchActivity.class.getName();
    private static final int MAX_PAGES = 5;

    private static String keyword;
    private static int pages = 1;
    private static boolean searched;
    private static boolean allowSearching;

    private LinearLayout headerRow;
    private EditText searchField;
    private TableLayout searchResultsList;
    private View focusThief;
    private ScrollView scrollView;
    private LinearLayout adminModeBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        LogW.d(LOG_TAG, "created with" + (savedInstanceState == null ? "out" : "") + " saved bundle");

        headerRow = (LinearLayout) findViewById(R.id.headers);
        searchField = (EditText) findViewById(R.id.search_field);
        searchResultsList = (TableLayout) findViewById(R.id.search_results_list);
        focusThief = findViewById(R.id.focus_thief);
        scrollView = (ScrollView) findViewById(R.id.song_scrollview);
        adminModeBanner = (LinearLayout) findViewById(R.id.admin_mode_banner);

        allowSearching = true;

        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    LogW.d(LOG_TAG,"enter/done pressed");
                    for (int i = 0; i < pages; i++) {
                        search(i);
                    }
                }
                return false;
            }
        });

        // hide keyboard when search field loses focus
        searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        if (searchResultsList.getHeight() <= scrollView.getHeight()) {
            selectPageAndSearch();
        }

        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                View view = scrollView.getChildAt(scrollView.getChildCount() - 1);

                // calculate the scrolldiff
                final int scrollY = scrollView.getScrollY();
                int diff = (view.getBottom() - (scrollView.getHeight() + scrollY));

                // if diff is zero or less, then the bottom has been reached
                if( diff <= 0 ) {
                    final boolean shouldSearch = searched && allowSearching;
                    LogW.d(LOG_TAG, "ScrollView bottom reached; searched = " + searched + "; allowSearching = " + allowSearching);
                    if (shouldSearch) {
                        selectPageAndSearch();
                    }
                }
            }
        });
    }

    private void selectPageAndSearch() {
        allowSearching = false;

        if (!Utils.isNetworkAvailable(this)) {
            Utils.showTextBriefly(getString(R.string.no_internet), this);
        } else if (pages < MAX_PAGES) {
            pages++;
            LogW.d(LOG_TAG, "pages count set to " + pages);
            search(pages - 1);
        } else {
            LogW.d(LOG_TAG, "max pages reached");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogW.d(LOG_TAG, "resumed");

        if (ListLockActivity.inAdminMode()) {
            Utils.setAuthorized(adminModeBanner);
        }

        keyword = SavedPreferences.getKeyword(SearchActivity.this);
        searched = SavedPreferences.getSearched(SearchActivity.this);

        searchField.setText(keyword);

        if (searched) {
            if (searchResultsList.getChildCount() == 0) {
                pages = 1;
                search(0);
            }
        } else {
            searchField.requestFocus();
        }
    }

    public void onClickSearch(View view) {
        LogW.d(LOG_TAG, "view to search clicked");
        // scroll to the top
        allowSearching = false;
        scrollView.setScrollY(0);
        focusThief.requestFocus();
        searchResultsList.removeAllViews();
        pages = 1;
        search(0);
    }

    public void onClickAdminModeBanner(View view) {
        LogW.d(LOG_TAG, "view to leave admin mode clicked");
        Utils.setUnauthorized(adminModeBanner, this);
    }

    private void search(final int pageToSearch) {
        final String enteredKeyword = searchField.getText().toString();

        if (!enteredKeyword.isEmpty()) {
            Thread loadSongs = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        keyword = enteredKeyword;
                        searched = true;

                        final List<SongInfo> results = new ArrayList<>();
                        results.addAll(SpotifyWebRequest.search(keyword, pageToSearch, UserInfo.getAccessToken()));
                        for (int j = 0; j < results.size(); j++) {
                            final SongInfo songInfo = results.get(j);
                            final SongInfoResultRow songInfoResultRow = new SongInfoResultRow(songInfo, SearchActivity.this, headerRow) {
                                @Override
                                protected void onClick(View v) {
                                    LogW.d(LOG_TAG, "song '" + songInfo.getName() + "' clicked");
                                    Intent data = new Intent();
                                    data.putExtra(PlayActivity.KEY_SONG_URI, songInfo.getUri());
                                    setResult(PlayActivity.SEARCH_REQUEST_CODE, data);
                                    finish();
                                }
                            };

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    searchResultsList.addView(songInfoResultRow);
                                }
                            });
                        }

                        allowSearching = true;
                    } catch (SpotifyWebRequestException e) {
                        LogW.e(LOG_TAG, "search failed", e);
                        Utils.showTextBriefly(getString(R.string.search_failed), SearchActivity.this);
                    }
                }
            });

            loadSongs.start();
        } else {
            LogW.d(LOG_TAG, "no keyword specified");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogW.d(LOG_TAG, "paused");

        SavedPreferences.setResults(keyword, searched, SearchActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogW.d(LOG_TAG, "destroyed");
    }
}
