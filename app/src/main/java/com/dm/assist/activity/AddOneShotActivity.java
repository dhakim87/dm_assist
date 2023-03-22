package com.dm.assist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.assist.R;
import com.dm.assist.model.OneShot;

public class AddOneShotActivity extends AppCompatActivity {

    private EditText oneShotNameEditText;
    private EditText oneShotDescriptionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_one_shot);

        oneShotNameEditText = findViewById(R.id.oneShotNameEditText);
        oneShotDescriptionEditText = findViewById(R.id.oneShotDescriptionEditText);

        OneShot editChar = this.getIntent().getParcelableExtra("oneShot");
        if (editChar != null) {
            oneShotNameEditText.setText(editChar.name);
            oneShotDescriptionEditText.setText(editChar.desc);
        }
    }

    @Override
    public void onBackPressed() {
        saveOneShot();
    }

    private void saveOneShot() {
        OneShot toSave = new OneShot(
            oneShotNameEditText.getText().toString().trim(),
            oneShotDescriptionEditText.getText().toString().trim()
        );

        OneShot toEdit = this.getIntent().getParcelableExtra("oneShot");
        if (toEdit != null) {
            toSave.id = toEdit.id;
        }

        if (toSave.name.equals(""))
            toSave.name = "Unnamed One Shot";

        Intent resultIntent = new Intent();
        resultIntent.putExtra("oneShot", toSave);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
