package org.allin.enq.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.TextView;

import org.allin.enq.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Agustina on 14/09/2015.
 */
public class ListItemActivity extends EnqActivity {


    @Bind(R.id.list_activity_toolbar) Toolbar listActivityToolbar;
    @Bind(R.id.label_prom_esp) TextView labelPromEsp;
    @Bind(R.id.group_button) Button group_Button;
    @Bind(R.id.VestimatedTime) TextView vEstimatedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_item);

        ButterKnife.bind(this);

        setupActionBar(listActivityToolbar.getId(), "Seleccione el tipo de atención");

        labelPromEsp.setTypeface(comfortaa_regular);
        group_Button.setTypeface(comfortaa_regular);
        vEstimatedTime.setTypeface(comfortaa_regular);
    }
}

