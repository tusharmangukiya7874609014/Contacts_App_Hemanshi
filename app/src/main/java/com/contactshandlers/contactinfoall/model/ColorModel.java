package com.contactshandlers.contactinfoall.model;

public class ColorModel {
    private String color;
    private boolean isSelected;

    public ColorModel(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
