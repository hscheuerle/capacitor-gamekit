package com.hscheuerle.myplayservices;

import android.app.AlertDialog;
import android.content.Intent;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.Auth;

import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnSuccessListener;

@NativePlugin()
public class MyPlayServices extends Plugin {
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_LEADERBOARD_UI = 9004;

    @PluginMethod()
    public void submitScore(PluginCall call) {
        GoogleSignInAccount mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this.getContext());
        if (mGoogleSignInAccount != null) {
            Games.getLeaderboardsClient(this.getContext(), mGoogleSignInAccount)
                    .submitScore(call.getString("leaderboardId"), call.getInt("score"));
        } else {
            JSObject ret = new JSObject();
            ret.put("message", "last signed in account was null");
            call.success(ret);
        }

    }

    @PluginMethod()
    public void startSignInIntent(PluginCall call) {
        GoogleSignInOptions signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN;
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this.getContext(), signInOptions);
        Intent intent = signInClient.getSignInIntent();
        startActivityForResult(call, intent, MyPlayServices.RC_SIGN_IN);
    }

    @PluginMethod()
    public void showLeaderboard(final PluginCall call) {
        GoogleSignInAccount mGoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(getContext());
        Games.getLeaderboardsClient(getContext(), mGoogleSignInAccount)
                .getLeaderboardIntent(call.getString("leaderboardId"))
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(call, intent, RC_LEADERBOARD_UI);
                    }
                });
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                GoogleSignInAccount signedInAccount = result.getSignInAccount();
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message == null || message.isEmpty()) {
                    message = "Unknown sign in error";
                }
//                new AlertDialog.Builder(this.getContext()).setMessage(message)
//                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }
}