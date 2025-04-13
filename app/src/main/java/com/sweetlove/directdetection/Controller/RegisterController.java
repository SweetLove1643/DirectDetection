package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

public class RegisterController extends AppCompatActivity {

    private TextView fullname_input, username_input, password_input, forgotpassword, signin, family_username_input;
    private Spinner usertype;
    private Spinner roleSpinner;
    private ImageButton back_btn;
    private Button signup;
    private CheckBox rememberme;
    private FirebaseAuth mauth;
    private FirebaseFirestore db;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mauth.getCurrentUser();
        if(currentUser != null){
            Intent home_form = new Intent(getApplicationContext(), HomeController.class);
            startActivity(home_form);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_controller);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullname_input = findViewById(R.id.name_input);
        username_input = findViewById(R.id.username_input);
        password_input = findViewById(R.id.password_input);
        family_username_input = findViewById(R.id.family_username_input);

        usertype = findViewById(R.id.spinner_usertype);

        forgotpassword = findViewById(R.id.forgotpassword_btn);
        signin = findViewById(R.id.signin_btn);

        signup = findViewById(R.id.signup_btn);
        back_btn = findViewById(R.id.back_btn);
        rememberme = findViewById(R.id.rememberme_checkbox);

        String[] userTypes = {"User", "Family user"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_selected_item,
                userTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usertype.setAdapter(adapter);
        usertype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                if (selectedItem.equals("Family user")) {
                    family_username_input.setVisibility(View.VISIBLE);
                } else {
                    family_username_input.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                family_username_input.setVisibility(View.GONE);
            }
        });


        signin.setOnClickListener(v -> {
            Intent login_form = new Intent(RegisterController.this, LoginController.class);
            startActivity(login_form);
            finish();
        });

        back_btn.setOnClickListener(v -> finish());

    }
}