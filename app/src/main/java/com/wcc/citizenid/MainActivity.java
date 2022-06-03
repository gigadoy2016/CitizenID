package com.wcc.citizenid;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView mResponseTextView;
    private Button mPowerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPowerButton = findViewById(R.id.button_1);
        mResponseTextView = findViewById(R.id.main_text_view_response);
    }

    public void btn1Click(View v){
        mResponseTextView.setText("Hello 000");
    }


}