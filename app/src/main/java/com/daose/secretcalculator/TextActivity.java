package com.daose.secretcalculator;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextActivity extends AppCompatActivity {

    private EditText editText;
    private String text;
    private static final String LOG_TAG = "TextActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        editText = (EditText) findViewById(R.id.editText);
        loadFile();
    }

    private void loadFile() {
        Log.d(LOG_TAG, "loadFile");
        try {
            FileInputStream fin = openFileInput("secretFile.txt");
            text = "";
            int c;
            while ((c = fin.read()) != -1) {
                text += Character.toString((char) c);
            }
            fin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        editText.setText(text);
    }

    public void save(View view) {
        Log.d(LOG_TAG, "save");
        try {
            FileOutputStream fos = openFileOutput("secretFile.txt", MODE_PRIVATE);
            fos.write(editText.getText().toString().getBytes());
            fos.close();
            Toast.makeText(TextActivity.this, "saved", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void back(View view){
        Log.d(LOG_TAG, "back");
        Intent intent = new Intent(this, CalculatorActivity.class);
        startActivity(intent);
    }

}
