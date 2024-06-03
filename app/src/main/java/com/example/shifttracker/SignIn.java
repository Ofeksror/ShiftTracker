package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getFirestoreInstance;
import static com.example.shifttracker.FirebaseManager.syncUserWithDatabase;
import static com.example.shifttracker.FirebaseManager.setUserInstance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import data_models.Job;
import data_models.User;

public class SignIn extends AppCompatActivity implements View.OnClickListener {

    private FirebaseManager firebaseManager;
    private String TAG = "SIGN IN";
    private TextView signInTitle;
    private EditText inputEmail, inputPassword;
    private Button signInButton, signUpButton, signInGoogleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        this.firebaseManager = FirebaseManager.getInstance();

        this.signInTitle = (TextView) findViewById(R.id.signInTitle);
        this.inputEmail = (EditText) findViewById(R.id.inputEmail);
        this.inputPassword = (EditText) findViewById(R.id.inputPassword);
        this.signInButton = (Button) findViewById(R.id.signInButton);
        this.signUpButton = (Button) findViewById(R.id.signUpButton);
        this.signInGoogleButton = (Button) findViewById(R.id.signInGoogleButton);

        this.signInButton.setOnClickListener(this);
        this.signUpButton.setOnClickListener(this);
        this.signInGoogleButton.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        reload();
    }

    public void reload() {
        inputPassword.setText("");

        if (getAuthInstance().getCurrentUser() != null) {
            Intent MainActivityIntent = new Intent(this, MainActivity.class);
            startActivity(MainActivityIntent);
        }
    }

    public boolean validateCredentials(String email, String password) {
        return true;
    }

    public void signInCredentials(String email, String password) {
        if (!validateCredentials(email, password)) {
            return;
        }

        getAuthInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            syncUserWithDatabase();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SignIn.this, "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }

                        reload();
                    }
                });
    }

    public void createAccount(String email, String password) {
        if (!validateCredentials(email, password)) {
            return;
        }

        getAuthInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");

                            String userId = getAuthInstance().getCurrentUser().getUid().toString();

                            User createdUser = new User(userId, new ArrayList<Job>());

                            getFirestoreInstance().collection("users")
                                    .document(userId)
                                    .set(createdUser)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "DocumentSnapshot successfully written!");
                                            Toast.makeText(SignIn.this, "Successfuly created user", Toast.LENGTH_SHORT).show();
                                            setUserInstance(createdUser);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error writing document", e);
                                            Toast.makeText(SignIn.this, "Error while creating your user: " + e, Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignIn.this, "Authentication failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }

                        reload();
                    }
                });
    }


    @Override
    public void onClick(View view) {
        if (view == signInGoogleButton) {
            // TODO
            return;
        }

        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();

        if (!validateCredentials(email, password)) {
            Toast.makeText(SignIn.this, "Invalid credentials, Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (view == signInButton) {
            signInCredentials(email, password);
        }
        else if (view == signUpButton) {
            createAccount(email, password);
        }
    }
}