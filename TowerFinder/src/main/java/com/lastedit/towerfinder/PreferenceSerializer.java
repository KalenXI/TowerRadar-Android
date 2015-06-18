package com.lastedit.towerfinder;

import android.content.SharedPreferences;

import org.apache.pig.impl.util.ObjectSerializer;

import java.io.Serializable;

/**
 * This was created by kevinvinck on 6/21/13.
 */
public class PreferenceSerializer {

    public void setObject(SharedPreferences prefs, String key, Serializable obj) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, ObjectSerializer.serialize(obj));
        editor.apply();
    }

    public Object getObject(SharedPreferences prefs, String key) {
        return ObjectSerializer.deserialize(prefs.getString(key, null));
    }

}
