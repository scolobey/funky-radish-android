package com.funkyradish.funky_radish;


public final class Constants {
    private static final String INSTANCE_ADDRESS = "recipe-realm.us1.cloud.realm.io";
    public static final String AUTH_URL = "https://" + INSTANCE_ADDRESS + "/auth";
    public static final String REALM_URL = "realms://" + INSTANCE_ADDRESS + "/~/recipes";
    public static final String REALM_DB_NAME = "fr_realm_db";

    public static final String ENDPOINT = "https://funky-radish-api.herokuapp.com/users";
    public static final String ENDPOINT2 = "https://funky-radish-api.herokuapp.com/authenticate";
    // Switch for local dev
    //public static final String ENDPOINT = "http://10.0.2.2:8080/users"
    //public static final String ENDPOINT2 = "http://10.0.2.2:8080/authenticate"

    public static final String FR_TOKEN = "fr_token";
    public static final String FR_USERNAME = "fr_username";
    public static final String FR_USER_EMAIL = "fr_user_email";
    public static final String OFFLINE = "fr_offline";
}