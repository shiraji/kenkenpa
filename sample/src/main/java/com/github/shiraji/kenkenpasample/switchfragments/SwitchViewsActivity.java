package com.github.shiraji.kenkenpasample.switchfragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import android.view.View;
import com.github.shiraji.kenkenpasample.R;

public class SwitchViewsActivity extends ActionBarActivity {

    FragmentSwitcher mSwitchFragmentHelper;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, SwitchViewsActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.switch_layout);

        mSwitchFragmentHelper = FragmentSwitcher.create(getSupportFragmentManager());

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitchFragmentHelper.switchFragment(getSupportFragmentManager());
            }
        });
    }

}
