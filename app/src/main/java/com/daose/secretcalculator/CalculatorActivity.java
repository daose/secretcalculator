package com.daose.secretcalculator;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daose.secretcalculator.welcome.WelcomeSlideActivity;

//TODO: operations to more than two numbers
public class CalculatorActivity extends AppCompatActivity{

    private enum Operation{
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE
    }

    private static final String LOG_TAG = "CalcActivity";
    private TextView calcDisplay, currentOp;

    private boolean isOperating = false;
    private boolean firstInstall = false;

    private double firstNumber = 0;
    private double secondNumber = 0;

    private Operation op;

    private SharedPreferences pref;

    private Button eButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences("first_install", 0);
        setContentView(R.layout.activity_calculator);

        if(!pref.contains("hasInstalled")){
            firstInstall = true;
        } else {
            firstInstall = false;
        }
        /*
        if(!pref.contains("hasInstalled")){
            Intent intent = new Intent(this, WelcomeSlideActivity.class);
            startActivity(intent);
        }
        */

        setupCalculator();
    }

    private void setupCalculator(){
        calcDisplay = (TextView) findViewById(R.id.calcDisplay);
        currentOp = (TextView) findViewById(R.id.currentOperation);
        eButton = (Button) findViewById(R.id.equal);

        if(firstInstall){
            calcDisplay.setText("set pass");
            eButton.setText("SET");
            clearOnNextAppend(calcDisplay.getText());
        }
        Log.d(LOG_TAG, "pass: " + pref.getString("pass", null));
    }


    public void append(View view){
        Button button = (Button) findViewById(view.getId());
        assert button != null;

        if(calcDisplay.getText().toString().equals("0")){
            calcDisplay.setText(button.getText());
        } else {
            calcDisplay.append(button.getText());
        }
    }

    public void setOperation(View view){
        if(isOperating || calcDisplay.getText().toString().equals("")) return;
        isOperating = true;

        Button button = (Button) findViewById(view.getId());
        assert button != null;

        String operationName = getResources().getResourceEntryName(view.getId());
        Log.d(LOG_TAG, operationName);
        switch(operationName){
            case "add":
                op = Operation.ADD;
                break;
            case "subtract":
                op = Operation.SUBTRACT;
                break;
            case "multiply":
                op = Operation.MULTIPLY;
                break;
            case "divide":
                op = Operation.DIVIDE;
                break;
            default:
                op = null;
        }
        firstNumber = Double.parseDouble(calcDisplay.getText().toString());
        currentOp.setText(button.getText());
        clearOnNextAppend(calcDisplay.getText());
    }

    public void clearOnNextAppend(CharSequence text){
        calcDisplay.setHint(text);
        calcDisplay.setText("");
    }

    public void clear(View view){
        calcDisplay.setText("0");
    }

    public void doOperation(View view){

        if(firstInstall){
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("pass", calcDisplay.getText().toString());
            editor.apply();
            goToVoiceActivity();
        }

        if(calcDisplay.getText().toString().equals(pref.getString("pass", null))){
            Log.d(LOG_TAG, "password detected!");
            goToVoiceActivity();
        }

        if(!isOperating) return;
        Log.d(LOG_TAG, "doOperation");
        isOperating = false;
        currentOp.setText("");
        secondNumber = Double.parseDouble(calcDisplay.getText().toString());
        double answer = 0.0;
        switch(op){
            case ADD:
                answer = firstNumber + secondNumber;
                break;
            case SUBTRACT:
                answer = firstNumber - secondNumber;
                break;
            case MULTIPLY:
                answer = firstNumber * secondNumber;
                break;
            case DIVIDE:
                answer = firstNumber / secondNumber;
            default:
                calcDisplay.setText("Error");
                break;
        }
        calcDisplay.setText(String.valueOf(answer));
        clearOnNextAppend(calcDisplay.getText());
    }

    private void goToVoiceActivity(){
        Intent intent = new Intent(this, VoiceActivity.class);
        intent.putExtra("isNewUser", firstInstall);
        startActivity(intent);
    }
}
