package instamovies.app.in.models;

import android.graphics.drawable.Drawable;

public class ShareIntentModel {

    private String name;
    private String packageName;
    private Drawable icon;
    private int iconId = 0;

    public void setName(String name) {
        this.name = name;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public int getIconId() {
        return iconId;
    }
}