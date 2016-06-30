package com.daose.secretcalculator.welcome;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.daose.secretcalculator.R;

public class WelcomeScreenFragment extends Fragment {
    private static final String ARG_ITEM_NUMBER = "intro_number";

    private int mNumber;

    public WelcomeScreenFragment() {
        // Required empty public constructor
    }

    public static WelcomeScreenFragment create(int mNumber) {
        WelcomeScreenFragment fragment = new WelcomeScreenFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ITEM_NUMBER, mNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNumber = getArguments().getInt(ARG_ITEM_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        switch(mNumber){
            case 0:
                return inflater.inflate(R.layout.fragment_welcome_screen, container, false);
            case 1:
                return inflater.inflate(R.layout.fragment_register_screen, container, false);
            default:
                return inflater.inflate(R.layout.fragment_welcome_screen, container, false);
        }
    }
}
