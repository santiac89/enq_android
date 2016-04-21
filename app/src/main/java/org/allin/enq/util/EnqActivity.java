package org.allin.enq.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;
import org.allin.enq.service.EnqService;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by santiagocarullo on 9/13/15.
 */
public class EnqActivity extends AppCompatActivity {

    Typeface comfortaa_regular = null;
    Typeface comfortaa_bold = null;

    protected EnqService enqService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        comfortaa_regular = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
        comfortaa_bold = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Bold.ttf");
    }

    public void setupActivity() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        setTypefaceForGroup(root);
    }

    public void setupTexts() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        setTypefaceForGroup(root);
    }

    private void setTypefaceForGroup(ViewGroup root) {

        int childCount = root.getChildCount();

        for (int i = 0; i < childCount; i++) {

            View childView = root.getChildAt(i);

            if (childView instanceof ViewGroup)
                setTypefaceForGroup((ViewGroup) childView);
            else if (childView instanceof Button)
                ((Button) childView).setTypeface(getRegular());
            else if (childView instanceof TextView)
                ((TextView) childView).setTypeface(getRegular());
        }
    }

    public Typeface getRegular() {
        return comfortaa_regular;
    }


    protected void bindEnqService(ServiceConnection connection) {
        Intent intent = new Intent(this, EnqService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }


}
