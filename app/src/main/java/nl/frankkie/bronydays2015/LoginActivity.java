package nl.frankkie.bronydays2015;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.acra.ACRA;

import nl.frankkie.bronydays2015.util.GcmUtil;
import nl.frankkie.bronydays2015.util.GoogleApiUtil;
import nl.frankkie.bronydays2015.util.Util;

/**
 * Created by FrankkieNL on 18-1-2015.
 */
public class LoginActivity extends ActionBarActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //<editor-fold desc="ActionBar Stuff">
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    Toolbar mToolbar;
    ActionBarDrawerToggle mDrawerToggle;

    public void initToolbar() {
        mTitle = getTitle();
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        setSupportActionBar(mToolbar);
        ///
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        // Set up the drawer.
        mDrawerToggle = mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void restoreActionBar() {
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Callback from Hamburger-menu
     *
     * @param position
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Util.navigateFromNavDrawer(this, position);
    }
    //</editor-fold>

    //Google Plus Stuff
    //https://github.com/googleplus/gplus-quickstart-android
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private static final String SAVED_PROGRESS = "sign_in_progress";
    private static final int RC_SIGN_IN = 0; //requestcode
    //
    private GoogleApiClient mGoogleApiClient;
    private int mSignInProgress;
    private PendingIntent mSignInIntent;
    private int mSignInError;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        initToolbar();

        initGooglePlus();

        initUI();
    }

    public void initGooglePlus() {
        mGoogleApiClient = buildGoogleApiClient();
    }

    private GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
    }

    public void initUI() {
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, R.string.logging_in, Toast.LENGTH_LONG).show();
                resolveSignInError();
            }
        });
        findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                mGoogleApiClient = null; //kill old instance
                initGooglePlus(); //make new client
                //delete data
                GoogleApiUtil.setUserLoggedIn(LoginActivity.this, false);
                GoogleApiUtil.setUserEmail(LoginActivity.this, "");
                GoogleApiUtil.setUserNickname(LoginActivity.this, "");
                //
                findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
                findViewById(R.id.sign_out_button).setVisibility(View.GONE);
                findViewById(R.id.view_achievements).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.view_achievements).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleApiUtil.showAchievements(LoginActivity.this, mGoogleApiClient);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGoogleApiClient.connect();
    }

    private void resolveSignInError() {
        if (mSignInIntent != null) {
            //Start intent to let user login
            try {
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            //Show Error Dialog
            if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(
                        mSignInError,
                        this,
                        RC_SIGN_IN,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Log.e(LoginActivity.this.getString(R.string.app_name), "Google Play services resolution cancelled");
                                mSignInProgress = STATE_DEFAULT;
                            }
                        });
                d.show();
            } else {
                Toast.makeText(LoginActivity.this, R.string.google_play_games_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        String currentUserEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
        GoogleApiUtil.setUserEmail(this, currentUserEmail);
        //Toast.makeText(LoginActivity.this,"Logged In", Toast.LENGTH_SHORT).show();
        //Remove login button, as already logged in.
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        findViewById(R.id.view_achievements).setVisibility(View.VISIBLE);
        //Unlock the first Achievement
        Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_ready_to_go));
        //Sync Favorites from Cloud
        Util.syncData(this, Util.SYNCFLAG_DOWNLOAD_FAVORITES);
        //Sync QRS found from other device maybe :P
        Util.syncData(this, Util.SYNCFLAG_DOWNLOAD_QRFOUND);
        //Send to server
        if (!GoogleApiUtil.isUserLoggedIn(this)) {
            LoggedInTask task = new LoggedInTask(this, currentUserEmail, currentUser, GcmUtil.gcmGetRegId(this));            
            task.execute();
        }
        //Now, set logged in and stuff.
        GoogleApiUtil.setUserLoggedIn(this, true);
    }

    /**
     * @param currentUser
     * @deprecated
     */
    @Deprecated
    public void askUserForNickname(final Person currentUser) {
        if (GoogleApiUtil.isUserLoggedIn(this)
                || !"".equals(GoogleApiUtil.getUserNickname(this))) {
            //Is already logged in, no need to ask for username
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.ask_nickname);
        final EditText ed = new EditText(this);
        ed.setHint(R.string.ask_nickname);
        b.setView(ed);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nickname = ed.getText().toString();
                if (nickname == null || "".equals(nickname.trim())) {
                    //so you left the field blank >.>
                    //Google+ nickname it is
                    nickname = currentUser.getDisplayName();
                    if (nickname == null || "".equals(nickname.trim())) {
                        //so you have an incomplete Google+ profile >.>
                        //Emailadres it is
                        nickname = GoogleApiUtil.getUserEmail(LoginActivity.this);
                    }
                }
                GoogleApiUtil.setUserNickname(LoginActivity.this, nickname);
            }
        });
        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //no nickname? we use your name from Google Plus                                
                String nickname = currentUser.getDisplayName();
                if (nickname == null || "".equals(nickname.trim())) {
                    //so you have an incomplete Google+ profile >.>
                    //Emailadres it is
                    nickname = GoogleApiUtil.getUserEmail(LoginActivity.this);
                }
                GoogleApiUtil.setUserNickname(LoginActivity.this, nickname);
            }
        });
        b.create().show();
    }

    public static void askUserForNickname(final Context c, final String defaultNickname) {
        if (!"".equals(GoogleApiUtil.getUserNickname(c))) {
            //Is already logged in, no need to ask for username
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(R.string.ask_nickname);
        final EditText ed = new EditText(c);
        ed.setHint(R.string.ask_nickname);
        ed.setText(defaultNickname);
        b.setView(ed);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nickname = ed.getText().toString();
                if (nickname == null || "".equals(nickname.trim())) {
                    //so you left the field blank >.>
                    //default nickname it is
                    nickname = defaultNickname;
                    if (nickname == null || "".equals(nickname.trim())) {
                        //Emailadres it is
                        nickname = GoogleApiUtil.getUserEmail(c);
                    }
                } else if (!nickname.equals(defaultNickname)) {
                    //User has given a new nickname, send to server
                    ChangeNicknameTask task = new ChangeNicknameTask(GoogleApiUtil.getUserEmail(c),nickname,GcmUtil.gcmGetRegId(c));
                    task.execute();
                }
                GoogleApiUtil.setUserNickname(c, nickname);
            }
        });
        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //no nickname? we use your name from Google Plus                                
                String nickname = defaultNickname;
                if (nickname == null || "".equals(nickname.trim())) {
                    //Emailadres it is
                    nickname = GoogleApiUtil.getUserEmail(c);
                }
                GoogleApiUtil.setUserNickname(c, nickname);
            }
        });
        b.create().show();
    }


    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Google Play Services: Connection Suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Toast.makeText(this,"Google Play Services: Connection Failed", Toast.LENGTH_LONG).show();
        //this can happen when user is not logged in yet, so don't display error message in that case.
        mSignInIntent = connectionResult.getResolution();
    }

    public static class ChangeNicknameTask extends AsyncTask<Void, Void, Void> {

        String email;
        String nickname;
        String regId;

        public ChangeNicknameTask(String email, String nickname, String regId) {
            this.email = email;
            this.nickname = nickname;
            this.regId = regId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String url = "http://wofje.8s.nl/hwcon/api/v1/changenickname.php?useremail=" + email + "&regId=" + regId + "&nickname=" + nickname;
            url = url.replace(" ","+");
            try {
                Util.httpDownload(url);
            } catch (Exception e) {
                ACRA.getErrorReporter().putCustomData("url",url);
                ACRA.getErrorReporter().handleException(e);
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class LoggedInTask extends AsyncTask<Void, Void, String> {

        Context context;
        String email;
        Person user;
        String regId;
        Dialog d;

        public LoggedInTask(Context context, String email, Person user, String regId) {
            this.context = context;
            this.email = email;
            this.user = user;
            this.regId = regId;
        }

        @Override
        protected void onPreExecute() {
            d = ProgressDialog.show(context, context.getString(R.string.logging_in), context.getString(R.string.logging_in));
        }

        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            String url = "http://wofje.8s.nl/hwcon/api/v1/applogin.php?useremail=" + email + "&regId=" + regId + "&gplusname=" + user.getDisplayName();
            url = url.replace(" ","+");
            try {
                response = Util.httpDownload(url);
                response = response.trim();
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().putCustomData("url",url);
                ACRA.getErrorReporter().handleException(e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                d.dismiss();
            } catch (Exception e) {
                //ignore.                   
            }
            if (response == null){
                response = "";                
            }
            askUserForNickname(context, response);
        }
    }
}