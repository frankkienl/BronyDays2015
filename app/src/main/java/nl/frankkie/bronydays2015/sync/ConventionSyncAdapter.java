package nl.frankkie.bronydays2015.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import org.acra.ACRA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.frankkie.bronydays2015.data.EventContract;
import nl.frankkie.bronydays2015.util.Util;

/**
 * Created by FrankkieNL on 6-12-2014.
 */
public class ConventionSyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;

    public ConventionSyncAdapter(Context c, boolean autoInit) {
        super(c, autoInit);
        mContentResolver = c.getContentResolver();
    }

    public ConventionSyncAdapter(Context c, boolean autoInit, boolean allowParallel) {
        super(c, autoInit, allowParallel);
        mContentResolver = c.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //Placed it (download and parsing) into separate methods, because the IDE complained this method was too complex.
        Log.d("Convention", "SyncAdapter: onPerformSync");
        int syncFlags = extras.getInt("syncflags", Util.SYNCFLAG_CONVENTION_DATA);
        //http://stackoverflow.com/questions/6067411/checking-flag-bits-java
        if ((syncFlags & Util.SYNCFLAG_CONVENTION_DATA) == Util.SYNCFLAG_CONVENTION_DATA) {
            //CHANGE THIS URL WHEN USING FOR OTHER CONVENTION
            String json = Util.httpDownload("http://wofje.8s.nl/bronydays2015/api/v1/downloadconventiondata.php");
            if (json != null) {
                parseConventionDataJSON(json);
            }
        }
    }

    public void parseConventionDataJSON(String json) {
        //<editor-fold desc="boring json parsing and DB inserting code">
        try {
            JSONObject data = new JSONObject(json).getJSONObject("data");

            //<editor-fold desc="events">
            JSONArray events = data.getJSONArray("events");
            ContentValues[] eventCVs = new ContentValues[events.length()];
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.EventEntry._ID, event.getInt("_id"));
                values.put(EventContract.EventEntry.COLUMN_NAME_TITLE, event.getString("title"));
                values.put(EventContract.EventEntry.COLUMN_NAME_TITLE_FR, event.getString("title_fr"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION, event.getString("description"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION_FR, event.getString("description_fr"));
                values.put(EventContract.EventEntry.COLUMN_NAME_KEYWORDS, event.getString("keywords"));
                values.put(EventContract.EventEntry.COLUMN_NAME_IMAGE, event.getString("image"));
                values.put(EventContract.EventEntry.COLUMN_NAME_COLOR, event.getString("color"));
                values.put(EventContract.EventEntry.COLUMN_NAME_START_TIME, event.getString("start_time"));
                values.put(EventContract.EventEntry.COLUMN_NAME_END_TIME, event.getString("end_time"));
                values.put(EventContract.EventEntry.COLUMN_NAME_LOCATION_ID, event.getInt("location_id"));
                values.put(EventContract.EventEntry.COLUMN_NAME_SORT_ORDER, event.getInt("sort_order"));
                eventCVs[i] = values;
            }

            //Delete old values
            getContext().getContentResolver().delete(EventContract.EventEntry.CONTENT_URI, null, null); //null deletes all rows
            //Insert new ones
            getContext().getContentResolver().bulkInsert(EventContract.EventEntry.CONTENT_URI, eventCVs);
            //Notify observers
            getContext().getContentResolver().notifyChange(EventContract.EventEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="speakers">
            JSONArray speakers = data.getJSONArray("speakers");
            ContentValues[] speakerCVs = new ContentValues[speakers.length()];
            for (int i = 0; i < speakers.length(); i++) {
                JSONObject speaker = speakers.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.SpeakerEntry._ID, speaker.getInt("_id"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_NAME, speaker.getString("name"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_NAME_FR, speaker.getString("name_fr"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION, speaker.getString("description"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION_FR, speaker.getString("description_fr"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_IMAGE, speaker.getString("image"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_COLOR, speaker.getString("color"));
                speakerCVs[i] = values;
            }
            getContext().getContentResolver().delete(EventContract.SpeakerEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.SpeakerEntry.CONTENT_URI, speakerCVs);
            getContext().getContentResolver().notifyChange(EventContract.SpeakerEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="locations">
            JSONArray locations = data.getJSONArray("locations");
            ContentValues[] locationCVs = new ContentValues[locations.length()];
            for (int i = 0; i < locations.length(); i++) {
                JSONObject location = locations.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(EventContract.LocationEntry._ID, location.getInt("_id"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_NAME, location.getString("name"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_NAME_FR, location.getString("name_fr"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION, location.getString("description"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION_FR, location.getString("description_fr"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_MAP_LOCATION, location.getString("map_location"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_FLOOR, location.getInt("floor"));
                locationCVs[i] = values;
            }
            getContext().getContentResolver().delete(EventContract.LocationEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.LocationEntry.CONTENT_URI, locationCVs);
            getContext().getContentResolver().notifyChange(EventContract.LocationEntry.CONTENT_URI, null);
            //</editor-fold>

            //<editor-fold desc="speakers in events">
            JSONArray speakersInEvents = data.getJSONArray("speakers_in_events");
            ContentValues[] sieCVs = new ContentValues[speakersInEvents.length()];
            for (int i = 0; i < speakersInEvents.length(); i++) {
                JSONObject sie = speakersInEvents.getJSONObject(i);
                ContentValues sieCV = new ContentValues();
                sieCV.put(EventContract.SpeakersInEventsEntry._ID, sie.getInt("_id"));
                sieCV.put(EventContract.SpeakersInEventsEntry.COLUMN_NAME_EVENT_ID, sie.getInt("event_id"));
                sieCV.put(EventContract.SpeakersInEventsEntry.COLUMN_NAME_SPEAKER_ID, sie.getInt("speaker_id"));
                sieCVs[i] = sieCV;
            }
            getContext().getContentResolver().delete(EventContract.SpeakersInEventsEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.SpeakersInEventsEntry.CONTENT_URI, sieCVs);
            getContext().getContentResolver().notifyChange(EventContract.SpeakersInEventsEntry.CONTENT_URI, null);
            //</editor-fold>
        } catch (JSONException e) {
            Log.e("Convention", "Error in SyncAdapter.onPerformSync, ConventionData JSON ", e);
            ACRA.getErrorReporter().handleException(e);
        }
        //</editor-fold>
    }
}
