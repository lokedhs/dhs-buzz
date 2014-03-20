package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>
 */
public class Users implements EntryPoint
{
    public void onModuleLoad() {
        RootLayoutPanel root = RootLayoutPanel.get();
        root.add( new UserListEditor() );
    }
}
