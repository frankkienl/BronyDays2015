package nl.frankkie.bronydays2015;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import org.acra.ACRA;

import nl.frankkie.bronydays2015.util.GcmUtil;
import nl.frankkie.bronydays2015.util.Util;


/**
 * An activity representing a list of Events. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link EventDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EventListFragment} and the item details
 * (if present) is a {@link EventDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link EventListFragment.Callbacks} interface
 * to listen for item selections.
 * <p/>
 * To use ActionBar and Fragments, extend from ActionBarActivity,
 * as ActionBarActivity supports Fragments.
 * http://stackoverflow.com/questions/18451575/action-bar-fragment-activity
 */
public class EventListActivity extends AppCompatActivity implements
        EventListFragment.Callbacks, 
        NavigationDrawerFragment.NavigationDrawerCallbacks {


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

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //See: R.values-sw600dp.refs
        setContentView(R.layout.activity_event_list);
        initToolbar();

        if (findViewById(R.id.event_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((EventListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.event_list))
                    .setActivateOnItemClick(true);
        }
        //Sync ContentProvider using SyncAdapter
        Util.syncConventionData(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Callback method from {@link EventListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id, String shareTitle) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(EventDetailFragment.ARG_ITEM_ID, id);
            arguments.putString(EventDetailFragment.ARG_ITEM_SHARETITLE, shareTitle);
            EventDetailFragment fragment = new EventDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.event_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, EventDetailActivity.class);
            detailIntent.putExtra(EventDetailFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(EventDetailFragment.ARG_ITEM_SHARETITLE, shareTitle);
            startActivity(detailIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Check for Google Play Service
        int flag = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (flag != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(flag)) {
                GooglePlayServicesUtil.showErrorNotification(flag, this);
            } else {
                Log.e(getString(R.string.app_name), "Google Play Services not supported.");
                ACRA.getErrorReporter().handleException(new RuntimeException("Google Play Services not supported."));
            }
        } else {
            //GCM is available!!
            String regId = GcmUtil.gcmGetRegId(this);
            //not using String.isEmpty, as this might not work on some older Android versions.
            //not sure from which version isEmpty is supported, not taking any chances here.
            if (regId == null || "".equals(regId)) {
                //not registered yet
                GcmUtil.gcmRegister(this);
            }
        }
    }
}
