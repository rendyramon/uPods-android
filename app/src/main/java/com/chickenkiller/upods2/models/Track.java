package com.chickenkiller.upods2.models;

import java.io.Serializable;

/**
 * Created by alonzilberman on 8/31/15.
 */
public abstract class Track implements Serializable {

    protected String title;
    protected String audeoUrl;
    protected boolean isSelected;

    public Track(){
        this.title = "";
        this.audeoUrl = "";
        this.isSelected = false;
    }

    public abstract String getDuration();

    public abstract String getDate();

    public abstract String getTitle();

    public abstract String getAudeoUrl();

    public abstract String getSubTitle();

    public abstract String getInfo();

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAudeoUrl(String audeoUrl) {
        this.audeoUrl = audeoUrl;
    }
}
