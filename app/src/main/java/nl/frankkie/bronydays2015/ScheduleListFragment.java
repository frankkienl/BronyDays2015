package nl.frankkie.bronydays2015;

import android.app.Activity;
import android.app.usage.UsageEvents;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import nl.frankkie.bronydays2015.data.EventContract;

/**
 * A list fragment representing a list of Events. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link EventDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ScheduleListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    ScheduleListAdapter mScheduleListAdapter;
    int SCHEDULE_LOADER = 0;
    ListView mListView;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = GridView.INVALID_POSITION;

    public static final int COL_ID = 0;
    public static final int COL_TITLE = 1;
    public static final int COL_TIME = 2;
    public static final int COL_IMAGE = 3;
    public static final int COL_COLOR = 4;
    public static final int COL_LOCATION = 5;
    public static final int COL_FAVORITE_ID = 6;

    public static final String[] SCHEDULE_COLUMNS = {
            EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry._ID,
            EventContract.EventEntry.COLUMN_NAME_TITLE,
            EventContract.EventEntry.COLUMN_NAME_START_TIME,
            EventContract.EventEntry.COLUMN_NAME_IMAGE,
            EventContract.EventEntry.COLUMN_NAME_COLOR,
            EventContract.LocationEntry.COLUMN_NAME_NAME,
            EventContract.FavoritesEntry.TABLE_NAME + "." + EventContract.FavoritesEntry._ID
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = EventContract.EventEntry.CONTENT_URI;
        //Uri uri = Uri.parse("content://nl.frankkie.bronydays2015/favorites/event");
        String sortOrder = EventContract.EventEntry.COLUMN_NAME_START_TIME + " ASC";
        CursorLoader cl = new CursorLoader(getActivity(), uri, SCHEDULE_COLUMNS, null, null, sortOrder);
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mScheduleListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mScheduleListAdapter.swapCursor(null);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id, String shareTitle);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ScheduleListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule_list, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mScheduleListAdapter = new ScheduleListAdapter(getActivity(), null, 0);

        mListView = getListView();
        //mListView = (ListView) inflater.inflate(R.layout.fragment_schedule_list, container, false);
        mListView.setAdapter(mScheduleListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mScheduleListAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    mCallbacks.onItemSelected("" + cursor.getLong(COL_ID), cursor.getString(1));
                }
                setActivatedPosition(position);
            }
        });

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(SCHEDULE_LOADER, null, this);
        mCallbacks = (Callbacks)getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().restartLoader(SCHEDULE_LOADER, null, this);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id, String shareTitle) {
        }
    };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != GridView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    boolean mTwoPane = false;

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        mListView.setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);

        mTwoPane = activateOnItemClick;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            mListView.setItemChecked(mActivatedPosition, false);
        } else {
            mListView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
