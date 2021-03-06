package com.mart.listlock.listlockactivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mart.listlock.R;
import com.mart.listlock.common.Constants;
import com.mart.listlock.common.LogW;
import com.mart.listlock.common.SavedPreferences;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.common.Utils;
import com.mart.listlock.playactivity.MusicService;
import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.request.DownloadReceiver;
import com.mart.listlock.request.DownloadService;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;
import com.mart.listlock.request.TokenSet;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.concurrent.CountDownLatch;

public class ListLockActivity extends AppCompatActivity implements ConnectionStateCallback {

    private static final int REQUEST_CODE = 8038;
    private static final String LOG_TAG = ListLockActivity.class.getName();

    private static boolean adminMode = false;
    private static boolean loggedIn = false;

    private LinearLayout adminModeBanner;
    private AlertDialog pinDialog;
    private AlertDialog newPinDialog;
    private AlertDialog appInfoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listlock);

        Toolbar customToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(customToolbar);

        adminModeBanner = (LinearLayout) findViewById(R.id.admin_mode_banner);

        LogW.turnOn();

        LogW.d(LOG_TAG, "created with" + (savedInstanceState == null ? "out" : "") + " saved bundle");

        updateViews();

        if (SavedPreferences.getRefreshToken(ListLockActivity.this) != null && !loggedIn) {
            LogW.d(LOG_TAG, "refresh token found");
            try {
                final TokenSet tokens = SpotifyWebRequest.refreshAccessToken(SavedPreferences.getRefreshToken(ListLockActivity.this));
                LogW.d(LOG_TAG, "access token refreshed");
                Utils.doWhileLoading(new Utils.Action() {
                    @Override
                    public void execute() {
                        onTokensReceived(tokens);
                    }
                }, ListLockActivity.this);
            } catch (SpotifyWebRequestException e) {
                LogW.e(LOG_TAG, "failed to refresh access token", e);
                SavedPreferences.clearTokenPrefs(this); // Clear tokens so refreshAccessToken will not be called again
                Utils.showTextBriefly(getString(R.string.auto_login_failed), ListLockActivity.this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogW.d(LOG_TAG, "resumed");

        if (inAdminMode()) {
            Utils.setAuthorized(adminModeBanner);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            final AuthenticationResponse.Type type = response.getType();
            LogW.d(LOG_TAG, "login activity response type: " + type);
            switch (type) {
                // Response was successful and contains auth token
                case CODE:
                    Utils.showTextBriefly(getString(R.string.auth_success), ListLockActivity.this);
                    Utils.doWhileLoading(new Utils.Action() {
                        @Override
                        public void execute() {
                            onCodeReceived(response);
                        }
                    }, ListLockActivity.this);
                    break;

                // Auth flow returned an error
                case ERROR:
                    Utils.showTextBriefly(getString(R.string.login_fail), ListLockActivity.this);
                    LogW.e(LOG_TAG, response.getError() != null ? response.getError() : "no error message provided");
                    break;

                // Most likely auth flow was cancelled
                case EMPTY:
                    Utils.showTextBriefly(getString(R.string.login_cancel), ListLockActivity.this);
                    break;

                default:
                    Utils.showTextBriefly(getString(R.string.login_fail), ListLockActivity.this);
            }
        }
    }

    private void onCodeReceived(AuthenticationResponse authResponse) {
        // Once we have obtained an authorization code, we can request the auth and refresh token
        LogW.d(LOG_TAG, "authentication code received");

        final String code = authResponse.getCode();

        try {
            final TokenSet tokens = SpotifyWebRequest.requestTokens(code);
            LogW.d(LOG_TAG, "tokens received");

            SavedPreferences.setAccessToken(ListLockActivity.this, tokens.getAccessToken());
            SavedPreferences.setRefreshToken(ListLockActivity.this, tokens.getRefreshToken());

            onTokensReceived(tokens);
        } catch (SpotifyWebRequestException e) {
            LogW.e(LOG_TAG, "auth request failed", e);
            Utils.showTextBriefly(getString(R.string.login_fail), ListLockActivity.this);
        }
    }

    private void onTokensReceived(final TokenSet tokens) {
        final String accessToken = tokens.getAccessToken();

        if (MusicService.player() == null) {
            Config playerConfig = new Config(getApplicationContext(), accessToken, Constants.CLIENT_ID);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            MusicService.setPlayer(Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    LogW.d(LOG_TAG, "player initialized");
                    player.addConnectionStateCallback(ListLockActivity.this);
                }

                @Override
                public void onError(Throwable error) {
                    LogW.e(LOG_TAG, "error in initialization: " + error.getMessage());
                }
            }));
        } else {
            MusicService.player().login(accessToken);
        }

        initUserInfo(tokens);
    }

    private void initUserInfo(TokenSet tokens) {
        LogW.d(LOG_TAG, "initializing UserInfo");
        try {
            UserInfo.init(tokens);
            loggedIn = true;
            updateViews();
        } catch (SpotifyWebRequestException e) {
            LogW.e(LOG_TAG, "failed to retrieve UserInfo", e);
            Utils.showTextProlonged(getString(R.string.user_info_fail), ListLockActivity.this);
        }
    }

    private void updateViews() {
        LogW.d(LOG_TAG, "updating views");

        final TextView logInText = (TextView) findViewById(R.id.log_in_text);
        final ImageView profileImage = (ImageView) findViewById(R.id.profile_image);

        if (loggedIn) {
            logInText.post(new Runnable() {
                @Override
                public void run() {
                    logInText.setText(String.format(getString(R.string.welcome_user), UserInfo.getDisplayName()));
                }
            });

            if (profileImage.getDrawable() == null && UserInfo.getImage() != null) {
                downloadFileToImageView(UserInfo.getImage(), profileImage);
            }
        } else {
            logInText.post(new Runnable() {
                @Override
                public void run() {
                    logInText.setText(getString(R.string.not_logged_in));
                }
            });

            profileImage.post(new Runnable() {
                @Override
                public void run() {
                    profileImage.setImageDrawable(null);
                }
            });
        }

        swapButtons(loggedIn);
        invalidateOptionsMenu();
    }

    public void onClickLogin(View view) {
        LogW.d(LOG_TAG, "view to log in clicked");
        if (!loggedIn) {
            openLoginWindow(true);
        } else {
            LogW.e(LOG_TAG, "login clicked while logged in; button should be hidden");
        }
    }

    private void openLoginWindow(boolean showDialog) {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.CODE, Constants.REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"})
                .setShowDialog(showDialog)
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    public void onTemporaryError() {
        LogW.d(LOG_TAG, "temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        LogW.d(LOG_TAG, "received connection message: " + message);
    }

    @Override
    public void onLoggedIn() {
        LogW.d(LOG_TAG, "player logged in");
        AccessTokenUpdater.start();
    }

    @Override
    public void onLoggedOut() {
        LogW.d(LOG_TAG, "player logged out");
        AccessTokenUpdater.stop();
    }

    @Override
    public void onLoginFailed(Error e) {
        LogW.e(LOG_TAG, "login error " + e.name());
    }

    public void onClickStart(View view) {
        LogW.d(LOG_TAG, "view to start clicked");
        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    public void onClickAdminModeBanner(View view) {
        LogW.d(LOG_TAG, "view to leave admin mode clicked");
        Utils.setUnauthorized(adminModeBanner, this);
    }

    private void swapButtons(final boolean loggedIn) {
        final Button logInButton = (Button) findViewById(R.id.log_in_button);
        logInButton.post(new Runnable() {
            @Override
            public void run() {
                logInButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
            }
        });

        final Button startButton = (Button) findViewById(R.id.start_button);
        startButton.post(new Runnable() {
            @Override
            public void run() {
                startButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void downloadFileToImageView(final String url, final ImageView imageView) {
        final DownloadReceiver mReceiver = new DownloadReceiver(new Handler(Looper.getMainLooper()));

        final int requestId = 1234;

        mReceiver.setReceiver(new DownloadReceiver.Receiver() {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                switch (resultCode) {
                    case DownloadService.STATUS_RUNNING:
                        LogW.d(LOG_TAG, "downloader: status running received");
                        if (imageView.getDrawable() == null)
                            imageView.setImageDrawable(ContextCompat.getDrawable(ListLockActivity.this, R.drawable.loader));
                        break;
                    case DownloadService.STATUS_FINISHED:
                        LogW.d(LOG_TAG, "downloader: status finished received");

                        int requestIdReceived = resultData.getInt(DownloadService.REQUEST_ID_TAG);
                        LogW.d(LOG_TAG, "downloader: request id expected: " + requestId + "; received: " + requestIdReceived);

                        if (requestIdReceived == requestId) {
                            byte[] data = resultData.getByteArray(DownloadService.BYTE_ARRAY_TAG);

                            if (data == null) {
                                LogW.d(LOG_TAG, "downloader: data is null");
                                Utils.showTextBriefly(getString(R.string.no_profile_image), ListLockActivity.this);
                                imageView.setImageDrawable(null);
                            } else {
                                Drawable image = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(data, 0, data.length));
                                imageView.setImageDrawable(image);
                            }
                        } else {
                            imageView.setImageDrawable(null);
                        }

                        break;
                    case DownloadService.STATUS_ERROR:
                        LogW.d(LOG_TAG, "status error received");
                        imageView.setImageDrawable(null);
                        String error = resultData.getString(Intent.EXTRA_TEXT);
                        LogW.e(LOG_TAG, error);
                        break;
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_SYNC, null, ListLockActivity.this, DownloadService.class);

                intent.putExtra(DownloadService.URL_TAG, url);
                intent.putExtra(DownloadService.RECEIVER_TAG, mReceiver);
                intent.putExtra(DownloadService.REQUEST_ID_TAG, requestId);

                startService(intent);
            }
        }).start();
    }

    @Override
    public void onPause() {
        LogW.d(LOG_TAG, "paused");
        super.onPause();
    }

    @Override
    public void onStop() {
        LogW.d(LOG_TAG, "stopped");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_start, menu);
        MenuItem actionLogOut = menu.findItem(R.id.action_log_out);
        actionLogOut.setVisible(loggedIn);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_log_out:
                LogW.d(LOG_TAG, getString(R.string.action_log_out) + " clicked");

                if (loggedIn) {
                    pinDialog = Utils.doWhenAuthorized(this, new Utils.Action() {
                        @Override
                        public void execute() {
                            if (MusicService.player() != null) {
                                MusicService.player().logout();
                            }

                            SavedPreferences.clearTokenPrefs(ListLockActivity.this);

                            loggedIn = false;
                            updateViews();

                            openLoginWindow(true);
                        }
                    }, adminModeBanner, true);
                } else {
                    LogW.e(LOG_TAG, "log out button should not be visible if loggedIn = true");
                }
                break;
            case R.id.action_pin:
                LogW.d(LOG_TAG, getString(R.string.action_pin) + " clicked");
                pinDialog = Utils.doWhenAuthorized(this, new Utils.Action() {
                    @Override
                    public void execute() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ListLockActivity.this);
                        builder.setMessage(R.string.enter_new_pin);
                        builder.setPositiveButton(getString(R.string.ok), null);

                        final EditText input = new EditText(ListLockActivity.this);
                        input.setInputType(InputType.TYPE_CLASS_NUMBER);

                        input.addTextChangedListener(new TextWatcher() {
                            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                            @Override
                            public void afterTextChanged(Editable s) {
                                final String inputText = input.getText().toString();
                                if (inputText.length() > 4) {
                                    input.setText(inputText.substring(0, inputText.length() - 1));
                                    input.setSelection(4);
                                }
                            }
                        });

                        builder.setView(input);

                        newPinDialog = builder.create();

                        newPinDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                            @Override
                            public void onShow(DialogInterface dialog) {

                                Button b = newPinDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                b.setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View view) {
                                        final String inputText = input.getText().toString();

                                        if (inputText.length() < 4) {
                                            Utils.showTextBriefly(getString(R.string.pin_min_length), ListLockActivity.this);
                                        } else {
                                            SharedPreferences settings = ListLockActivity.this.getSharedPreferences(ListLockActivity.this.getString(R.string.app_name), 0);

                                            SharedPreferences.Editor editor = settings.edit();
                                            editor.putString(Utils.KEY_PIN, input.getText().toString());
                                            editor.commit();
                                            newPinDialog.dismiss();

                                            Utils.showTextBriefly(getString(R.string.new_pin_set), ListLockActivity.this);
                                        }
                                    }
                                });
                            }
                        });

                        newPinDialog.show();
                    }
                }, adminModeBanner, true);
                break;
            case R.id.action_info:
                appInfoDialog = new AppInfoDialog(ListLockActivity.this);
                appInfoDialog.show();
                // Make links clickable
                ((TextView)appInfoDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                break;
            default:
                break;
        }

        return true;
    }

    public static boolean inAdminMode() {
        return adminMode;
    }

    public static void setAdminMode(boolean on) {
        adminMode = on;
    }

//    public boolean isLoggedIn() {
//        return MusicService.player() != null && MusicService.player().isLoggedIn();
//    }

    @Override
    protected void onDestroy() {
        LogW.d(LOG_TAG, "destroyed");

        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }

        if (appInfoDialog != null && appInfoDialog.isShowing()) {
            appInfoDialog.dismiss();
        }

        AuthenticationClient.stopLoginActivity(this, REQUEST_CODE);

        super.onDestroy();
    }

}
