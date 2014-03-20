package org.atari.dhs.buzztest.androidclient.displaybuzz;

public class LinkDescriptor
{
    private String url;
    private String description;
    private LinkType type;

    public LinkDescriptor( String url, String description, LinkType type ) {
        this.url = url;
        this.description = description;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public LinkType getType() {
        return type;
    }

    public enum LinkType
    {
        URL,
        HASHTAG
    }
}
