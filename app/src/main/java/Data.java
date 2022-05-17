package com.maven.maven;

public class Data {

    private String title;
    private String link;
    private String description;
    private String web;

    public Data(){
        this.title = title;
        this.link = link;
        this.description = description;
        this.web = web;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public String getWeb() {
        return web;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setWeb(String web) {
        this.web = web;
    }
}
