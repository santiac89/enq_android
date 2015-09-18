package org.allin.enq.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by santiagocarullo on 9/17/15.
 */
public class EnqProperties {

    private static Properties properties;

    public static void load(Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream inputStream = null;

        try {
            inputStream = assetManager.open("enq.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            properties = new Properties();
            properties.load(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
