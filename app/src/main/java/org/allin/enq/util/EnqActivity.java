package org.allin.enq.util;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by santiagocarullo on 9/13/15.
 */
public class EnqActivity extends ActionBarActivity {

    @Nullable
    @Bind(R.id.toolbar_title_text_view) TextView toolbarTitleTextView;

    Typeface comfortaa_regular = null;
    Typeface comfortaa_bold = null;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupTexts();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        comfortaa_regular = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
        comfortaa_bold = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Bold.ttf");
    }

    public void setupActivity(int toolbarId, String toolbarTitle) {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        setTypefaceForGroup(root);
        setupActionBar(toolbarId, toolbarTitle);
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

    private void setupActionBar(Integer toolbarId, String title) {
        Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        ButterKnife.bind(toolbar);
        toolbarTitleTextView.setTypeface(getBold());
        toolbarTitleTextView.setText(title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
    }

    public Typeface getBold() {
        return comfortaa_bold;
    }

    public Typeface getRegular() {
        return comfortaa_regular;
    }

}
