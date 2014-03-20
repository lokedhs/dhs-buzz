package org.atari.dhs.buzztest.androidclient.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.groups.GroupItemInfo;
import org.atari.dhs.buzztest.androidclient.groups.GroupSelector;
import org.atari.dhs.buzztest.androidclient.groups.SelectedGroups;
import org.atari.dhs.buzztest.androidclient.post.selectimages.PostSelectImagesActivity;
import org.atari.dhs.buzztest.androidclient.post.selectimages.SelectedImageResult;
import org.atari.dhs.buzztest.androidclient.post.selectlocation.SelectLocationActivity;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.QueryDialogBox;
import org.atari.dhs.buzztest.androidclient.tools.RotateableContainer;

public class PostActivity extends Activity
{
    private static final long LOCATION_UPDATE_MIN_TIME = 5 * 60 * 1000;
    private static final float LOCATION_UPDATE_MIN_DISTANCE = 10;

    private static final int REQUEST_CODE_SELECT_IMAGE_FROM_VIEWER = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE_FROM_URL = 2;
    private static final int REQUEST_CODE_SELECT_LOCATION = 3;
    private static final int REQUEST_CODE_SELECT_GROUPS = 4;

    private BuzzManager buzzManager;
    private LocationManager locationManager;
    private PreferencesManager preferencesManager;

    private ViewGroup imageAttachmentView;
    private List<AttachedImage> attachedImages = new ArrayList<AttachedImage>();

    private EditText contentEditText;
    private TextView urlTextView;
    private CheckBox includeLocationCheckbox;
    private RotateableContainer includeLocationRotateablePanel;
    private View locationControlsView;
    private TextView locationNameText;

    private RotateableContainer privateRotateablePanel;
    private CheckBox privateCheckbox;
    private View privateOptionsPanel;
    private TextView privateGroupsTextField;

    private Button sendButton;

    private String attachedUrlTitle;
    private String attachedUrl;

    private Location location;
    private PositionUpdateListener positionUpdateListener;

    private String selectedPlaceName;
    private String selectedPlaceId;
    private String selectedGeocode;
    private SelectedGroups selectedGroups;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        locationManager = (LocationManager)getSystemService( LOCATION_SERVICE );
        buzzManager = BuzzManager.createFromContext( this );
        preferencesManager = new PreferencesManager( this );

        setContentView( R.layout.post );

        PreferencesManager preferencesManager = new PreferencesManager( this );
        boolean hasImageAccount = preferencesManager.getAccountName() != null;

        imageAttachmentView = (ViewGroup)findViewById( R.id.image_attachment_view );
        contentEditText = (EditText)findViewById( R.id.content );
        urlTextView = (TextView)findViewById( R.id.url_text );
        includeLocationCheckbox = (CheckBox)findViewById( R.id.include_location_checkbox );
        includeLocationCheckbox.setText( "Include\nlocation" );
        includeLocationRotateablePanel = (RotateableContainer)findViewById( R.id.include_location_rotateable_panel );
        locationControlsView = findViewById( R.id.location_options_panel );
        locationNameText = (TextView)findViewById( R.id.location_name_text );

        privateRotateablePanel = (RotateableContainer)findViewById( R.id.private_post_rotateable_panel );
        privateCheckbox = (CheckBox)findViewById( R.id.private_post_checkbox );
        privateOptionsPanel = findViewById( R.id.private_options_panel );
        privateGroupsTextField = (TextView)findViewById( R.id.private_post_groups_text_field );

        sendButton = (Button)findViewById( R.id.send_button );

        Button attachImageButton = (Button)findViewById( R.id.attach_image_button );
        attachImageButton.setEnabled( hasImageAccount );

        InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.hideSoftInputFromWindow( contentEditText.getApplicationWindowToken(), 0 );

        contentEditText.addTextChangedListener( new TextWatcher()
        {
            @Override
            public void beforeTextChanged( CharSequence charSequence, int i, int i1, int i2 ) {
            }

            @Override
            public void onTextChanged( CharSequence charSequence, int i, int i1, int i2 ) {
            }

            @Override
            public void afterTextChanged( Editable editable ) {
                String s = contentEditText.getText().toString();
                s = s.trim();
                sendButton.setEnabled( !s.equals( "" ) );
            }
        } );

        sendButton.setEnabled( false );

        includeLocationCheckbox.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged( CompoundButton compoundButton, boolean checked ) {
                if( !checked ) {
                    disableLocation();
                }
                includeLocationRotateablePanel.setRotation( checked );
            }
        } );

        includeLocationRotateablePanel.setOnRotationCompleteListener( new RotateableContainer.RotationCompleteListener()
        {
            @Override
            public void onRotationComplete( boolean rotated ) {
                if( rotated ) {
                    enableLocation();
                }
            }
        } );

        updateLocationButtonText();

        privateCheckbox.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged( CompoundButton compoundButton, boolean checked ) {
                disablePrivate();
                privateRotateablePanel.setRotation( checked );
            }
        } );
        privateRotateablePanel.setOnRotationCompleteListener( new RotateableContainer.RotationCompleteListener()
        {
            @Override
            public void onRotationComplete( boolean rotated ) {
                if( rotated ) {
                    enablePrivate();
                }
            }
        } );

        Intent intent = getIntent();
        if( Intent.ACTION_SEND.equals( intent.getAction() ) ) {
            String typeName = intent.getType();
            if( typeName.equals( "text/plain" ) ) {
                handleSentUrl( intent );
            }
            else if( typeName.startsWith( "image/" ) ) {
                if( hasImageAccount ) {
                    handleSentImage( intent );
                }
            }
        }
        else if( Intent.ACTION_SEND_MULTIPLE.equals( intent.getAction() ) ) {
            if( hasImageAccount ) {
                List<Uri> urlList = intent.getParcelableArrayListExtra( Intent.EXTRA_STREAM );
                for( Uri uri : urlList ) {
                    attachImageByUrl( uri );
                }
            }
        }
    }

    private void enableLocation() {
        locationControlsView.setVisibility( View.VISIBLE );
    }

    private void disableLocation() {
        locationControlsView.setVisibility( View.GONE );
    }

    @Override
    protected void onDestroy() {
        buzzManager.close();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        location = null;

        Log.i( "show all providers" );
        for( String p : locationManager.getAllProviders() ) {
            Log.i( "p=" + p );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String providerName = findDecentLocationProvider( locationManager );
        if( providerName != null ) {
            includeLocationCheckbox.setEnabled( true );

//            LocationProvider provider = locationManager.getProvider( providerName );
            updateCurrentLocation( locationManager.getLastKnownLocation( providerName ) );
            positionUpdateListener = new PositionUpdateListener();
            locationManager.requestLocationUpdates( providerName, LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE,
                                                    positionUpdateListener );
        }
        else {
            includeLocationCheckbox.setChecked( false );
            includeLocationCheckbox.setEnabled( false );
        }
    }

    @Override
    protected void onPause() {
        if( positionUpdateListener != null ) {
            locationManager.removeUpdates( positionUpdateListener );
            positionUpdateListener = null;
        }

        super.onPause();
    }

    public static String findDecentLocationProvider( LocationManager locationManager ) {
//        String p = testProvider( locationManager, LocationManager.GPS_PROVIDER );
//        if( p != null ) {
//            return p;
//        }
//
//        p = testProvider( locationManager, LocationManager.NETWORK_PROVIDER );
//        if( p != null ) {
//            return p;
//        }
//
//        p = testProvider( locationManager, LocationManager.PASSIVE_PROVIDER );
//        return p;
        Criteria criteria = new Criteria();
        criteria.setAccuracy( Criteria.ACCURACY_COARSE );
        criteria.setBearingRequired( false );
        criteria.setSpeedRequired( false );
        return locationManager.getBestProvider( criteria, true );
    }

    private static String testProvider( LocationManager locationManager, String name ) {
        LocationProvider prov = locationManager.getProvider( name );
        if( prov != null && locationManager.isProviderEnabled( name ) ) {
            return name;
        }
        else {
            return null;
        }
    }

    private void handleSentUrl( Intent intent ) {
        final String title = intent.getStringExtra( Intent.EXTRA_SUBJECT );
        final String url = intent.getStringExtra( Intent.EXTRA_TEXT );
        setAttachedUrl( title, url );

        View urlLinkView = findViewById( R.id.url_link_view );
        urlLinkView.setVisibility( Button.VISIBLE );

        Button selectImagesButton = (Button)findViewById( R.id.image_select_button );
        selectImagesButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                Intent selectImageIntent = new Intent( PostActivity.this, PostSelectImagesActivity.class );
                selectImageIntent.putExtra( PostSelectImagesActivity.EXTRA_URL, url );
                startActivityForResult( selectImageIntent, REQUEST_CODE_SELECT_IMAGE_FROM_URL );
            }
        } );
    }

    private void handleSentImage( Intent intent ) {
        Uri uri = intent.getParcelableExtra( Intent.EXTRA_STREAM );
        attachImageByUrl( uri );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void postMessageClicked( View view ) {
        List<String> attachedImagesList = new ArrayList<String>();
        List<String> attachedImageUrlsList = new ArrayList<String>();
        for( AttachedImage img : attachedImages ) {
            String remoteUrl = img.getRemoteUrl();
            if( remoteUrl == null ) {
                attachedImagesList.add( img.getFile() );
            }
            else {
                attachedImageUrlsList.add( remoteUrl );
            }
        }

        final String[] attachedImagesArray = attachedImagesList.toArray( new String[attachedImagesList.size()] );
        final String[] attachedImageUrlsArray = attachedImageUrlsList.toArray( new String[attachedImageUrlsList.size()] );
        final CharSequence text = contentEditText.getText().toString();

        String geocode = null;
        String address = null;
        if( includeLocationCheckbox.isChecked() && location != null ) {
            if( selectedGeocode != null ) {
                geocode = selectedGeocode;
            }
            else {
                geocode = location.getLatitude() + " " + location.getLongitude();
            }
            address = selectedPlaceId;
        }

        String[] groups = null;
        if( privateCheckbox.isChecked() && selectedGroups != null ) {
            GroupItemInfo[] items = selectedGroups.getItems();
            if( items.length == 0 ) {
                Toast.makeText( this, "No groups selected", Toast.LENGTH_LONG ).show();
                return;
            }
            groups = new String[items.length];
            int i = 0;
            for( GroupItemInfo item : items ) {
                groups[i++] = item.getId();
            }
        }

        final String geocode0 = geocode;
        final String address0 = address;
        final String[] groups0 = groups;

        startService( new Intent( this, PostMessageService.class ) );
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.postMessage( text.toString(),
                                     attachedUrlTitle, attachedUrl,
                                     attachedImagesArray, attachedImageUrlsArray,
                                     geocode0, address0, groups0 );
                finish();
            }
        } );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void attachImageClicked( View view ) {
//        Intent intent = new Intent( Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI );
        Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
        intent.setType( "image/*" );
        try {
            startActivityForResult( intent, REQUEST_CODE_SELECT_IMAGE_FROM_VIEWER );
        }
        catch( ActivityNotFoundException e ) {
            Log.w( "could not find image viewer", e );
            Toast toast = Toast.makeText( this, R.string.post_no_image_viewer_available, Toast.LENGTH_SHORT );
            toast.show();
        }
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent returnedIntent ) {
        super.onActivityResult( requestCode, resultCode, returnedIntent );

        switch( requestCode ) {
            case REQUEST_CODE_SELECT_IMAGE_FROM_VIEWER:
                if( resultCode == RESULT_OK ) {
                    Uri selectedImage = returnedIntent.getData();
                    attachImageByUrl( selectedImage );
                }
                break;

            case REQUEST_CODE_SELECT_IMAGE_FROM_URL:
                if( resultCode == RESULT_OK ) {
                    Parcelable[] parcelableList = returnedIntent.getParcelableArrayExtra( PostSelectImagesActivity.EXTRA_RESULT_LIST );
                    for( Parcelable t : parcelableList ) {
                        SelectedImageResult image = (SelectedImageResult)t;
                        addFileToAttachedImages( image.localFile, image.url );
                    }
                }
                break;

            case REQUEST_CODE_SELECT_LOCATION:
                if( resultCode == RESULT_OK ) {
                    this.selectedPlaceName = returnedIntent.getStringExtra( SelectLocationActivity.EXTRA_SELECTED_PLACE_NAME );
                    this.selectedPlaceId = returnedIntent.getStringExtra( SelectLocationActivity.EXTRA_SELECTED_PLACE_ID );
                    this.selectedGeocode = returnedIntent.getStringExtra( SelectLocationActivity.EXTRA_SELECTED_LOCATION_GEOCODE );
                    updateLocationButtonText();
                }
                break;

            case REQUEST_CODE_SELECT_GROUPS:
                if( resultCode == RESULT_OK ) {
                    SelectedGroups selectedGroups = returnedIntent.getParcelableExtra( GroupSelector.EXTRA_RESULT_SELECTED_GROUPS );
                    setPrivateGroups( selectedGroups );
                }
                break;
        }
    }

    private void updateLocationButtonText() {
        Resources resources = getResources();
        String description;
        if( selectedPlaceName != null ) {
            if( selectedPlaceId != null ) {
                description = selectedPlaceName;
            }
            else {
                description = "Location reported as near " + selectedPlaceName;
            }
        }
        else if( location == null ) {
            description = resources.getString( R.string.post_button_label_location_not_available );
        }
        else {
            description = resources.getString( R.string.post_no_address );
        }

        locationNameText.setText( description );
    }

    private void attachImageByUrl( Uri selectedImage ) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query( selectedImage, filePathColumn, null, null, null );
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex( filePathColumn[0] );
        String filePath = cursor.getString( columnIndex );
        cursor.close();

        addFileToAttachedImages( filePath, null );
    }

    private void addFileToAttachedImages( String filePath, String remoteUrl ) {
        AttachedImage attachedImage = new AttachedImage( this, filePath, remoteUrl );
        attachedImages.add( attachedImage );
        imageAttachmentView.addView( attachedImage.getView() );
    }

    private void setAttachedUrl( String title, String url ) {
        this.attachedUrlTitle = title;
        this.attachedUrl = url;

        urlTextView.setText( title );
    }

//    @SuppressWarnings( { "UnusedDeclaration" })
//    public void openMapViewClicked( View view ) {
//        if( location != null ) {
//            Intent intent = new Intent( Intent.ACTION_VIEW,
//                                        Uri.parse( "geo:"
//                                                   + location.getLatitude()
//                                                   + ","
//                                                   + location.getLongitude()
//                                                   + "?z=14" ) );
//            startActivity( intent );
//        }
//    }

    private void updateCurrentLocation( Location location ) {
        this.location = location;
        updateLocationButtonText();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void selectPlace( View view ) {
        if( location != null ) {
            if( preferencesManager.isConfirmedGoogleDataCollection() ) {
                startSelectLocationActivity();
            }
            else {
                QueryDialogBox.DialogClickListener confirmCallback = new QueryDialogBox.DialogClickListener()
                {
                    @Override
                    public void buttonPressed() {
                        preferencesManager.setConfirmedGoogleDataCollection( true );
                        startSelectLocationActivity();
                    }
                };
                Resources resources = getResources();
                QueryDialogBox dialog = QueryDialogBox.create( this,
                                                               resources.getString( R.string.post_confirm_send_to_google_title ),
                                                               resources.getString( R.string.post_confirm_send_to_google_content ),
                                                               resources.getString( R.string.post_confirm_send_to_google_allow ), confirmCallback,
                                                               resources.getString( R.string.post_confirm_send_to_google_cancel ), null
                );
                dialog.show();
            }
        }
    }

    private void startSelectLocationActivity() {
        Intent intent = new Intent( this, SelectLocationActivity.class );
        intent.putExtra( SelectLocationActivity.EXTRA_LOCATION, location );
        startActivityForResult( intent, REQUEST_CODE_SELECT_LOCATION );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void selectGroupsClicked( View view ) {
        startActivityForResult( new Intent( this, GroupSelector.class ), REQUEST_CODE_SELECT_GROUPS );
    }

    private void enablePrivate() {
        privateOptionsPanel.setVisibility( View.VISIBLE );
    }

    private void disablePrivate() {
        privateOptionsPanel.setVisibility( View.GONE );
    }

    private void setPrivateGroups( SelectedGroups selectedGroups ) {
        this.selectedGroups = selectedGroups;
        String label = Joiner.on( ", " ).join( Lists.transform( Arrays.asList( selectedGroups.getItems() ), new Function<GroupItemInfo, String>()
        {
            @Override
            public String apply( GroupItemInfo input ) {
                return input.getGroupName();
            }
        } ) );
        privateGroupsTextField.setText( label );
    }

    private class PositionUpdateListener implements LocationListener
    {
        @Override
        public void onLocationChanged( Location location ) {
            updateCurrentLocation( location );
        }

        @Override
        public void onStatusChanged( String s, int i, Bundle bundle ) {
        }

        @Override
        public void onProviderEnabled( String s ) {
        }

        @Override
        public void onProviderDisabled( String s ) {
        }
    }
}
