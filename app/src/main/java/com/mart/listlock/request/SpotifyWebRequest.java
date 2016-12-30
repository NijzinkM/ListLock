package com.mart.listlock.request;

import android.os.AsyncTask;

import com.mart.listlock.common.Constants;
import com.mart.listlock.common.Decoder;
import com.mart.listlock.common.LogW;
import com.mart.listlock.common.Utils;
import com.mart.listlock.playactivity.spotifyobjects.AlbumInfo;
import com.mart.listlock.playactivity.spotifyobjects.ArtistInfo;
import com.mart.listlock.playactivity.spotifyobjects.PlaylistInfo;
import com.mart.listlock.playactivity.spotifyobjects.SpotifySong;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.Info;
import com.mart.listlock.playactivity.spotifyobjects.Album;
import com.mart.listlock.playactivity.spotifyobjects.Artist;
import com.mart.listlock.playactivity.spotifyobjects.SongInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

public class SpotifyWebRequest {

    public static final String BASE_URL = "https://api.spotify.com/v1";
    public static final String ACCOUNT_URL = "https://accounts.spotify.com/api/token";
    public static final int REQUEST_TIME_OUT = 5000;

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String LOG_TAG = SpotifyWebRequest.class.getName();
    private static final String ARTIST_PREFIX = "spotify:artist:";
    private static final String TRACK_PREFIX = "spotify:track:";
    private static final String ALBUM_PREFIX = "spotify:album:";

    public static void requestUserInfo(final String accessToken) throws SpotifyWebRequestException {
        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {

            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject userInfoJSON;

                try {
                    userInfoJSON = new JSONObject(response.getResponseText());
                    UserInfo.setDisplayName(userInfoJSON.getString("display_name"));
                    UserInfo.setCountry(userInfoJSON.getString("country"));
                    UserInfo.setId(userInfoJSON.getLong("id"));
                    UserInfo.setImage(userInfoJSON.getJSONArray("images").getJSONObject(0).getString("url"));
                    UserInfo.setPremium(userInfoJSON.getString("product").equals("premium"));
//                    requestRefreshCode(accessToken);
                    LogW.d(LOG_TAG, String.format("UserInfo: [displayName: %1$s; country: %2$s; id: %3$s; imageURL: %4$s; premium: %5$b]",
                            UserInfo.getDisplayName(), UserInfo.getCountry(), UserInfo.getId(), UserInfo.getImage(), UserInfo.isPremium()));
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/me";

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }
    }

//    private static void requestRefreshCode(String accessToken) {
//        GETResponseHandler<SpotifyWebRequestException> responseHandler = new DefaultGETResponseHandler() {
//            @Override
//            protected void handle200(HTTPResponse response) {
//                if (response == null) {
//                    setException(new SpotifyWebRequestException("response is null"));
//                    return;
//                }
//
//                JSONObject tokensJSON;
//
//                try {
//                    tokensJSON = new JSONObject(response.getResponseText());
//                    UserInfo.setRefreshToken(tokensJSON.getString("refresh_token"));
//                } catch (JSONException e) {
//                    setException(new SpotifyWebRequestException(e));
//                }
//            }
//        };
//
//        final String url = "https://accounts.spotify.com/api/token";
//
//
//    }

    public static SongInfo requestSongInfo(String uri, String accessToken) throws SpotifyWebRequestException {
        final SongInfo songInfo = new SongInfo();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject songInfoJSON;

                try {
                    songInfoJSON = new JSONObject(response.getResponseText());
                    songInfo.override(readSongJSON(songInfoJSON));
                    LogW.d(LOG_TAG, songInfo.toString());
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/tracks/" + uri.substring(TRACK_PREFIX.length());

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return songInfo;
    }

    public static AlbumInfo requestAlbumInfo(String uri, String accessToken) throws SpotifyWebRequestException {
        final AlbumInfo albumInfo = new AlbumInfo();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject albumInfoJSON;

                try {
                    albumInfoJSON = new JSONObject(response.getResponseText());
                    albumInfo.override(readAlbumJSON(albumInfoJSON));
                    LogW.d(LOG_TAG, albumInfo.toString());
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/albums/" + uri.substring(ALBUM_PREFIX.length());

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return albumInfo;
    }

    public static ArtistInfo requestArtistInfo(String uri, String accessToken) throws SpotifyWebRequestException {
        final ArtistInfo artistInfo = new ArtistInfo();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject artistInfoJSON;

                try {
                    artistInfoJSON = new JSONObject(response.getResponseText());
                    artistInfo.override(readArtistJSON(artistInfoJSON));
                    artistInfo.toString();
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/artists/" + uri.substring(ARTIST_PREFIX.length());

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return artistInfo;
    }

    public static List<SongInfo> search(String keyword, int page, String accessToken) throws SpotifyWebRequestException {
        final List<SongInfo> results = new ArrayList<>();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject resultsJSON;

                try {
                    resultsJSON = new JSONObject(response.getResponseText());
                    JSONObject tracks = resultsJSON.getJSONObject("tracks");
                    JSONArray items = tracks.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        SongInfo songInfo = readSongJSON((JSONObject) items.get(i));
                        LogW.d(LOG_TAG, "song found: " + songInfo.getName());
                        results.add(songInfo);
                    }
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final int limit = 15;
        final String url = BASE_URL + "/search?q=" + keyword.replace(" ", "%20") + "&type=track&market=" + UserInfo.getCountry() + "&limit=" + limit + "&offset=" + limit * page;

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return results;
    }

    public static List<PlaylistInfo> requestPlaylists(long id, String accessToken) throws SpotifyWebRequestException {
        final List<PlaylistInfo> playlists = new ArrayList<>();


        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject resultsJSON;

                try {
                    resultsJSON = new JSONObject(response.getResponseText());
                    JSONArray items = resultsJSON.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        PlaylistInfo playlistInfo = new PlaylistInfo();
                        playlistInfo.override(readDefaultInfo((JSONObject) items.get(i)));
                        playlists.add(playlistInfo);
                    }
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/me/playlists";

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return playlists;
    }

    public static PlaylistInfo requestPlaylistInfo(String id, String accessToken) throws SpotifyWebRequestException {
        final PlaylistInfo playlistInfo = new PlaylistInfo();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject resultJSON;

                try {
                    resultJSON = new JSONObject(response.getResponseText());
                    playlistInfo.override(readPlaylistJSON(resultJSON));
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = BASE_URL + "/users/" + UserInfo.getId() + "/playlists/" + id;

        RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.GET);
        responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Bearer " + accessToken));

        try {
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return playlistInfo;
    }

    public static TokenSet requestTokens(final String code) throws SpotifyWebRequestException {
        final TokenSet tokens = new TokenSet();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject resultJSON;

                try {
                    resultJSON = new JSONObject(response.getResponseText());
                    tokens.setAccessToken(resultJSON.getString("access_token"));
                    tokens.setRefreshToken(resultJSON.getString("refresh_token"));
                    tokens.setExpiresIn(resultJSON.getInt("expires_in"));
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = ACCOUNT_URL + "?grant_type=authorization_code&redirect_uri=" + Constants.REDIRECT_URI.replace("/", "%2F") + "&code=" + code;
//        final String url = ACCOUNT_URL + "?grant_type=authorization_code&redirect_uri=" + Constants.REDIRECT_URI.replace("/", "%2F") + "&code=" + code + "&client_id=" + Constants.CLIENT_ID + "&client_secret=" + Constants.CLIENT_SECRET_ENCODED;


        try {
            RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.POST);
            String decoded = Utils.base64Encode(Constants.CLIENT_ID, new Decoder().decode(Constants.CLIENT_SECRET_ENCODED));
            responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Basic " + decoded));
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return tokens;
    }

    public static TokenSet refreshAccessToken(final String refreshToken) throws SpotifyWebRequestException {
        final TokenSet tokens = new TokenSet();

        GETResponseHandler responseHandler = new DefaultGETResponseHandler() {
            @Override
            protected void handle200(HTTPResponse response) {
                if (response == null) {
                    setException(new SpotifyWebRequestException("response is null"));
                    return;
                }

                JSONObject resultJSON;

                try {
                    resultJSON = new JSONObject(response.getResponseText());
                    tokens.setAccessToken(resultJSON.getString("access_token"));
                    tokens.setExpiresIn(resultJSON.getInt("expires_in"));
                    tokens.setRefreshToken(refreshToken);
                } catch (JSONException e) {
                    setException(new SpotifyWebRequestException(e));
                }
            }
        };

        final String url = ACCOUNT_URL + "?grant_type=refresh_token&refresh_token=" + refreshToken;
//        final String url = ACCOUNT_URL + "?grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + Constants.CLIENT_ID + "&client_secret=" + Constants.CLIENT_SECRET_ENCODED;

        try {
            RetrieveHTTPSResponse responseRetriever = new RetrieveHTTPSResponse(url, RequestMethod.POST);
            String decoded = Utils.base64Encode(Constants.CLIENT_ID, new Decoder().decode(Constants.CLIENT_SECRET_ENCODED));
            responseRetriever.addHeader(new Header(Header.AUTHORIZATION, "Basic " + decoded));
            responseHandler.handleResponse(responseRetriever.execute().get(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LogW.d(LOG_TAG, "unable to handle response from URL: " + url);
            throw new SpotifyWebRequestException(e);
        }

        return tokens;
    }

    private static SongInfo readSongJSON(JSONObject songInfoJSON) throws JSONException {
        SongInfo songInfo = new SongInfo();
        songInfo.override(readDefaultInfo(songInfoJSON));
        JSONArray artistsJSON = songInfoJSON.getJSONArray("artists");
        List<Artist> artists = new ArrayList<>();

        for (int i = 0; i < artistsJSON.length(); i++) {
            artists.add(new Artist(readArtistJSON(artistsJSON.getJSONObject(i))));
        }

        songInfo.setArtists(artists);
        songInfo.setAlbum(new Album(readAlbumJSON(songInfoJSON.getJSONObject("album"))));
        songInfo.setLength(songInfoJSON.getLong("duration_ms"));

        return songInfo;
    }

    private static ArtistInfo readArtistJSON(JSONObject artistJSON) throws JSONException {
        ArtistInfo artistInfo = new ArtistInfo();
        artistInfo.override(readDefaultInfo(artistJSON));
        return artistInfo;
    }

    private static AlbumInfo readAlbumJSON(JSONObject albumJSON) throws JSONException {
        AlbumInfo albumInfo = new AlbumInfo();
        albumInfo.override(readDefaultInfo(albumJSON));
        return albumInfo;
    }

    private static PlaylistInfo readPlaylistJSON(JSONObject playlistJSON) throws JSONException {
        PlaylistInfo playlistInfo = new PlaylistInfo();
        playlistInfo.override(readDefaultInfo(playlistJSON));
        JSONObject tracks = playlistJSON.getJSONObject("tracks");
        JSONArray items = tracks.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = (JSONObject) items.get(i);
            SongInfo songInfo = readSongJSON(item.getJSONObject("track"));
            SpotifySong song = new SpotifySong(songInfo);
            song.setLocked(true);
            playlistInfo.addSong(song);
        }
        return playlistInfo;
    }

    private static Info readDefaultInfo(JSONObject infoJSON) throws JSONException {
        Info info = new Info();
        info.setName(infoJSON.getString("name"));
        info.setId(infoJSON.getString("id"));
        info.setType(infoJSON.getString("type"));
        info.setUri(infoJSON.getString("uri"));

        return info;
    }

    private static enum RequestMethod {
        GET, POST
    }

    private static class RetrieveHTTPSResponse extends AsyncTask<String, Void, HTTPResponse> {

        private String urlText;
        private RequestMethod method;
        private String accessToken;
        private List<Header> headers;

        public RetrieveHTTPSResponse(String urlText, RequestMethod method) {
            this.urlText = urlText;
            this.method = method;
            this.accessToken = null;
            this.headers = new ArrayList<>();
        }

        @Override
        protected HTTPResponse doInBackground(String... params) {
            HTTPResponse response = new HTTPResponse();

            try {
                URL url = new URL(urlText);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                if (method != null) {
                    con.setRequestMethod(method.name());
                }

                if (accessToken != null) {
                    con.setRequestProperty(Header.AUTHORIZATION, "Bearer " + accessToken);
                }

                for (Header header : headers) {
                    con.setRequestProperty(header.getParam(), header.getValue());
                }

                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

                LogW.d(LOG_TAG, "sending " + con.getRequestMethod() + " request to URL: " + urlText);

                int responseCode = con.getResponseCode();
                response.setHTTPStatusCode(responseCode);

                LogW.d(LOG_TAG, "response Code: " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder responseBuffer = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    responseBuffer.append(inputLine);
                }
                in.close();

                String responseText = responseBuffer.toString();

                response.setResponseText(responseText);

                LogW.d(LOG_TAG, "HTTPS GET request response: " + responseText);
            } catch (IOException e) {
                response.setException(e);
            }

            return response;
        }

        public List<Header> getHeaders() {
            return headers;
        }

        public void setHeaders(List<Header> headers) {
            this.headers = headers;
        }

        public void addHeader(Header header) {
            headers.add(header);
        }
    }

    private static class Header {
        public static final String AUTHORIZATION = "Authorization";

        private String param;
        private String value;

        public Header(String param, String value) {
            this.param = param;
            this.value = value;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
