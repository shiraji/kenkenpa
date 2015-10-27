package com.github.shiraji.kenkenpasample.switchfragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.shiraji.kenkenpasample.R;

public class SwitchSecondFragment extends Fragment {
    public static SwitchSecondFragment newInstance() {
        SwitchSecondFragment fragment = new SwitchSecondFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.switch_fragment, container, false);
        TextView textView = (TextView)view.findViewById(R.id.fragment_switch_text_view);
        textView.setText(R.string.fragment_text_view_second);
        return view;
    }
}
