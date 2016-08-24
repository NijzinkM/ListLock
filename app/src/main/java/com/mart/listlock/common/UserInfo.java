package com.mart.listlock.common;

import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

public final class UserInfo {

    private static String displayName;
    private static String country;
    private static long id;
    private static String image;
    private static boolean premium;
    private static String accessToken;
    private static String refreshToken;

    public static void init(String accessToken) throws SpotifyWebRequestException {
        SpotifyWebRequest.requestUserInfo(accessToken);
    }

    public static String getDisplayName() {
        return displayName;
    }

    public static void setDisplayName(String username) {
        UserInfo.displayName = username;
    }

    public static String getCountry() {
        return country;
    }

    public static void setCountry(String country) {
        UserInfo.country = country;
    }

    public static long getId() {
        return id;
    }

    public static void setId(long id) {
        UserInfo.id = id;
    }

    public static String getImage() {
        return image;
    }

    public static void setImage(String image) {
        UserInfo.image = image;
    }

    public static boolean isPremium() {
        return premium;
    }

    public static void setPremium(boolean premium) {
        UserInfo.premium = premium;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static void setAccessToken(String accessToken) {
        UserInfo.accessToken = accessToken;
    }

    public static String getRefreshToken() {
        return refreshToken;
    }

    public static void setRefreshToken(String refreshToken) {
        UserInfo.refreshToken = refreshToken;
    }
}
