package nl.frankkie.bronydays2015.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by FrankkieNL on 22-11-2014.
 */
public class EventProvider extends ContentProvider {

    /*
     * Event
     * content://nl.frankkie.bronydays2015/
     * event
     * event/speakers
     * speakers
     * location
     * favorites
     * qr
     */
    //content://nl.frankkie.bronydays2015/event/ (LIST)
    public static final int EVENT = 100;
    //content://nl.frankkie.bronydays2015/event/ID (ITEM)
    public static final int EVENT_ID = 101;
    //content://nl.frankkie.bronydays2015/speaker/ (LIST)
    public static final int SPEAKER = 200;
    //content://nl.frankkie.bronydays2015/speaker/ (ITEM)
    public static final int SPEAKER_ID = 201;
    //content://nl.frankkie.bronydays2015/location/ (LIST)
    public static final int LOCATION = 300;
    //content://nl.frankkie.bronydays2015/location/ID (ITEM)
    public static final int LOCATION_ID = 301;
    //content://nl.frankkie.bronydays2015/speakers_in_events/ (LIST)
    public static final int SPEAKERS_IN_EVENTS = 400;
    //content://nl.frankkie.bronydays2015/speakers_in_events/ID (ITEM)
    public static final int SPEAKERS_IN_EVENTS_ID = 401;
    //content://nl.frankkie.bronydays2015/speakers_in_events/event/ID (LIST of Speakers in Event)
    public static final int SPEAKERS_IN_EVENTS_EVENT_ID = 402;
    //content://nl.frankkie.bronydays2015/favorites/ (LIST of favorites of all types)
    public static final int FAVORITES = 500;
    //content://nl.frankkie.bronydays2015/favorites/event (LIST of favorites of event type)
    public static final int FAVORITES_EVENTS = 501;
    //content://nl.frankkie.bronydays2015/qr/ (LIST of QR codes to be found)
    public static final int QR = 600;
    //content://nl.frankkie.bronydays2015/qr/ID (ITEM)
    public static final int QR_ID = 601;
    //content://nl.frankkie.bronydays2015/qr/hash/ID (ITEM)
    public static final int QR_HASH = 602;
    //content://nl.frankkie.bronydays2015/qrfound/ (LIST of found codes, with times, without names and description)
    public static final int QR_FOUND = 700;
    //content://nl.frankkie.bronydays2015/qrfound/ID (ITEM, note: is found-id, not qr-id)
    public static final int QR_FOUND_ID = 701;
    //content://nl.frankkie.bronydays2015/qrfound/qr/ID (ITEM, using QR-id)
    public static final int QR_FOUND_QR_ID = 702;


    private static final UriMatcher sUriMatcher = buildUriMatcher();
    SQLiteOpenHelper mOpenHelper;

    private static final SQLiteQueryBuilder sEventsWithLocationAndFavoriteQueryBuilder;
    private static final SQLiteQueryBuilder sEventWithLocationQueryBuilder;
    private static final SQLiteQueryBuilder sSpeakersWithEventQueryBuilder;
    private static final SQLiteQueryBuilder sFavoriteEventsWithLocationQueryBuilder;
    private static final SQLiteQueryBuilder sQrWithFoundQueryBuilder;

    static {
        // Sunshine combines location with weather, this app will combine event and location
        sEventWithLocationQueryBuilder = new SQLiteQueryBuilder();
        // Lets hope 'JOIN' does what I think it does.
        // I should have paid better attention at Database-lessons at school... >.>
        //SELECT event.title, location.name FROM event JOIN location ON event.location_id = location._id LEFT JOIN favorites ON event._id = favorites.item_id WHERE event._id = 3
        sEventWithLocationQueryBuilder.setTables(
                EventContract.EventEntry.TABLE_NAME + " JOIN " +
                        EventContract.LocationEntry.TABLE_NAME +
                        " ON " + EventContract.EventEntry.TABLE_NAME +
                        "." + EventContract.EventEntry.COLUMN_NAME_LOCATION_ID +
                        " = " + EventContract.LocationEntry.TABLE_NAME +
                        "." + EventContract.LocationEntry._ID +
                        " LEFT JOIN " + EventContract.FavoritesEntry.TABLE_NAME +
                        " ON " + EventContract.EventEntry.TABLE_NAME +
                        "." + EventContract.EventEntry._ID +
                        " = " + EventContract.FavoritesEntry.TABLE_NAME +
                        "." + EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID
        );
        // Get a list of all Events, with Location and Favorite-status, to show in Schedule
        sEventsWithLocationAndFavoriteQueryBuilder = new SQLiteQueryBuilder();
        /*
        SELECT event.*, location.name as 'location_name', favorites._id as 'favorite_id'
        FROM event
        LEFT JOIN location ON event.location_id = location._id
        LEFT JOIN favorites ON event._id = favorites.item_id
         */
        //80 columns is too small for SQL in source-code. Deal with it.
        sEventsWithLocationAndFavoriteQueryBuilder.setTables(
                EventContract.EventEntry.TABLE_NAME
                        + " LEFT JOIN " + EventContract.LocationEntry.TABLE_NAME + " ON "
                        + EventContract.EventEntry.TABLE_NAME + "."
                        + EventContract.EventEntry.COLUMN_NAME_LOCATION_ID + " = "
                        + EventContract.LocationEntry.TABLE_NAME + "." + EventContract.LocationEntry._ID
                        + " LEFT JOIN " + EventContract.FavoritesEntry.TABLE_NAME + " ON "
                        + EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry._ID
                        + " = " + EventContract.FavoritesEntry.TABLE_NAME + "."
                        + EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID
        );
        // Get Speakers from Event
        sSpeakersWithEventQueryBuilder = new SQLiteQueryBuilder();
        sSpeakersWithEventQueryBuilder.setTables(
                EventContract.SpeakersInEventsEntry.TABLE_NAME + " INNER JOIN " +
                        EventContract.SpeakerEntry.TABLE_NAME + " ON " +
                        EventContract.SpeakersInEventsEntry.TABLE_NAME +
                        "." + EventContract.SpeakersInEventsEntry.COLUMN_NAME_SPEAKER_ID +
                        " = " + EventContract.SpeakerEntry.TABLE_NAME + "." + EventContract.SpeakerEntry._ID
        );
        //Get all favorite Events (with location)
        //SELECT event.title, event.start_time, location.name, favorites._id FROM event JOIN location ON event.location_id = location._id JOIN favorites ON favorites.item_id = event._id WHERE favorites.type = 'event'
        sFavoriteEventsWithLocationQueryBuilder = new SQLiteQueryBuilder();
        sFavoriteEventsWithLocationQueryBuilder.setTables(
                EventContract.EventEntry.TABLE_NAME + " JOIN " +
                        EventContract.LocationEntry.TABLE_NAME + " ON " +
                        EventContract.EventEntry.TABLE_NAME +
                        "." + EventContract.EventEntry.COLUMN_NAME_LOCATION_ID +
                        " = " + EventContract.LocationEntry.TABLE_NAME + "." +
                        EventContract.LocationEntry._ID + " JOIN " +
                        EventContract.FavoritesEntry.TABLE_NAME + " ON " +
                        EventContract.FavoritesEntry.TABLE_NAME + "." +
                        EventContract.FavoritesEntry.COLUMN_NAME_ITEM_ID + " = " +
                        EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry._ID
        );
        //Get all QR codes to be found, with the time_found column from QrFound.
        //SELECT qr._id, qr.hash, qr.name, qr.description, qr.image, qrfound.time_found FROM qr JOIN qrfound ON qrfound.qr_id = qr._id 
        sQrWithFoundQueryBuilder = new SQLiteQueryBuilder();
        sQrWithFoundQueryBuilder.setTables(
                EventContract.QrEntry.TABLE_NAME + " LEFT JOIN " + //Left Join, so just give an empty value when time_found == null, instead of ignoring whole row.
                        EventContract.QrFoundEntry.TABLE_NAME + " ON " +
                        EventContract.QrFoundEntry.TABLE_NAME +
                        "." + EventContract.QrFoundEntry.COLUMN_NAME_QR_ID +
                        " = " + EventContract.QrEntry.TABLE_NAME + "." +
                        EventContract.QrEntry._ID
        );
    }


    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_EVENT, EVENT);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_EVENT + "/#", EVENT_ID); //# = numbers
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_SPEAKER, SPEAKER);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_SPEAKER + "/#", SPEAKER_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_LOCATION, LOCATION);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_LOCATION + "/#", LOCATION_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_SPEAKERS_IN_EVENTS, SPEAKERS_IN_EVENTS);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_SPEAKERS_IN_EVENTS + "/#", SPEAKERS_IN_EVENTS_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_SPEAKERS_IN_EVENTS + "/event/#", SPEAKERS_IN_EVENTS_EVENT_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_FAVORITES, FAVORITES);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_FAVORITES + "/event", FAVORITES_EVENTS);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QR, QR);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QR + "/#", QR_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QR + "/hash/*", QR_HASH); //find by hash, * = numbers and letters
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QRFOUND, QR_FOUND);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QRFOUND + "/#", QR_FOUND_ID);
        matcher.addURI(EventContract.CONTENT_AUTHORITY, EventContract.PATH_QRFOUND + "/qr/#", QR_FOUND_QR_ID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new EventDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor = null;
        //TODO: finish this!
//        retCursor = mOpenHelper.getReadableDatabase().query()
        switch (sUriMatcher.match(uri)) {
            case EVENT: {
                //List of Events
                retCursor = mOpenHelper.getReadableDatabase().query(
                        //EventContract.EventEntry.TABLE_NAME,
                        sEventsWithLocationAndFavoriteQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case EVENT_ID: {
                //1 Event, with speakers
                //Override selection when empty
                //SELECT event.title, location.name FROM event JOIN location ON event.location_id = location._id WHERE event._id = ?
                if (selection == null || "".equals(selection)) {
                    selection = EventContract.EventEntry.TABLE_NAME + "." + EventContract.EventEntry._ID + " = ?";
                    //content://nl.frankkie.bronydays2015/event/0 <-- last segment is ID.
                    selectionArgs = new String[]{uri.getLastPathSegment()};
                }
                retCursor = mOpenHelper.getReadableDatabase().query(
                        sEventWithLocationQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null, null,
                        sortOrder

                );
                break;
            }
            case SPEAKER: {
                //list
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.SpeakerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case SPEAKER_ID: {
                //1 speaker
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.SpeakerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case LOCATION: {
                //List of location
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case LOCATION_ID: {
                //1 location
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case SPEAKERS_IN_EVENTS: {
                //list of event and speaker ids
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.SpeakersInEventsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case SPEAKERS_IN_EVENTS_ID: {
                //1 row with an event id and a speaker id
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.SpeakersInEventsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case SPEAKERS_IN_EVENTS_EVENT_ID: {
                //all speakers of 1 event (from id)
                if (selection == null || "".equals(selection)) {
                    selection = EventContract.SpeakersInEventsEntry.TABLE_NAME + "." + EventContract.SpeakersInEventsEntry.COLUMN_NAME_EVENT_ID + " = ?";
                    //content://nl.frankkie.bronydays2015/speakers_in_events/event/0 <-- last segment is ID.
                    selectionArgs = new String[]{uri.getLastPathSegment()};
                }
                retCursor = mOpenHelper.getReadableDatabase().query(
                        //EventContract.SpeakersInEventsEntry.TABLE_NAME,
                        sSpeakersWithEventQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case FAVORITES: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.FavoritesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case FAVORITES_EVENTS: {
                if (selection == null || "".equals(selection)) {
                    selection = EventContract.FavoritesEntry.TABLE_NAME + "." + EventContract.FavoritesEntry.COLUMN_NAME_TYPE + " = ?";
                    selectionArgs = new String[]{EventContract.FavoritesEntry.TYPE_EVENT};
                }
                retCursor = mOpenHelper.getReadableDatabase().query(
                        sFavoriteEventsWithLocationQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null, //having
                        null, //group by
                        sortOrder
                );
                break;
            }
            case QR: {
                //List all QR's to find, with time_found (from the other table)                
                retCursor = mOpenHelper.getReadableDatabase().query(
                        sQrWithFoundQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QR_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.QrEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QR_HASH: {
                //TODO: IMPLETMENT THIS !!!
                if (selection == null || "".equals(selection)) {
                    selection = EventContract.QrEntry.TABLE_NAME + "." + EventContract.QrEntry.COLUMN_NAME_HASH + " = ?";
                    selectionArgs = new String[]{uri.getLastPathSegment()};
                }
                retCursor = mOpenHelper.getReadableDatabase().query(
                        sQrWithFoundQueryBuilder.getTables(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QR_FOUND: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.QrFoundEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QR_FOUND_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.QrFoundEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QR_FOUND_QR_ID: {
                //get found by QR id
                if (selection == null || "".equals(selection)) {
                    selection = EventContract.QrFoundEntry.TABLE_NAME + "." + EventContract.QrFoundEntry.COLUMN_NAME_QR_ID + " = ?";
                    selectionArgs = new String[]{uri.getLastPathSegment()};
                }
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EventContract.QrFoundEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Register a content-observer (to watch for content changes)
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case EVENT: {
                return EventContract.EventEntry.CONTENT_TYPE; //list
            }
            case EVENT_ID: {
                return EventContract.EventEntry.CONTENT_ITEM_TYPE; //1 event with speakers
            }
            case SPEAKER: {
                return EventContract.SpeakerEntry.CONTENT_TYPE; //list
            }
            case SPEAKER_ID: {
                return EventContract.SpeakerEntry.CONTENT_ITEM_TYPE; //list
            }
            case LOCATION: {
                return EventContract.LocationEntry.CONTENT_TYPE; // list
            }
            case LOCATION_ID: {
                return EventContract.LocationEntry.CONTENT_ITEM_TYPE; //1 location
            }
            case SPEAKERS_IN_EVENTS: {
                return EventContract.SpeakersInEventsEntry.CONTENT_TYPE; //list of all speakers in all events
            }
            case SPEAKERS_IN_EVENTS_ID: {
                return EventContract.SpeakersInEventsEntry.CONTENT_TYPE; //list of speakers in 1 event
            }
            case FAVORITES: {
                return EventContract.FavoritesEntry.CONTENT_TYPE; //list of all favorites of all types
            }
            case FAVORITES_EVENTS: {
                return EventContract.FavoritesEntry.CONTENT_TYPE; //list of all favorites of type event
            }
            case QR: {
                return EventContract.QrEntry.CONTENT_TYPE; //list
            }
            case QR_ID: {
                return EventContract.QrEntry.CONTENT_ITEM_TYPE; //1 qr code (with hash)
            }
            case QR_HASH: {
                return EventContract.QrEntry.CONTENT_ITEM_TYPE; //1 qr code (with found_time) by hash
            }
            case QR_FOUND: {
                return EventContract.QrFoundEntry.CONTENT_TYPE; //list of found qr-codes
            }
            case QR_FOUND_ID: {
                return EventContract.QrFoundEntry.CONTENT_ITEM_TYPE; //1 found qr (with time) by found_id
            }
            case QR_FOUND_QR_ID: {
                return EventContract.QrFoundEntry.CONTENT_ITEM_TYPE; //1 found qr (with time) by qr_id
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //No idea why the db-var is final in Sunshine.
        //Its only used in this method, so idk, might be performance?
        //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/data/WeatherProvider.java#L221
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        Uri returnUri;
        switch (match) {
            //*cough* code duplication *cough*
            case EVENT: {
                //Database insert returns row_id of new row.
                long id = db.insert(EventContract.EventEntry.TABLE_NAME, null, values);
                if (id != -1L) { //return -1 on error
                    returnUri = EventContract.EventEntry.buildEventUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert Event row into: " + uri);
                }
                break;
            }
            case LOCATION: {
                long id = db.insert(EventContract.LocationEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.LocationEntry.buildLocationUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert Location row into: " + uri);
                }
                break;
            }
            case SPEAKER: {
                long id = db.insert(EventContract.SpeakerEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.SpeakerEntry.buildSpeakerUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert Speaker row into: " + uri);
                }
                break;
            }
            case SPEAKERS_IN_EVENTS: {
                long id = db.insert(EventContract.SpeakersInEventsEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.SpeakersInEventsEntry.buildSpeakersInEventsUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert SpeakersInEventsEntry row into: " + uri);
                }
                break;
            }
            case FAVORITES: {
                long id = db.insert(EventContract.FavoritesEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.FavoritesEntry.buildFavoriteUri(id);
                } else {
                    throw new SQLException("Failed to insert Favorite row into: " + uri);
                }
                break;
            }
            case QR: {
                long id = db.insert(EventContract.QrEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.QrEntry.buildQrUri(id);
                } else {
                    throw new SQLException("Failed to insert Qr row into: " + uri);
                }
                break;
            }
            case QR_FOUND: {
                long id = db.insert(EventContract.QrFoundEntry.TABLE_NAME, null, values);
                if (id != -1L) {
                    returnUri = EventContract.QrFoundEntry.buildQrFoundUri(id);
                } else {
                    throw new SQLException("Failed to insert QrFound row into: " + uri);
                }
                break;
            }
            default:
                throw new android.database.SQLException("Unknown uri: " + uri);
        }
        //Notify that DB has changed.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case EVENT: {
                db.beginTransaction();
                int returnInt = 0; //number of added rows
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(EventContract.EventEntry.TABLE_NAME, null, value);
                        if (id != -1L) {
                            returnInt++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return returnInt;
            }
            case LOCATION: {
                db.beginTransaction();
                int returnInt = 0; //number of added rows
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(EventContract.LocationEntry.TABLE_NAME, null, value);
                        if (id != -1L) {
                            returnInt++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return returnInt;
            }
            case SPEAKER: {
                db.beginTransaction();
                int returnInt = 0; //number of added rows
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(EventContract.SpeakerEntry.TABLE_NAME, null, value);
                        if (id != -1L) {
                            returnInt++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return returnInt;
            }
            case SPEAKERS_IN_EVENTS: {
                db.beginTransaction();
                int returnInt = 0; //number of added rows
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(EventContract.SpeakersInEventsEntry.TABLE_NAME, null, value);
                        if (id != -1L) {
                            returnInt++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return returnInt;
            }
            case QR: {
                db.beginTransaction();
                int returnInt = 0; // number of rows added
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(EventContract.QrEntry.TABLE_NAME, null, value);
                        if (id != -1L) {
                            returnInt++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return returnInt;
            }
            //TODO: implement BulkInsert for QRFOUND
            //Note: No Rush, default implementation is to use regular insert when BulkInsert is not implement.
            //It'll work, but less optimized.
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int numberOfRowsDeleted = 0;
        switch (match) {
            case EVENT: {
                numberOfRowsDeleted = db.delete(EventContract.EventEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case LOCATION: {
                numberOfRowsDeleted = db.delete(EventContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case SPEAKER: {
                numberOfRowsDeleted = db.delete(EventContract.SpeakerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case SPEAKERS_IN_EVENTS: {
                numberOfRowsDeleted = db.delete(EventContract.SpeakersInEventsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case FAVORITES: {
                numberOfRowsDeleted = db.delete(EventContract.FavoritesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case QR: {
                numberOfRowsDeleted = db.delete(EventContract.QrEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case QR_FOUND: {
                numberOfRowsDeleted = db.delete(EventContract.QrFoundEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Null as selection deletes all rows
        if (selection == null || numberOfRowsDeleted != 0) {
            //notify that DB has changed.
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numberOfRowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int rowsUpdated = 0;
        switch (match) {
            case EVENT: {
                rowsUpdated = db.update(EventContract.EventEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case LOCATION: {
                rowsUpdated = db.update(EventContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case SPEAKER: {
                rowsUpdated = db.update(EventContract.SpeakerEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case SPEAKERS_IN_EVENTS: {
                rowsUpdated = db.update(EventContract.SpeakersInEventsEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case FAVORITES: {
                rowsUpdated = db.update(EventContract.FavoritesEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case QR: {
                rowsUpdated = db.update(EventContract.QrEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case QR_FOUND: {
                rowsUpdated = db.update(EventContract.QrFoundEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
