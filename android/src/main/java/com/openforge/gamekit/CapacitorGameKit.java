package com.openforge.gamekit;

import com.openforge.gamekit.GameHelper.GameHelperListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Result;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.leaderboard.*;
import com.google.android.gms.games.achievement.*;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

@NativePlugin()
public class CapacitorGameKit extends Plugin implements GameHelperListener {

    private static final String LOGTAG = "openforge-CapacitorGameKit";

    private static final int ACTIVITY_CODE_SHOW_LEADERBOARD = 0;
    private static final int ACTIVITY_CODE_SHOW_ACHIEVEMENTS = 1;

    private GameHelper gameHelper;

    private CallbackContext authCallbackContext;
    private int googlePlayServicesReturnCode;

    @Override
    public load() {
        Activity activity = getActivity();
        googlePlayServicesReturnCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);

        if (googlePlayServicesReturnCode == ConnectionResult.SUCCESS) {
            gameHelper = new GameHelper(activity, GameHelper.CLIENT_GAMES);
            gameHelper.setup(this);
        } else {
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));
        }
    }

    @PluginMethod()
    public auth(final PluginCall call) {
        saveCall(call);
        checkGameHelper(call);
        gameHelper.beginUserInitiatedSignIn();
    }

    @PluginMethod()
    public signOut(final PluginCall call) {
        checkGameHelper(call);
        gameHelper.signOut();
        call.success();
    }

    @PluginMethod()
    public isSignedIn(final PluginCall call) {
        checkGameHelper(call);
        try {
            JSONObject result = new JSONObject();
            result.put("isSignedIn", gameHelper.isSignedIn());
            call.success(result);
        } catch (JSONException e) {
            Log.w(LOGTAG, "executeIsSignedIn: unable to determine if user is signed in or not", e);
            call.error("executeIsSignedIn: unable to determine if user is signed in or not");
        }
    }

    @PluginMethod()
    public submitScore(final PluginCall call) {
        checkGameHelper(call);
        try {
            if (gameHelper.isSignedIn()) {
                Games.Leaderboards.submitScore(gameHelper.getApiClient(), options.getString("leaderboardId"), options.getInt("score"));
                call.success("executeSubmitScore: score submited successfully");
            } else {
                call.error("executeSubmitScore: not yet signed in");
            }
        } catch (JSONException e) {
            Log.w(LOGTAG, "executeSubmitScore: unexpected error", e);
            call.error("executeSubmitScore: error while submitting score");
        }
    }

    @PluginMethod()
    public submitScoreNow(final PluginCall call) {
        checkGameHelper(call);
        try {
            if (gameHelper.isSignedIn()) {
                PendingResult<Leaderboards.SubmitScoreResult> result = Games.Leaderboards.submitScoreImmediate(gameHelper.getApiClient(), options.getString("leaderboardId"), options.getInt("score"));
                result.setResultCallback(new ResultCallback<Leaderboards.SubmitScoreResult>() {
                    @Override
                    public void onResult(Leaderboards.SubmitScoreResult submitScoreResult) {
                        if (submitScoreResult.getStatus().isSuccess()) {
                            ScoreSubmissionData scoreSubmissionData = submitScoreResult.getScoreData();

                            if (scoreSubmissionData != null) {
                                try {
                                    ScoreSubmissionData.Result scoreResult = scoreSubmissionData.getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME);
                                    JSONObject result = new JSONObject();
                                    result.put("leaderboardId", scoreSubmissionData.getLeaderboardId());
                                    result.put("playerId", scoreSubmissionData.getPlayerId());
                                    result.put("formattedScore", scoreResult.formattedScore);
                                    result.put("newBest", scoreResult.newBest);
                                    result.put("rawScore", scoreResult.rawScore);
                                    result.put("scoreTag", scoreResult.scoreTag);
                                    call.success(result);
                                } catch (JSONException e) {
                                    Log.w(LOGTAG, "executeSubmitScoreNow: unexpected error", e);
                                    call.error("executeSubmitScoreNow: error while submitting score");
                                }
                            } else {
                                call.error("executeSubmitScoreNow: can't submit the score");
                            }
                        } else {
                            call.error("executeSubmitScoreNow error: " + submitScoreResult.getStatus().getStatusMessage());
                        }
                    }
                });
            } else {
                call.error("executeSubmitScoreNow: not yet signed in");
            }
        } catch (JSONException e) {
            Log.w(LOGTAG, "executeSubmitScoreNow: unexpected error", e);
            call.error("executeSubmitScoreNow: error while submitting score");
        }
    }

    @PluginMethod()
    public getPlayerScore(final PluginCall call) {
        checkGameHelper(call);
        try {
            if (gameHelper.isSignedIn()) {
                PendingResult<Leaderboards.LoadPlayerScoreResult> result = Games.Leaderboards.loadCurrentPlayerLeaderboardScore(gameHelper.getApiClient(), options.getString("leaderboardId"), LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC);
                result.setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                    @Override
                    public void onResult(Leaderboards.LoadPlayerScoreResult playerScoreResult) {
                        if (playerScoreResult.getStatus().isSuccess()) {
                            LeaderboardScore score = playerScoreResult.getScore();

                            if (score != null) {
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("playerScore", score.getRawScore());
                                    call.success(result);
                                } catch (JSONException e) {
                                    Log.w(LOGTAG, "executeGetPlayerScore: unexpected error", e);
                                    call.error("executeGetPlayerScore: error while retrieving score");
                                }
                            } else {
                                call.error("There isn't have any score record for this player");
                            }
                        } else {
                            call.error("executeGetPlayerScore error: " + playerScoreResult.getStatus().getStatusMessage());
                        }
                    }
                });
            } else {
                call.error("executeGetPlayerScore: not yet signed in");
            }
        } catch (JSONException e) {
            Log.w(LOGTAG, "executeGetPlayerScore: unexpected error", e);
            call.error("executeGetPlayerScore: error while retrieving score");
        }
    }

    @PluginMethod()
    public showAllLeaderboards(final PluginCall call) {
        saveCall(call);
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            Intent allLeaderboardsIntent = Games.Leaderboards.getAllLeaderboardsIntent(gameHelper.getApiClient());
            startActivityForResult(plugin, allLeaderboardsIntent, ACTIVITY_CODE_SHOW_LEADERBOARD);
            call.success();
        } else {
            Log.w(LOGTAG, "executeShowAllLeaderboards: not yet signed in");
            call.error("executeShowAllLeaderboards: not yet signed in");
        }
    }

    @PluginMethod()
    public showLeaderboard(final PluginCall call) {
        saveCall(call);
        checkGameHelper(call);
        try {
            if (gameHelper.isSignedIn()) {
                Intent leaderboardIntent = Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), options.getString("leaderboardId"));
                startActivityForResult(plugin, leaderboardIntent, ACTIVITY_CODE_SHOW_LEADERBOARD);
                call.success();
            } else {
                Log.w(LOGTAG, "executeShowLeaderboard: not yet signed in");
                call.error("executeShowLeaderboard: not yet signed in");
            }
        } catch (JSONException e) {
            Log.w(LOGTAG, "executeShowLeaderboard: unexpected error", e);
            call.error("executeShowLeaderboard: error while showing specific leaderboard");
        }
    }

    @PluginMethod()
    public showAchievements(final PluginCall call) {
        saveCall(call);
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            Intent achievementsIntent = Games.Achievements.getAchievementsIntent(gameHelper.getApiClient());
            startActivityForResult(plugin, achievementsIntent, ACTIVITY_CODE_SHOW_ACHIEVEMENTS);
            call.success();
        } else {
            Log.w(LOGTAG, "executeShowAchievements: not yet signed in");
            call.error("executeShowAchievements: not yet signed in");
        }
    }

    @PluginMethod()
    public unlockAchievement(final PluginCall call) {
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            Games.Achievements.unlock(gameHelper.getApiClient(), options.optString("achievementId"));
            call.success();
        } else {
            Log.w(LOGTAG, "executeUnlockAchievement: not yet signed in");
            call.error("executeUnlockAchievement: not yet signed in");
        }
    }

    @PluginMethod()
    public unlockAchievementNow(final PluginCall call) {
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            PendingResult<Achievements.UpdateAchievementResult> result = Games.Achievements.unlockImmediate(gameHelper.getApiClient(), options.optString("achievementId"));
            result.setResultCallback(new ResultCallback<Achievements.UpdateAchievementResult>() {
                    @Override
                    public void onResult(Achievements.UpdateAchievementResult achievementResult) {
                        if (achievementResult.getStatus().isSuccess()) {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("achievementId", achievementResult.getAchievementId());
                                call.success(result);
                            } catch (JSONException e) {
                                Log.w(LOGTAG, "executeUnlockAchievementNow: unexpected error", e);
                                call.error("executeUnlockAchievementNow: error while unlocking achievement");
                            }
                        } else {
                            call.error("executeUnlockAchievementNow error: " + achievementResult.getStatus().getStatusMessage());
                        }
                    }
                });
        } else {
            Log.w(LOGTAG, "executeUnlockAchievementNow: not yet signed in");
            call.error("executeUnlockAchievementNow: not yet signed in");
        }
    }

    @PluginMethod()
    public incrementAchievement(final PluginCall call) {
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            Games.Achievements.increment(gameHelper.getApiClient(), options.optString("achievementId"), options.optInt("numSteps"));
            call.success();
        } else {
            Log.w(LOGTAG, "executeIncrementAchievement: not yet signed in");
            call.error("executeIncrementAchievement: not yet signed in");
        }
    }

    @PluginMethod()
    public incrementAchievementNow(final PluginCall call) {
        checkGameHelper(call);
        if (gameHelper.isSignedIn()) {
            PendingResult<Achievements.UpdateAchievementResult> result = Games.Achievements.incrementImmediate(gameHelper.getApiClient(), options.optString("achievementId"), options.optInt("numSteps"));
            result.setResultCallback(new ResultCallback<Achievements.UpdateAchievementResult>() {
                    @Override
                    public void onResult(Achievements.UpdateAchievementResult achievementResult) {
                        if (achievementResult.getStatus().isSuccess()) {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("achievementId", achievementResult.getAchievementId());
                                call.success(result);
                            } catch (JSONException e) {
                                Log.w(LOGTAG, "executeIncrementAchievementNow: unexpected error", e);
                                call.error("executeIncrementAchievementNow: error while incrementing achievement");
                            }
                        } else {
                            call.error("executeIncrementAchievementNow error: " + achievementResult.getStatus().getStatusMessage());
                        }
                    }
                });
            call.success();
        } else {
            Log.w(LOGTAG, "executeIncrementAchievement: not yet signed in");
            call.error("executeIncrementAchievement: not yet signed in");
        }
    }

    @PluginMethod()
    public showPlayer(final PluginCall call) {
        checkGameHelper(call);
        try {
            if (gameHelper.isSignedIn()) {

                Player player = Games.Players.getCurrentPlayer(gameHelper.getApiClient());

                JSONObject playerJson = new JSONObject();
                playerJson.put("displayName", player.getDisplayName());
                playerJson.put("playerId", player.getPlayerId());
                playerJson.put("title", player.getTitle());
                playerJson.put("iconImageUrl", player.getIconImageUrl());
                playerJson.put("hiResIconImageUrl", player.getHiResImageUrl());

                call.success(playerJson);

            } else {
                Log.w(LOGTAG, "executeShowPlayer: not yet signed in");
                call.error("executeShowPlayer: not yet signed in");
            }
        }
        catch(Exception e) {
            Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
            call.error("executeShowPlayer: Error providing player data");
        }
    }

    private checkGameHelper(final PluginCall call) {
        if (gameHelper == null) {
            Log.w(LOGTAG, String.format("Tried calling: '" + action + "', but error with GooglePlayServices"));
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));

            JSONObject googlePlayError = new JSONObject();
            googlePlayError.put("errorCode", googlePlayServicesReturnCode);
            googlePlayError.put("errorString", GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode));

            JSONObject result = new JSONObject();
            result.put("googlePlayError", googlePlayError);
            call.error(result);

            return true;
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
      super.handleOnActivityResult(requestCode, resultCode, data);
      gameHelper.onActivityResult(requestCode, resultCode, intent);
    }
}
