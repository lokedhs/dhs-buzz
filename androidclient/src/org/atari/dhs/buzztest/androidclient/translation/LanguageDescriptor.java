package org.atari.dhs.buzztest.androidclient.translation;

class LanguageDescriptor
{
    public String languageName;
    public boolean includeInMenu;

    LanguageDescriptor( String languageName, boolean includeInMenu ) {
        this.languageName = languageName;
        this.includeInMenu = includeInMenu;
    }
}
