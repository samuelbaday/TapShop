package sbsolutions.tapshot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginPage extends AppCompatActivity {

    @Bind(R.id.facebook_profile) ProfilePictureView profilePictureView;
    @Bind(R.id.facebook_login) LoginButton loginButton;

    @Bind(R.id.progressBarHolder)
    ViewGroup progressBarHolder;
    AlphaAnimation inAnimation;
    AlphaAnimation outAnimation;

    CallbackManager callbackManager;
    private static final String EMAIL = "email";
    private FirebaseAuth mAuth;

    UserProfile userProfile = new UserProfile();

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();
            Log.i("FIRE_AUTH","EMAIL: " + email);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent mainAct = new Intent(LoginPage.this,MainActivity.class);
                    mainAct.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainAct);
                }
            }).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);
        ButterKnife.bind(this);

        mAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

//        printHashKey(getBaseContext());

        loginButton = (LoginButton) findViewById(R.id.facebook_login);
        loginButton.setReadPermissions(Arrays.asList(EMAIL));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final String[] userId = new String[1];
                showLoading();
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v("LoginActivity", response.toString());

                                // Application code
                                String id = null;
                                String email = null;
                                try {
                                    email = object.getString("email");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    userId[0] = object.getString("id");
                                    Log.i("FB_PIC","VAL: " + userId[0]);
                                    setProfilePic(userId[0]);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                fireAuthMake(email, userId[0]);
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {
                hideLoading();
            }

            @Override
            public void onError(FacebookException exception) {
                hideLoading();
            }
        });

       checkFbLogin();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void printHashKey(Context pContext) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i("HASH_TAG", "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e("HASH_TAG", "printHashKey()", e);
        } catch (Exception e) {
            Log.e("HASH_TAG", "printHashKey()", e);
        }
    }

    public void checkFbLogin(){
        if(isLoggedIn()){
            Log.i("FB_LOGIN", String.valueOf(AccessToken.getCurrentAccessToken()));
            final String[] userId = new String[1];
            GraphRequest request = GraphRequest.newMeRequest(
                    AccessToken.getCurrentAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            Log.v("LoginActivity", response.toString());

                            String email = null;
                            try {
                                email = object.getString("email");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                userId[0] = object.getString("id");
                                Log.i("FB_PIC","VAL: " + userId[0]);
                                setProfilePic(userId[0]);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                userProfile.setUserName(object.getString("name"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            userProfile.setUserEmail(email);
                            userProfile.setUserId(userId[0]);
                            fireAuthMake(email,userId[0]);
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,name,email,gender");
            request.setParameters(parameters);
            request.executeAsync();
        } else {
            Log.i("FB_LOGIN", String.valueOf(AccessToken.getCurrentAccessToken()));
        }

    }

    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
//        Log.i("FB_LOGIN", "TOKEN: " + accessToken.isExpired());
        return accessToken != null;
    }

    public void setProfilePic(String userId){
//        profilePictureView = (ProfilePictureView) findViewById(R.id.facebook_profile);
//        profilePictureView.setProfileId(userId);
    }

    public void fireAuthMake(final String email, final String id){
        mAuth.createUserWithEmailAndPassword(email, id)
                .addOnCompleteListener(LoginPage.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FIRE_AUTH", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            mAuth.signInWithEmailAndPassword(email, id)
                                    .addOnCompleteListener(LoginPage.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Sign in success, update UI with the signed-in user's information
                                                Log.d("FIRE_AUTH", "signInWithEmail:success");
                                                FirebaseUser user = mAuth.getCurrentUser();
//
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Intent mainAct = new Intent(LoginPage.this,MainActivity.class);
                                                        mainAct.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(mainAct);
                                                    }
                                                }).start();
                                            } else {
                                                // If sign in fails, display a message to the user.
                                                Log.w("FIRE_AUTH", "signInWithEmail:failure", task.getException());

                                                if(task.getException().toString().contains("already in use")){
                                                    Log.i("ALREADY_EXISTS","ALREADY_EXISTS");
                                                }

                                            }

                                        }
                                    });
                        } else {
                            hideLoading();
                            Log.w("FIRE_AUTH", "createUserWithEmail:failure", task.getException());
                            if(task.getException().toString().contains("already in use")){
                                Log.i("ALREADY_EXISTS","ALREADY_EXISTS");
                                signInFirebase(email,id);
                            }
                        }
                    }
                });
    }

    public void signInFirebase(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FIRE_AUTH", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            hideLoading();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent mainAct = new Intent(LoginPage.this,MainActivity.class);
                                    mainAct.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(mainAct);
                                }
                            }).start();
                        } else {
                            hideLoading();
                            // If sign in fails, display a message to the user.
                            Log.w("FIRE_AUTH", "signInWithEmail:failure", task.getException());
                        }
                    }
                });
    }

    public void showLoading() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        inAnimation = new AlphaAnimation(0f, 1f);
                        inAnimation.setDuration(200);
                        progressBarHolder.setAnimation(inAnimation);
                        progressBarHolder.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();

    }

    public void hideLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outAnimation = new AlphaAnimation(1f, 0f);
                outAnimation.setDuration(200);
                progressBarHolder.setAnimation(outAnimation);
                progressBarHolder.setVisibility(View.GONE);
            }
        });
    }
}
