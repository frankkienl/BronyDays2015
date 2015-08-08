package nl.frankkie.bronydays2015.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import org.acra.ACRA;

/**
 * Created by fbouwens on 21-01-15.
 */
public class GoogleApiUtil {

    /**
     * Set user email, according to Google+
     * @param context
     * @param userEmail emailadres from Google+
     */
    public static void setUserEmail(Context context, String userEmail){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("google_plus_email", userEmail).commit();
    }

    /**
     * Get user email
     * @param context
     * @return emailadres of user according to Google+, empty string when unknown.
     */
    public static String getUserEmail(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("google_plus_email","");
    }

    public static void setUserLoggedIn(Context context, boolean loggedIn){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("google_plus_loggedin", loggedIn).commit();
    }

    public static boolean isUserLoggedIn(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("google_plus_loggedin",false);
    }
    
    public static void setUserNickname(Context context, String name){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String nickname = name;
        prefs.edit().putString("user_nickname", nickname).apply();
    }
    
    public static String getUserNickname(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("user_nickname","");                
    }

    public static void showAchievements(Activity context, GoogleApiClient mGoogleApiClient){
        int REQUEST_ACHIEVEMENTS = 9009;
        try {
            context.startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), REQUEST_ACHIEVEMENTS);
        } catch (Exception e){
            Toast.makeText(context,"Cannot show Achievements..", Toast.LENGTH_LONG).show();
            ACRA.getErrorReporter().handleException(e);
        }
    }

    /**
     * Interface to get GoogleApiClient from Activity.
     * Best way to communicate between fragment and activity 
     * http://developer.android.com/training/basics/fragments/communicating.html 
     */
    public static interface GiveMeGoogleApiClient {
        public GoogleApiClient getGoogleApiClient();
    }
}
