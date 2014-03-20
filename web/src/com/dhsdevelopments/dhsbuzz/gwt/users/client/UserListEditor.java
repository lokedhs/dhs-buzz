package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import com.dhsdevelopments.dhsbuzz.gwt.users.client.tools.DefaultAsyncCallback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Tree;

public class UserListEditor extends Composite
{
    interface UserListEditorUiBinder extends UiBinder<DockLayoutPanel, UserListEditor>
    {
    }

    private static UserListEditorUiBinder ourUiBinder = GWT.create( UserListEditorUiBinder.class );

    @UiField
    Tree tree;

    public UserListEditor() {
        DockLayoutPanel rootElement = ourUiBinder.createAndBindUi( this );
        initWidget( rootElement );

        UsersService.App.getInstance().loadProfiles( new DefaultAsyncCallback<ProfileList>()
        {
            @Override
            public void onSuccess( ProfileList result ) {
                fillInProfiles( result );
            }
        } );
    }

    private void fillInProfiles( ProfileList result ) {
        for( ProfileCategory category : result.getCategories() ) {
            tree.addItem( category.getName() );
        }
    }
}