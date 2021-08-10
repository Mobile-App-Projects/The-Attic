package com.mutinda.theattic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mutinda.theattic.databinding.ActivityMainBinding;

public class RegisterActivity extends AppCompatActivity {
    private Button registerBtn;
    private EditText emailField, usernameField, passwordField;
    private TextView loginTxtView;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference userDetailsReference;
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        loginTxtView = findViewById(R.id.loginTxtView);
        registerBtn = findViewById(R.id.register_btn);
        emailField = findViewById(R.id.email);
        usernameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();

        userDetailsReference = database.getReference().child("Users");

        loginTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(RegisterActivity.this, "LOADING...", Toast.LENGTH_LONG).show();

                final String username = usernameField.getText().toString();
                final String email = emailField.getText().toString();
                final String password = passwordField.getText().toString();

                if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)){
                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            String user_id = mAuth.getCurrentUser().getUid();

                            DatabaseReference current_user_db = userDetailsReference.child(user_id);
                            current_user_db.child("Username").setValue(username);
                            current_user_db.child("Image").setValue("Default");

                            Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();

                            Intent profIntent = new Intent(RegisterActivity.this, ProfileActivity.class);
                            profIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(profIntent);

                        }
                    });
                }else{
                    Toast.makeText(RegisterActivity.this, "Complete all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}