package org.allin.enq.activity;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.allin.enq.R;


public class WaitingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intnt = getIntent();

        TextView estimatedTimeView = (TextView) findViewById(R.id.VestimatedTime);
        TextView numberView = (TextView) findViewById(R.id.Vnumber);

        estimatedTimeView.setText(getIntent().getStringExtra("estimated"));
        numberView.setText(getIntent().getStringExtra("number"));


    }
}
