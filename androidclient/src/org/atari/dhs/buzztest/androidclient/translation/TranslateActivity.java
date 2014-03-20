package org.atari.dhs.buzztest.androidclient.translation;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.*;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class TranslateActivity extends AsyncSupportActivity
{
    public static final String EXTRA_TEXT = "languageText";

    private TextView textView;

    private Map<String, LanguageWrapper> languageMap;

    private String text;
    private TranslateTask runningTask;
    private String waitingTranslationLanguage;
    private String defaultLanguage;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView( R.layout.translation_popup );

        textView = (TextView)findViewById( R.id.content );

        Intent intent = getIntent();
        text = intent.getStringExtra( EXTRA_TEXT );
        if( text == null ) {
            throw new IllegalStateException( "text missing" );
        }

        Resources resources = getResources();

        defaultLanguage = resources.getString( R.string.translate_language );

        buildLanguageMap( resources );
        initLanguageSelectionSpinner( resources );

        startTranslationTask( null, defaultLanguage, false );
    }

    private void startTranslationTask( String sourceLanguage, String defaultLanguage, boolean inhibitEnableProgressBar ) {
        if( !inhibitEnableProgressBar ) {
            setProgressBarIndeterminateVisibility( true );
        }

        runningTask = new TranslateTask();
        startAsyncTask( runningTask, new TranslateTask.Args( text, sourceLanguage, defaultLanguage ) );
    }

    private void initLanguageSelectionSpinner( Resources resources ) {
        Spinner spinner = (Spinner)findViewById( R.id.language_selection );

        final ArrayAdapter<LanguageWrapper> adapter = new ArrayAdapter<LanguageWrapper>( this, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

        List<LanguageWrapper> spinnerValues = new ArrayList<LanguageWrapper>( languageMap.values() );
        final Collator collator = Collator.getInstance();
        Comparator<LanguageWrapper> comparator = new Comparator<LanguageWrapper>()
        {
            @Override
            public int compare( LanguageWrapper o1, LanguageWrapper o2 ) {
                return collator.compare( o1.name, o2.name );
            }
        };
        Collections.sort( spinnerValues, comparator );
        adapter.add( new LanguageWrapper( resources.getString( R.string.translate_autodetect_language ), null, true ) );
        for( LanguageWrapper wrapper : spinnerValues ) {
            if( wrapper.includeInMenu ) {
                adapter.add( wrapper );
            }
        }

        spinner.setAdapter( adapter );

        spinner.setSelection( 0 );

        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected( AdapterView<?> adapterView, View view, int position, long l ) {
                LanguageWrapper wrapper = adapter.getItem( position );
                if( runningTask != null ) {
                    waitingTranslationLanguage = wrapper.languageCode;
                }
                else {
                    startTranslationTask( wrapper.languageCode, defaultLanguage, false );
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> adapterView ) {
            }
        } );
    }

    private void buildLanguageMap( Resources resources ) {
        languageMap = new HashMap<String, LanguageWrapper>();
        addLanguage( "af", R.string.language_af, resources, true );
        addLanguage( "sq", R.string.language_sq, resources, true );
        addLanguage( "ar", R.string.language_ar, resources, true );
        addLanguage( "hy", R.string.language_hy, resources, true );
        addLanguage( "az", R.string.language_az, resources, true );
        addLanguage( "eu", R.string.language_eu, resources, true );
        addLanguage( "be", R.string.language_be, resources, true );
        addLanguage( "bg", R.string.language_bg, resources, true );
        addLanguage( "ca", R.string.language_ca, resources, true );
        addLanguage( "zh-CN", R.string.language_zh_cn, resources, true );
        addLanguage( "zh-TW", R.string.language_zh_tw, resources, true );
        addLanguage( "zh", R.string.language_zh, resources, false );
        addLanguage( "hr", R.string.language_hr, resources, true );
        addLanguage( "cs", R.string.language_cs, resources, true );
        addLanguage( "da", R.string.language_da, resources, true );
        addLanguage( "nl", R.string.language_nl, resources, true );
        addLanguage( "en", R.string.language_en, resources, true );
        addLanguage( "et", R.string.language_et, resources, true );
        addLanguage( "tl", R.string.language_tl, resources, true );
        addLanguage( "fi", R.string.language_fi, resources, true );
        addLanguage( "fr", R.string.language_fr, resources, true );
        addLanguage( "gl", R.string.language_gl, resources, true );
        addLanguage( "ka", R.string.language_ka, resources, true );
        addLanguage( "de", R.string.language_de, resources, true );
        addLanguage( "el", R.string.language_el, resources, true );
        addLanguage( "ht", R.string.language_ht, resources, true );
        addLanguage( "iw", R.string.language_iw, resources, true );
        addLanguage( "hi", R.string.language_hi, resources, true );
        addLanguage( "hu", R.string.language_hu, resources, true );
        addLanguage( "is", R.string.language_is, resources, true );
        addLanguage( "id", R.string.language_id, resources, true );
        addLanguage( "ga", R.string.language_ga, resources, true );
        addLanguage( "it", R.string.language_it, resources, true );
        addLanguage( "ja", R.string.language_ja, resources, true );
        addLanguage( "ko", R.string.language_ko, resources, true );
        addLanguage( "lv", R.string.language_lv, resources, true );
        addLanguage( "lt", R.string.language_lt, resources, true );
        addLanguage( "mk", R.string.language_mk, resources, true );
        addLanguage( "ms", R.string.language_ms, resources, true );
        addLanguage( "mt", R.string.language_mt, resources, true );
        addLanguage( "no", R.string.language_no, resources, true );
        addLanguage( "fa", R.string.language_fa, resources, true );
        addLanguage( "pl", R.string.language_pl, resources, true );
        addLanguage( "pt", R.string.language_pt, resources, true );
        addLanguage( "ro", R.string.language_ro, resources, true );
        addLanguage( "ru", R.string.language_ru, resources, true );
        addLanguage( "sr", R.string.language_sr, resources, true );
        addLanguage( "sk", R.string.language_sk, resources, true );
        addLanguage( "sl", R.string.language_sl, resources, true );
        addLanguage( "es", R.string.language_es, resources, true );
        addLanguage( "sw", R.string.language_sw, resources, true );
        addLanguage( "sv", R.string.language_sv, resources, true );
        addLanguage( "th", R.string.language_th, resources, true );
        addLanguage( "tr", R.string.language_tr, resources, true );
        addLanguage( "uk", R.string.language_uk, resources, true );
        addLanguage( "ur", R.string.language_ur, resources, true );
        addLanguage( "vi", R.string.language_vi, resources, true );
        addLanguage( "cy", R.string.language_cy, resources, true );
        addLanguage( "yi", R.string.language_yi, resources, true );
    }

    private void addLanguage( String languageCode, int resourceId, Resources resources, boolean includeInMenu ) {
        String languageName = resources.getString( resourceId );
        languageMap.put( languageCode, new LanguageWrapper( languageName, languageCode, includeInMenu ) );
    }

    void processResult( TranslateTask.Result result ) {
        Resources resources = getResources();
        if( result.errorMessage != null ) {
            String fmt = resources.getString( R.string.translate_error_loading_translation );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
        }
        else {
            if( result.detectedLanguage != null ) {
                LanguageWrapper languageWrapper = languageMap.get( result.detectedLanguage );
                String languageName;
                if( languageWrapper == null ) {
                    languageName = result.detectedLanguage;
                }
                else {
                    languageName = languageWrapper.name;
                }
                String fmt = resources.getString( R.string.translate_detected_as );
                setTitle( MessageFormat.format( fmt, languageName ) );
            }
            textView.setText( result.translatedMessage );
        }

        runningTask = null;
        if( waitingTranslationLanguage != null ) {
            String s = waitingTranslationLanguage;
            waitingTranslationLanguage = null;
            startTranslationTask( waitingTranslationLanguage, defaultLanguage, true );
        }
        else {
            setProgressBarIndeterminateVisibility( false );
        }
    }

    private static class LanguageWrapper
    {
        String name;
        String languageCode;
        private boolean includeInMenu;

        private LanguageWrapper( String name, String languageCode, boolean includeInMenu ) {
            this.name = name;
            this.languageCode = languageCode;
            this.includeInMenu = includeInMenu;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
