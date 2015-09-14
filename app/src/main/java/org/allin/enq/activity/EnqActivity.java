package org.allin.enq.activity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import org.allin.enq.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by santiagocarullo on 9/13/15.
 */
public class EnqActivity extends ActionBarActivity {

    @Bind(R.id.toolbar_title_text_view) TextView toolbarTitleTextView;

    Typeface comfortaa_regular = null;
    Typeface comfortaa_bold = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        comfortaa_regular = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Regular.ttf");
        comfortaa_bold = Typeface.createFromAsset(getAssets(), "fonts/Comfortaa-Bold.ttf");
    }

    public void setupActionBar(Integer toolbarId, String title) {

        Toolbar toolbar = (Toolbar) findViewById(toolbarId);

        ButterKnife.bind(toolbar);

        toolbarTitleTextView.setTypeface(comfortaa_bold);

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
