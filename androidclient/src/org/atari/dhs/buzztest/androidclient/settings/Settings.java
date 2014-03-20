package org.atari.dhs.buzztest.androidclient.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import org.atari.dhs.buzztest.androidclient.DhsBuzzApp;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.settings.auth.Authenticate;

public class Settings extends PreferenceActivity
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        PreferencesManager prefsManager = new PreferencesManager( this );
        if( prefsManager.getBuzzToken() == null ) {
            Log.i( "no buzz token, redirecting to Authenticate" );
            startActivity( new Intent( this, Authenticate.class ) );
            return;
        }

        addPreferencesFromResource( R.xml.preferences );

        DhsBuzzApp application = (DhsBuzzApp)getApplicationContext();
        if( application.isC2dmDisabled() ) {
            CheckBoxPreference autoRealtimePref = (CheckBoxPreference)findPreference( PreferencesManager.SHARED_AUTO_REALTIME_KEY );
            autoRealtimePref.setEnabled( false );
        }

        initAccountValues();
    }

    private void initAccountValues() {
        ListPreference pref = (ListPreference)findPreference( PreferencesManager.SHARED_ACCOUNT_NAME_KEY );

        Resources resources = getResources();

        AccountManager accountManager = AccountManager.get( this );
        Account[] accounts = accountManager.getAccountsByType( "com.google" );

        String[] entries = new String[accounts.length + 1];
        entries[0] = resources.getString( R.string.settings_no_account );
        for( int i = 0 ; i < accounts.length ; i++ ) {
            entries[i + 1] = accounts[i].name;
        }
        pref.setEntries( entries );

        String[] values = new String[accounts.length + 1];
        values[0] = "";
        for( int i = 0 ; i < accounts.length ; i++ ) {
            values[i + 1] = accounts[i].name;
        }
        pref.setEntryValues( values );
    }
}
