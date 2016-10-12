package com.demo.sunny.flapybird.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.demo.sunny.flapybird.view.FlapyBird;

public class MainActivity extends AppCompatActivity {
    FlapyBird mGame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mGame = new FlapyBird(this);
        setContentView(mGame);
    }

}
