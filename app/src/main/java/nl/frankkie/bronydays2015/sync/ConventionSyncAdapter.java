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

import java.io.IOException;

import nl.frankkie.bronydays2015.data.EventContract;
import nl.frankkie.bronydays2015.util.GcmUtil;
import nl.frankkie.bronydays2015.util.GoogleApiUtil;
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
            String regId = GcmUtil.gcmGetRegId(getContext());
            //CHANGE THIS URL WHEN USING FOR OTHER CONVENTION
            String json = Util.httpDownload("http://wofje.8s.nl/bronydays2015/api/v1/downloadconventiondata.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseConventionDataJSON(json);
            }
        }
        if ((syncFlags & Util.SYNCFLAG_DOWNLOAD_FAVORITES) == Util.SYNCFLAG_DOWNLOAD_FAVORITES) {
            String regId = GcmUtil.gcmGetRegId(getContext());
            //With username "&username=", for syncing between devices of same user
            String json = Util.httpDownload("http://wofje.8s.nl/bronydays2015/api/v1/downloadfavorites.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseFavoritesDataJson(json);
            }
        }
        if ((syncFlags & Util.SYNCFLAG_UPLOAD_FAVORITES) == Util.SYNCFLAG_UPLOAD_FAVORITES) {
            //this is for uploading all the favorites. For a delta, use Asynctask instead. See Util.
            try {
                Cursor cursor = getContext().getContentResolver().query(EventContract.FavoritesEntry.CONTENT_URI,
                        new String[]{EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID},
                        EventContract.FavoritesEntry.COLUMN_NAME_TYPE + " = 'event'", null, null);
                if (cursor.getCount() != 0) {
                    //Only do this when there is data to be send
                    JSONObject root = new JSONObject();
                    JSONArray events = new JSONArray();
                    cursor.moveToFirst();
                    do {
                        events.put(cursor.getString(0));
                    } while (cursor.moveToNext());
                    cursor.close();
                    root.put("events", events);
                    JSONObject device = new JSONObject();
                    device.put("regId", GcmUtil.gcmGetRegId(getContext()));
                    device.put("username", GoogleApiUtil.getUserEmail(getContext()));
                    device.put("nickname", GoogleApiUtil.getUserNickname(getContext()));
                    root.put("device", device);
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("data", root);
                    String json = wrapper.toString();
                    String postData = "json=" + json;
                    ////////////////////////////
                    String response = Util.httpPost(getContext(), "http://wofje.8s.nl/bronydays2015/api/v1/uploadfavorites.php", postData);
                    if (!"ok".equals(response.trim())) {
                        //There muse be something wrong
                        Util.sendACRAReport("Server did not send 'ok', Favorites", "http://wofje.8s.nl/bronydays2015/api/v1/uploadfavorites.php", postData + "\n" + response);
                    }
                }
                /////////////////////////////
            } catch (Exception e) {
                ACRA.getErrorReporter().handleException(e);
                e.printStackTrace();
            }
        }
        ///
        if ((syncFlags & Util.SYNCFLAG_UPLOAD_QRFOUND) == Util.SYNCFLAG_UPLOAD_QRFOUND) {
            try {
                Cursor cursor = getContext().getContentResolver().query(
                        EventContract.QrFoundEntry.CONTENT_URI, //table
                        new String[]{EventContract.QrFoundEntry.COLUMN_NAME_QR_ID, //projection
                                EventContract.QrFoundEntry.COLUMN_NAME_TIME},
                        null, //selection
                        null, //selectionArgs
                        null //sort-order
                );
                if (cursor.getCount() != 0) {
                    JSONObject root = new JSONObject();
                    JSONArray qrsfound = new JSONArray();
                    cursor.moveToFirst();
                    do {
                        JSONObject qrfound = new JSONObject();
                        qrfound.put("qr_id", cursor.getString(0));
                        long unixTimestamp = Long.parseLong(cursor.getString(1));                         
                        qrfound.put("found_time", unixTimestamp);
                        qrsfound.put(qrfound);
                    } while (cursor.moveToNext());
                    cursor.close();
                    root.put("qrsfound", qrsfound);
                    JSONObject device = new JSONObject();
                    device.put("regId", GcmUtil.gcmGetRegId(getContext()));
                    device.put("username", GoogleApiUtil.getUserEmail(getContext()));
                    device.put("nickname", GoogleApiUtil.getUserNickname(getContext()));                            
                    root.put("device", device);
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("data", root);
                    String json = wrapper.toString();
                    String postData = "json=" + json;
                    ////////////////////////////
                    String response = Util.httpPost(getContext(), "http://wofje.8s.nl/bronydays2015/api/v1/uploadqrfound.php", postData);
                    if (!"ok".equals(response.trim())) {
                        //There muse be something wrong
                        Util.sendACRAReport("Server did not send 'ok', QRs", "http://wofje.8s.nl/bronydays2015/api/v1/uploadqrfound.php", postData + "\n" + response);
                    }
                }
            } catch (Exception e) {
                ACRA.getErrorReporter().handleException(e);
                e.printStackTrace();
            }
        }
        ////
        if ((syncFlags & Util.SYNCFLAG_DOWNLOAD_QRFOUND) == Util.SYNCFLAG_DOWNLOAD_QRFOUND) {
            String regId = GcmUtil.gcmGetRegId(getContext());
            String json = Util.httpDownload("http://wofje.8s.nl/bronydays2015/api/v1/downloadqrfound.php?regId=" + regId + "&username=" + GoogleApiUtil.getUserEmail(getContext()));
            if (json != null) {
                parseQrFoundDataJson(json);
            }
        }
    }

    public void parseQrFoundDataJson(String json) {
        try {
            JSONObject data = new JSONObject(json).getJSONObject("data");
            JSONArray qrsfound = data.getJSONArray("qrsfound");
            ContentValues[] qrfCVs = new ContentValues[qrsfound.length()];
            for (int i = 0; i < qrsfound.length(); i++) {
                JSONObject qrf = qrsfound.getJSONObject(i);
                ContentValues qrCV = new ContentValues();
                qrCV.put(EventContract.QrFoundEntry.COLUMN_NAME_QR_ID, qrf.getString("qr_id"));
                qrCV.put(EventContract.QrFoundEntry.COLUMN_NAME_TIME, qrf.getString("found_time"));
                qrfCVs[i] = qrCV;
            }

            //Delete old values
            getContext().getContentResolver().delete(EventContract.QrFoundEntry.CONTENT_URI, null, null); //null deletes all rows
            //Insert new ones
            getContext().getContentResolver().bulkInsert(EventContract.QrFoundEntry.CONTENT_URI, qrfCVs);
            //Notify observers
            getContext().getContentResolver().notifyChange(EventContract.QrFoundEntry.CONTENT_URI, null);
            //Notify QR list.
            getContext().getContentResolver().notifyChange(EventContract.QrEntry.CONTENT_URI, null);
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
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
                values.put(EventContract.EventEntry.COLUMN_NAME_TITLE_NL, event.getString("title_nl"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION, event.getString("description"));
                values.put(EventContract.EventEntry.COLUMN_NAME_DESCRIPTION_NL, event.getString("description_nl"));
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
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_NAME_NL, speaker.getString("name_nl"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION, speaker.getString("description"));
                values.put(EventContract.SpeakerEntry.COLUMN_NAME_DESCRIPTION_NL, speaker.getString("description_nl"));
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
                values.put(EventContract.LocationEntry.COLUMN_NAME_NAME_NL, location.getString("name_nl"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION, location.getString("description"));
                values.put(EventContract.LocationEntry.COLUMN_NAME_DESCRIPTION_NL, location.getString("description_nl"));
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

            //<editor-fold desc="qr">
            //QR is a separate table, to keep the userdata (found or not) separate from the convention-data
            if (data.has("qr")) {
                JSONArray qrs = data.getJSONArray("qr");
                ContentValues[] qrCVs = new ContentValues[qrs.length()];
                for (int i = 0; i < qrs.length(); i++) {
                    JSONObject qr = qrs.getJSONObject(i);
                    ContentValues qrCV = new ContentValues();
                    qrCV.put(EventContract.QrEntry._ID, qr.getInt("_id"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_HASH, qr.getString("hash"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_NAME, qr.getString("name"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_NAME_NL, qr.getString("name_nl"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_DESCRIPTION, qr.getString("description"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_DESCRIPTION_NL, qr.getString("description_nl"));
                    qrCV.put(EventContract.QrEntry.COLUMN_NAME_IMAGE, qr.getString("image"));
                    qrCVs[i] = qrCV;
                }
                getContext().getContentResolver().delete(EventContract.QrEntry.CONTENT_URI, null, null);
                getContext().getContentResolver().bulkInsert(EventContract.QrEntry.CONTENT_URI, qrCVs);
                getContext().getContentResolver().notifyChange(EventContract.QrEntry.CONTENT_URI, null);
            }
            //</editor-fold>
        } catch (JSONException e) {
            Log.e("Convention", "Error in SyncAdapter.onPerformSync, ConventionData JSON ", e);
            ACRA.getErrorReporter().handleException(e);
        }
        //</editor-fold>
    }

    public void parseFavoritesDataJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject data = root.getJSONObject("data");
            JSONArray events = data.getJSONArray("events");
            ContentValues[] eCVs = new ContentValues[events.length()];
            for (int i = 0; i < events.length(); i++) {
                int id = Integer.parseInt(events.getString(i));
                ContentValues eCV = new ContentValues();
                eCV.put(EventContract.FavoritesEntry.COLUMN_NAME_TYPE, EventContract.FavoritesEntry.TYPE_EVENT);
                eCV.put(EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID, id);
                eCVs[i] = eCV;
            }
            getContext().getContentResolver().delete(EventContract.FavoritesEntry.CONTENT_URI, null, null);
            getContext().getContentResolver().bulkInsert(EventContract.FavoritesEntry.CONTENT_URI, eCVs);
            getContext().getContentResolver().notifyChange(EventContract.FavoritesEntry.CONTENT_URI, null);
            //TODO add code to sync other types of favorites.
        } catch (JSONException e) {
            Log.e("Convention", "Error in SyncAdapter.onPerformSync, ConventionData JSON ", e);
            ACRA.getErrorReporter().handleException(e);
        }
    }
}
