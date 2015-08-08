package nl.frankkie.bronydays2015;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.koushikdutta.ion.Ion;

import nl.frankkie.bronydays2015.data.EventContract;
import nl.frankkie.bronydays2015.util.GoogleApiUtil;
import nl.frankkie.bronydays2015.util.Util;

/**
 * A fragment representing a single Event detail screen.
 * This fragment is either contained in a {@link nl.frankkie.bronydays2015.EventListActivity} or {@link nl.frankkie.bronydays2015.ScheduleActivity}
 * in two-pane mode (on tablets) or a {@link nl.frankkie.bronydays2015.EventDetailActivity}
 * on handsets.
 */
public class EventDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EVENT_DETAIL_LOADER = 0;
    public static final int EVENT_SPEAKERS_LOADER = 1;
    public static final String[] EVENT_COLUMNS = {
            EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry._ID,
            EventContract.EventEntry.COLUMN_NAME_TITLE,
            EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry.COLUMN_NAME_DESCRIPTION,
            EventContract.EventEntry.COLUMN_NAME_KEYWORDS,
            EventContract.EventEntry.COLUMN_NAME_START_TIME,
            EventContract.EventEntry.COLUMN_NAME_END_TIME,
            EventContract.EventEntry.COLUMN_NAME_COLOR,
            EventContract.EventEntry.COLUMN_NAME_IMAGE,
            EventContract.LocationEntry.TABLE_NAME + "." + EventContract.LocationEntry._ID,
            EventContract.LocationEntry.COLUMN_NAME_NAME,
            EventContract.LocationEntry.TABLE_NAME + "." + EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION,
            EventContract.LocationEntry.COLUMN_NAME_FLOOR,
            EventContract.FavoritesEntry.TABLE_NAME + "." + EventContract.FavoritesEntry._ID //If filled, its starred.
    };
    public static final String[] SPEAKERS_COLUMNS = {
            EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry._ID,
            EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry.COLUMN_NAME_NAME,
            EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION,
            EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry.COLUMN_NAME_COLOR,
            EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry.COLUMN_NAME_IMAGE
    };

    String mId;
    String mShareTitle; //title to share via ShareActionProvider.
    ShareActionProvider mShareActionProvider;
    //Views
    TextView mTitle;
    TextView mDescription;
    TextView mKeywords;
    TextView mStartTime;
    TextView mEndTime;
    TextView mLocation;
    TextView mLocationDescription;
    TextView mLocationFloor;
    ImageView mImage;
    TextView mSpeakersHeader;
    LinearLayout mSpeakersContainer;
    MenuItem mStarMenuItem;
    //workaround for when onLoadFinished is called before actionbar is loaded
    boolean isLoadingFinished = false;
    boolean isLoadingFinished_isFavorite = false;
    private GoogleApiUtil.GiveMeGoogleApiClient apiClientGetter;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == EVENT_DETAIL_LOADER) {
            Uri uri = EventContract.EventEntry.buildEventUri(Long.parseLong(mId));
            //Is sortOrder needed? We'll get only 1 row.
            String sortOrder = EventContract.EventEntry.COLUMN_NAME_START_TIME + " ASC";
            CursorLoader cl = new CursorLoader(getActivity(), uri, EVENT_COLUMNS, null, null, sortOrder);
            return cl;
        } else if (i == EVENT_SPEAKERS_LOADER) {
            Uri uri = EventContract.SpeakersInEventsEntry.buildSpeakersInEventUri(Long.parseLong(mId));
            String sortOrder = "";
            CursorLoader cl = new CursorLoader(getActivity(), uri, SPEAKERS_COLUMNS, null, null, sortOrder);
            return cl;
        }
        return null;
    }

    public String getFloorName(int floor) {
        switch (floor) {
            case 0:
                return getString(R.string.map_floor0);
            case 1:
                return getString(R.string.map_floor1);
            case 2:
                return getString(R.string.map_floor2);
            default:
                return "";
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        //set View-content
        if (cursorLoader.getId() == EVENT_DETAIL_LOADER) {
            if (data != null && data.moveToFirst()) {
                int eventId = data.getInt(0);
                mTitle.setText(data.getString(1));
                String descriptionString = data.getString(2);
                mDescription.setText(Html.fromHtml(descriptionString));
                mKeywords.setText(data.getString(3));
                mStartTime.setText(Util.getDataTimeString(data.getLong(4)));
                mEndTime.setText(Util.getDataTimeString(data.getLong(5)));
                String color = data.getString(6);
                String image = data.getString(7);
                Ion.with(this)
                        .load(image)
                        .withBitmap()
                        .error(R.drawable.transparentpixel)
                        .placeholder(R.drawable.transparentpixel)
                        .intoImageView(mImage);
                int locationId = data.getInt(8);
                mLocation.setText(data.getString(9));
                mLocationDescription.setText(data.getString(10));
                int floor = data.getInt(11);
                mLocationFloor.setText(getFloorName(floor));
                //Star
                handleStar(!data.isNull(12));
            }
        } else if (cursorLoader.getId() == EVENT_SPEAKERS_LOADER) {
            //List of speakers of this Event.
            if (data == null || data.getCount() < 1) { //.getCount gives number of rows
                //There are no rows, return.
                return;
            }
            //There are speakers, make Speaker header visible
            mSpeakersHeader.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            mSpeakersContainer.removeAllViews(); //clear content
            while (data.moveToNext()) {
                ViewGroup speakerItem = (ViewGroup) inflater.inflate(R.layout.event_detail_speaker_item, mSpeakersContainer, false);
                TextView sName = (TextView) speakerItem.findViewById(R.id.event_detail_speaker_item_name);
                TextView sDescription = (TextView) speakerItem.findViewById(R.id.event_detail_speaker_item_description);
                ImageView sImageView = (ImageView) speakerItem.findViewById(R.id.event_detail_speaker_item_image);
                sName.setText(data.getString(1));
                String descriptionString = data.getString(2);
                //descriptionString = descriptionString.replace("\\n","\n");
                sDescription.setText(Html.fromHtml(descriptionString));
                String sImageUrl = data.getString(4);
                mSpeakersContainer.addView(speakerItem);

                //Load image
                Ion.with(this)
                        .load(sImageUrl)
                        .withBitmap()
                        .error(R.drawable.ic_launcher2)
                        .placeholder(R.drawable.ic_launcher2)
                        .intoImageView(sImageView);
            }
        }
    }

    public void handleStar(boolean isFavorite) {
        //workaround for when onLoadFinished is called before actionbar is loaded
        isLoadingFinished = true;
        isLoadingFinished_isFavorite = isFavorite;
        if (mStarMenuItem == null) {
            return;
        }
        //endof workaround
        mStarMenuItem.setVisible(true);
        if (isFavorite) { //column in database is null when not starred.
            mStarMenuItem.setChecked(true);
            mStarMenuItem.setTitle(R.string.action_star_remove);
            mStarMenuItem.setIcon(android.R.drawable.btn_star_big_on);
            //Google Play Games
            try {
                Games.Achievements.increment(apiClientGetter.getGoogleApiClient(), getActivity().getString(R.string.achievement_personal_schedule), 1);
            } catch(IllegalStateException e){
                //This happens when GoogleApiClient is not connected yet.
                //Ignore this error, user gets Achievement next time.
            }
        } else {
            mStarMenuItem.setChecked(false);
            mStarMenuItem.setTitle(R.string.action_star_add);
            mStarMenuItem.setIcon(android.R.drawable.btn_star_big_off);
        }
        persistFavorite(isFavorite);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        //do nothing
    }

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_SHARETITLE = "item_sharetitle";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventDetailFragment() {
        setHasOptionsMenu(true); //for ShareActionProvider
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.eventdetailfragment, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        mStarMenuItem = menu.findItem(R.id.action_star);
        mStarMenuItem.setVisible(false); //set invisible here, make visible when data is loaded.
        mStarMenuItem.setCheckable(true);
        /*
        Apparently, this method sometimes get called AFTER onLoaderFinished.
        This give problems with setting the star, because its still null at that point.
        This is a workaround
        When loading is finished, the value will be saved in isLoadingFinished_isFavorite
        So we can set the correct value, now, when the menu is made.
        */
        if (isLoadingFinished) {
            handleStar(isLoadingFinished_isFavorite);
            //
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_star) {
            handleStar(!item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public Intent createShareIntent() {
        //Like in Sunshine
        Intent i = new Intent(Intent.ACTION_SEND);
        //We should use FLAG_ACTIVITY_NEW_DOCUMENT instead, but it has the same value (0x00080000)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_text), mShareTitle));
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            //Set the id, that will be used in onCreateLoader (CursorLoader)
            mId = getArguments().getString(ARG_ITEM_ID);
        }
        if (getArguments().containsKey(ARG_ITEM_SHARETITLE)) {
            mShareTitle = getArguments().getString(ARG_ITEM_SHARETITLE);
        }

        apiClientGetter = (GoogleApiUtil.GiveMeGoogleApiClient) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_detail, container, false);

        //Should I do this in a ViewHolder? Nah, its not a ListView item.
        mTitle = (TextView) rootView.findViewById(R.id.event_detail_title);
        mDescription = (TextView) rootView.findViewById(R.id.event_detail_description);
        mKeywords = (TextView) rootView.findViewById(R.id.event_detail_keywords);
        mStartTime = (TextView) rootView.findViewById(R.id.event_detail_starttime);
        mEndTime = (TextView) rootView.findViewById(R.id.event_detail_endtime);
        mLocation = (TextView) rootView.findViewById(R.id.event_detail_location);
        mLocationDescription = (TextView) rootView.findViewById(R.id.event_detail_location_description);
        mLocationFloor = (TextView) rootView.findViewById(R.id.event_detail_location_floor);
        mImage = (ImageView) rootView.findViewById(R.id.event_detail_image);
        mSpeakersHeader = (TextView) rootView.findViewById(R.id.event_detail_label_speakers);
        mSpeakersHeader.setVisibility(View.GONE); //Make visible (again) if there are Speakers for this event.
        mSpeakersContainer = (LinearLayout) rootView.findViewById(R.id.event_detail_speakers_container);
        //
        showStarTip();
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Loaders depend on Activity not on Fragment!
        getLoaderManager().initLoader(EVENT_DETAIL_LOADER, null, this);
        getLoaderManager().initLoader(EVENT_SPEAKERS_LOADER, null, this);
    }

    public void showStarTip() {
        //Show Star-tip
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean shownStarTip = prefs.getBoolean("prefs_shown_star_tip", false);
        if (!shownStarTip) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.star_tip_title);
            builder.setMessage(R.string.star_tip_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Set to shown=true, when user presses the OK-button.
                    prefs.edit().putBoolean("prefs_shown_star_tip", true).apply();
                    //Using apply (instead of commit), because we don't want to stall the UI-thread.
                    //apply will make the change in memory, and then save it to persistent story
                    //on a background thread.
                }
            });
            builder.create().show();
        }
    }

    public void persistFavorite(boolean checked) {
        if (checked) {
            //If checked, add a row to DB
            ContentValues cv = new ContentValues();
            //cv.put(EventContract.FavoritesEntry._ID,null);
            //giving null or no ID at all will generate an ID.
            //We do not need to know what the ID of the row will be.
            cv.put(EventContract.FavoritesEntry.COLUMN_NAME_TYPE, EventContract.FavoritesEntry.TYPE_EVENT);
            cv.put(EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID, mId); //Id of this Event
            getActivity().getContentResolver().insert(EventContract.FavoritesEntry.CONTENT_URI, cv);
            //I made favorites.type && favorites.item_id UNIQUE, to prevent duplicates.
        } else {
            //If not checked, remove row from DB
            //We use EventID in where-clause, hence, we don't need to know the RowID of the Favorite.
            getActivity().getContentResolver().delete(EventContract.FavoritesEntry.CONTENT_URI,
                    EventContract.FavoritesEntry.COLUMN_NAME_TYPE + " = '" + EventContract.FavoritesEntry.TYPE_EVENT +
                            "' AND " + EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID + " = ?", new String[]{mId});
        }
        //sendToServer, after persisting in local database
        Util.sendFavoriteDelta(getActivity(), mId, checked);
    }
}