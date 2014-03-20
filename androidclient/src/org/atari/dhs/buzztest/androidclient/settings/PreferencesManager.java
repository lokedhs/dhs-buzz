package org.atari.dhs.buzztest.androidclient.settings;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.acra.ErrorReporter;

public class PreferencesManager
{
    public static final String PREFS_NAME = "buzz_config";
    public static final String BUZZ_TOKEN_KEY = "buzzAuthToken";
    public static final String BUZZ_SECRET_KEY = "buzzAuthKey";
    public static final String BUZZ_USER_ID_KEY = "userId";
    public static final String BUZZ_C2DM_SERVER_KEY = "c2dmServerKey";
    public static final String SHARED_ACCOUNT_NAME_KEY = "account";
    public static final String C2DM_DISABLED_KEY = "c2DMDisabled";
    public static final String LAST_VERSION_STARTED_KEY = "mostRecentVersion";
    public static final String CONFIRMED_GOOGLE_LOCATION_COLLECTION_KEY = "confirmedGoogleCommunication";

    public static final String SHARED_AUTO_REALTIME_KEY = "realtimePost";
    public static final String SHARED_AUTO_DOWNLOAD_REPLIES_KEY = "downloadReplies";
    public static final String SHARED_DEFAULT_START_ACTIVITY_KEY = "startScreen";

    public static final String SHARED_NOTIFICATION_SOUND_KEY = "soundMode";
    public static final String SHARED_NOTIFICATION_VIBRATE_KEY = "vibrateActive";
    public static final String SHARED_NOTIFICATION_BALL_COLOUR_KEY = "flashColour";

    private static Map<String,Integer> BALL_COLOUR_VALUES;

    private SharedPreferences privatePrefs;
    private SharedPreferences defaultPrefs;

    static {
        Map<String,Integer> values = new HashMap<String, Integer>();
        values.put( "off", 0x00000000 );
        values.put( "white", 0xffffffff );
        values.put( "red", 0xfff0000 );
        values.put( "green", 0xff00ff00 );
        values.put( "blue", 0xff0000ff );
        BALL_COLOUR_VALUES = values;
    }

    public PreferencesManager( Context context ) {
        privatePrefs = context.getSharedPreferences( PREFS_NAME, Activity.MODE_PRIVATE );
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences( context );
    }

    public String getBuzzToken() {
        return privatePrefs.getString( BUZZ_TOKEN_KEY, null );
    }

    public String getBuzzSecret() {
        return privatePrefs.getString( BUZZ_SECRET_KEY, null );
    }

    public String getUserId() {
        return privatePrefs.getString( BUZZ_USER_ID_KEY, null );
    }

    public void setBuzzTokenAndSecret( String token, String secret, String userId ) {
        SharedPreferences.Editor editor = privatePrefs.edit();
        editor.putString( BUZZ_TOKEN_KEY, token );
        editor.putString( BUZZ_SECRET_KEY, secret );
        editor.putString( BUZZ_USER_ID_KEY, userId );
        editor.commit();

        updateErrorReporterUsername();
    }

    public String getC2DMKey() {
        return privatePrefs.getString( BUZZ_C2DM_SERVER_KEY, null );
    }

    public int getLastVersionStarted() {
        return privatePrefs.getInt( LAST_VERSION_STARTED_KEY, 0 );
    }

    public void setLastVersionStarted( int version ) {
        SharedPreferences.Editor editor = privatePrefs.edit();
        editor.putInt( LAST_VERSION_STARTED_KEY, version );
        editor.commit();
    }

    public void setC2DMKey( String key ) {
        SharedPreferences.Editor editor = privatePrefs.edit();
        editor.putString( BUZZ_C2DM_SERVER_KEY, key );
        editor.commit();
    }

    public String getAccountName() {
        String v = defaultPrefs.getString( SHARED_ACCOUNT_NAME_KEY, "" );
        if( v.equals( "" ) ) {
            return null;
        }
        else {
            return v;
        }
    }

    public boolean isC2DMDisabled() {
        return privatePrefs.getBoolean( C2DM_DISABLED_KEY, false );
    }

    public void setC2DMDisabled( boolean disabled ) {
        SharedPreferences.Editor editor = privatePrefs.edit();
        editor.putBoolean( C2DM_DISABLED_KEY, disabled );
        editor.commit();
    }

    public boolean hasC2DM() {
        return getC2DMKey() != null && !isC2DMDisabled();
    }

    public boolean isConfirmedGoogleDataCollection() {
        return privatePrefs.getBoolean( CONFIRMED_GOOGLE_LOCATION_COLLECTION_KEY, false );
    }

    public void setConfirmedGoogleDataCollection( boolean confirmed ) {
        SharedPreferences.Editor editor = privatePrefs.edit();
        editor.putBoolean( CONFIRMED_GOOGLE_LOCATION_COLLECTION_KEY, confirmed );
        editor.commit();
    }

    public int getBallColour() {
        String value = defaultPrefs.getString( SHARED_NOTIFICATION_BALL_COLOUR_KEY, "off" );
        Integer ret = BALL_COLOUR_VALUES.get( value );
        if( ret == null ) {
            throw new IllegalStateException( "illegal notification colour setting: " + value );
        }
        return ret;
    }

    public boolean getVibrate() {
        return defaultPrefs.getBoolean( SHARED_NOTIFICATION_VIBRATE_KEY, false );
    }

    public boolean getPlaySound() {
        return defaultPrefs.getBoolean( SHARED_NOTIFICATION_SOUND_KEY, false );
    }

    public AutoRealtimeOption getAutoRealtime() {
        boolean v = defaultPrefs.getBoolean( SHARED_AUTO_REALTIME_KEY, false );
        if( v ) {
            return AutoRealtimeOption.ENABLE;
        }
        else {
            return AutoRealtimeOption.DISABLE;
        }
    }

    public AutoDownloadRepliesOption getAutoDownloadReplies() {
        String v = defaultPrefs.getString( SHARED_AUTO_DOWNLOAD_REPLIES_KEY, "always" );
        if( v.equals( "always" ) ) {
            return AutoDownloadRepliesOption.ALWAYS;
        }
        else if( v.equals( "never" ) ) {
            return AutoDownloadRepliesOption.NEVER;
        }
        else if( v.equals( "only_wlan" ) ) {
            return AutoDownloadRepliesOption.ONLY_WHEN_ON_WLAN;
        }
        else {
            throw new IllegalStateException( "illegal config value for auto download: " + v );
        }
    }

    public StartActivityType getDefaultStartActivity() {
        String v = defaultPrefs.getString( SHARED_DEFAULT_START_ACTIVITY_KEY, "main_screen" );
        if( v.equals( "main_screen" ) ) {
            return StartActivityType.WELCOME_SCREEN;
        }
        else if( v.equals( "personal" ) ) {
            return StartActivityType.PERSONAL_FEED;
        }
        else if( v.equals( "nearby" ) ) {
            return StartActivityType.NEARBY;
        }
        else {
            throw new IllegalStateException( "illegal config value for default screen: " + v );
        }
    }

    public void updateErrorReporterUsername() {
        String userId = getUserId();
        ErrorReporter.getInstance().addCustomData( "userid", userId == null ? "not set" : userId );
    }

    public void updateErrorReporterAccountName() {
        String accountName = getAccountName();
        ErrorReporter.getInstance().addCustomData( "accountName", accountName == null ? "not set" : accountName );
    }
}
