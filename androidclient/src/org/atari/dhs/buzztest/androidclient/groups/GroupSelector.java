package org.atari.dhs.buzztest.androidclient.groups;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups.GroupItem;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.ListAsyncSupportActivity;

public class GroupSelector extends ListAsyncSupportActivity
{
    public static final String EXTRA_RESULT_SELECTED_GROUPS = "selectedGroups";

    private ProgressDialog loadingDialog;
    private ListAdapter adapter;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.group_selector );

        getListView().setChoiceMode( ListView.CHOICE_MODE_MULTIPLE );

        loadingDialog = ProgressDialog.show( this, null, "Loading groups\u2026", true, true, new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel( DialogInterface dialogInterface ) {
                finish();
            }
        } );

        loadingDialog.show();
        startAsyncTask( new LoadGroupsTask( this ) );
    }

    public void handleLoadResult( LoadGroupsTask.Result result ) {
        loadingDialog.dismiss();

        if( result.errorMessage != null ) {
            Toast.makeText( this, "Error loading groups: " + result.errorMessage, Toast.LENGTH_LONG ).show();
        }
        else {
            List<GroupItem> items = result.groups.items;
            List<GroupItemInfo> wrappers = new ArrayList<GroupItemInfo>( items.size() );
            for( GroupItem item : items ) {
                wrappers.add( new GroupItemInfo( item.title, item.id ) );
            }
            adapter = new ArrayAdapter<GroupItemInfo>( this, android.R.layout.simple_list_item_multiple_choice, wrappers );
            setListAdapter( adapter );
        }
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void selectClicked( View view ) {
        ArrayList<GroupItemInfo> selectedGroups = new ArrayList<GroupItemInfo>();
        SparseBooleanArray posList = getListView().getCheckedItemPositions();
        int n = adapter.getCount();
        for( int i = 0 ; i < n ; i++ ) {
            if( posList.get( i ) ) {
                selectedGroups.add( (GroupItemInfo)adapter.getItem( i ) );
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra( EXTRA_RESULT_SELECTED_GROUPS, new SelectedGroups( selectedGroups.toArray( new GroupItemInfo[selectedGroups.size()] ) ) );
        setResult( RESULT_OK, resultIntent );
        finish();
    }
}
