package org.atari.dhs.buzztest.androidclient.tools;

import android.R;
import android.app.Activity;
import android.os.Bundle;

public class ConfigurableStyleActivity extends Activity
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setTheme( R.style.Theme_Light );
    }
}
