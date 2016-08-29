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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mart.listlock.common.Constants;
import com.mart.listlock.common.LogW;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.request.DownloadService;
import com.mart.listlock.R;
import com.mart.listlock.common.Utils;
import com.mart.listlock.playactivity.MusicService;
import com.mart.listlock.playactivity.PlayActivity;
import com.mart.listlock.request.DownloadReceiver;
import com.mart.listlock.request.SpotifyWebRequestException;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.concurrent.TimeUnit;

public class ListLockActivity extends AppCompatActivity implements ConnectionStateCallback {

    private static final String KEY_ACCESS_TOKEN = "accesstoken";

    private static final int REQUEST_CODE = 8038;
    private static final String LOG_TAG = ListLockActivity.class.getName();

    private static boolean loggedIn = false;
    private static boolean adminMode = false;

    private LinearLayout adminModeBanner;

    @Override
     protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listlock);

        Toolbar customToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(customToolbar);

        adminModeBanner = (LinearLayout) findViewById(R.id.admin_mode_banner);

        LogW.turnOn();

        LogW.d(LOG_TAG, "created with" + (savedInstanceState == null ? "out" : "") + " saved bundle");

        if (!loggedIn) {
            if (!Utils.isNetworkAvailable(this)) {
                Utils.showTextBriefly(getString(R.string.no_internet), this);
            } else {
                AuthenticationClient.openLoginActivity(this, REQUEST_CODE, getDefaultRequest(true));
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

        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);

        final String savedAccessToken = settings.getString(KEY_ACCESS_TOKEN, null);


        final String currentAccessToken = UserInfo.getAccessToken();
        if (currentAccessToken == null) {
            LogW.d(LOG_TAG, "current access token is null");
            if (savedAccessToken == null) {
                LogW.d(LOG_TAG, "no saved access token");
                setLoggedIn(false);
                return;
            } else {
                LogW.d(LOG_TAG, "saved access token restored");
                Utils.doWhileLoading(new Utils.Action() {
                    @Override
                    public void execute() {
                        try {
                            UserInfo.init(savedAccessToken);

                            LogW.d(LOG_TAG, "access token=" + savedAccessToken);

                            if (MusicService.player() == null) {
                                initPlayer(currentAccessToken, UserInfo.getDisplayName());
                            }
                        } catch (SpotifyWebRequestException e) {
                            LogW.e(LOG_TAG, "failed to restore user session", e);
                            Utils.showTextBriefly(getString(R.string.auto_login_failed), ListLockActivity.this);
                        }
                    }
                }, this);
            }
        }

        setLoggedIn(true);
    }

    private AuthenticationRequest getDefaultRequest(boolean showDialog) {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.TOKEN, Constants.REDIRECT_URI);

        builder.setScopes(new String[]{"playlist-read", "playlist-read-private", "streaming"});
        return builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch(response.getType()) {
                case TOKEN:
                    Utils.showTextBriefly(getString(R.string.login_success), ListLockActivity.this);
                    Utils.doWhileLoading(new Utils.Action() {
                        @Override
                        public void execute() {
                            try {
                                UserInfo.init(response.getAccessToken());
                                initPlayer(response.getAccessToken(), UserInfo.getDisplayName());

                                if (UserInfo.getDisplayName() == null || UserInfo.getDisplayName().isEmpty()) {
                                    throw new SpotifyWebRequestException("no user info");
                                }

                                setLoggedIn(true);
                            } catch (SpotifyWebRequestException e) {
                                UserInfo.setDisplayName("unknown");
                                Utils.showTextProlonged(getString(R.string.user_info_fail), ListLockActivity.this);
                                LogW.e(LOG_TAG, "failed to retrieve UserInfo; UserInfo set to default values", e);
                            }
                        }
                    }, this, getString(R.string.init_spotify_player));
                    break;
                case ERROR:
                    Utils.showTextBriefly(getString(R.string.login_fail), this);
                    break;
                case EMPTY:
                    Utils.showTextBriefly(getString(R.string.login_cancel), this);
                    break;
                default:
                    Utils.showTextBriefly(getString(R.string.login_fail), this);
                    break;
            }
        }
    }

    private void initPlayer(String accessToken, String displayName) {
        Config playerConfig = new Config(this, accessToken, Constants.CLIENT_ID);

        LogW.d(LOG_TAG, "initializing player for username: " + displayName);

        if (MusicService.player() != null) {
            Spotify.destroyPlayer(MusicService.player());
        }

        MusicService.setPlayer(Spotify.getPlayer(playerConfig, MusicService.class, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer player) {
                MusicService.player().addConnectionStateCallback(ListLockActivity.this);
            }

            @Override
            public void onError(Throwable t) {
                LogW.e(LOG_TAG, "could not initialize player: " + t.getMessage(), t);
                Utils.showTextBriefly(getString(R.string.login_fail), ListLockActivity.this);
            }
        }));

        if (MusicService.player() == null) {
            AuthenticationClient.clearCookies(this);
            AuthenticationClient.openLoginActivity(this, REQUEST_CODE, getDefaultRequest(true));

            setLoggedIn(false);
        }

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
        LogW.d(LOG_TAG, "user logged in");
    }

    @Override
    public void onLoggedOut() {
        LogW.d(LOG_TAG, "user logged out");
    }

    @Override
    public void onLoginFailed(int i) {
        LogW.e(LOG_TAG, "login error " + i);
    }

    public void onClickLogin(View view) {
        LogW.d(LOG_TAG, "view to log in clicked");
        if (!Utils.isNetworkAvailable(this)) {
            Utils.showTextBriefly(getString(R.string.no_internet), this);
        } else {
            AuthenticationClient.openLoginActivity(this, REQUEST_CODE, getDefaultRequest(true));
        }
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

    private void setLoggedIn(boolean in) {
        LogW.d(LOG_TAG, "setting logged in to " + in);
        final TextView logInText = (TextView) findViewById(R.id.log_in_text);
        final ImageView profileImage = (ImageView) findViewById(R.id.profile_image);

        if (in) {
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

        swapButtons(in);
        loggedIn = in;
        invalidateOptionsMenu();
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
        super.onPause();
        LogW.d(LOG_TAG, "paused");

        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_ACCESS_TOKEN, UserInfo.getAccessToken());

        LogW.d(LOG_TAG, "saving access token=" + UserInfo.getAccessToken());

        editor.apply();
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
                    Utils.doWhenAuthorized(this, new Utils.Action() {
                        @Override
                        public void execute() {
                            if (MusicService.player() != null) {
                                MusicService.player().logout();
                            }

                            UserInfo.setAccessToken(null);
                            AuthenticationClient.clearCookies(ListLockActivity.this);

                            setLoggedIn(false);
                        }
                    }, adminModeBanner, true);
                }
                break;
            case R.id.action_pin:
                LogW.d(LOG_TAG, getString(R.string.action_pin) + " clicked");
                Utils.doWhenAuthorized(this, new Utils.Action() {
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

                        final AlertDialog alertDialog = builder.create();

                        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                            @Override
                            public void onShow(DialogInterface dialog) {

                                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
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
                                            alertDialog.dismiss();

                                            Utils.showTextBriefly(getString(R.string.new_pin_set), ListLockActivity.this);
                                        }
                                    }
                                });
                            }
                        });

                        alertDialog.show();
                    }
                }, adminModeBanner, true);
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

    @Override
    protected void onDestroy() {
        LogW.d(LOG_TAG, "destroyed");
        super.onDestroy();
    }

}
