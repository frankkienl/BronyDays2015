package nl.frankkie.bronydays2015.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by fbouwens on 19-11-14.
 */
public class EventContract {

    //Used for ContentProvider
    public static final String CONTENT_AUTHORITY = "nl.frankkie.bronydays2015";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_EVENT = "event";
    public static final String PATH_SPEAKER = "speaker";
    public static final String PATH_LOCATION = "location";
    public static final String PATH_SPEAKERS_IN_EVENTS = "speakers_in_events";
    public static final String PATH_FAVORITES = "favorites";
    public static final String PATH_QR = "qr";
    public static final String PATH_QRFOUND = "qrfound";

    /**
     * Events
     */
    public static final class EventEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_EVENT).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_EVENT;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_EVENT;
        //
        public static final String TABLE_NAME = "event";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TITLE_FR = "title_fr";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_DESCRIPTION_FR = "description_fr";
        public static final String COLUMN_NAME_KEYWORDS = "keywords";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_COLOR = "color";
        public static final String COLUMN_NAME_START_TIME = "start_time";
        public static final String COLUMN_NAME_END_TIME = "end_time";
        public static final String COLUMN_NAME_LOCATION_ID = "location_id";
        public static final String COLUMN_NAME_SORT_ORDER = "sort_order";

        public static Uri buildEventUri(long id) {
            //This method is used in Sunshine
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /**
     * Speakers
     */
    public static final class SpeakerEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKER).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_SPEAKER;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_SPEAKER;
        public static final String TABLE_NAME = "speaker";
        //
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_NAME_FR = "name_fr";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_DESCRIPTION_FR = "description_fr";
        public static final String COLUMN_NAME_IMAGE = "image";
        public static final String COLUMN_NAME_COLOR = "color";

        public static Uri buildSpeakerUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /**
     * Locations
     */
    public static final class LocationEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String TABLE_NAME = "location";
        //
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_NAME_FR = "name_fr";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_DESCRIPTION_FR = "description_fr";
        public static final String COLUMN_NAME_MAP_LOCATION = "map_location";
        public static final String COLUMN_NAME_FLOOR = "floor"; //where 0 is ground-level.

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }


    /**
     * Make a list of Speakers in a Event.
     */
    public static final class SpeakersInEventsEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS_IN_EVENTS).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_SPEAKERS_IN_EVENTS;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_SPEAKERS_IN_EVENTS;
        public static final String TABLE_NAME = "speakers_in_events";
        public static final String COLUMN_NAME_EVENT_ID = "event_id";
        public static final String COLUMN_NAME_SPEAKER_ID = "speaker_id";

        public static Uri buildSpeakersInEventsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildSpeakersInEventUri(long eventId) {
            return ContentUris.withAppendedId(CONTENT_URI.buildUpon().appendPath("event").build(), eventId);
        }
    }

    /**
     * Mark your favorite Events. (And later also other things)
     * Your Favorite Events will be shown in My Schedule.
     */
    public static final class FavoritesEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITES).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITES;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITES;
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_ITEM_ID = "item_id";

        public static final String TYPE_EVENT = "event";
        public static final String TYPE_LOCATION = "location";
        public static final String TYPE_SPEAKER = "speaker";

        public static Uri buildFavoriteUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildFavoritesByTypeEventUri(long eventId){
            return ContentUris.withAppendedId(CONTENT_URI.buildUpon().appendPath("event").build(), eventId);
        }
    }

    /**
     * QR Hunt
     * This table will contain the data from the server.
     * This table will not contain user-data. (found or not)
     * I want to keep this in a separate table.
     */
    public static final class QrEntry implements BaseColumns {
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_QR).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_QR;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_QR;
        public static final String TABLE_NAME = "qr";
        public static final String COLUMN_NAME_HASH = "hash";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_NAME_FR = "name_fr";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_DESCRIPTION_FR = "description_fr";
        public static final String COLUMN_NAME_IMAGE = "image";

        public static Uri buildQrUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildQrByHashUri(String hash){
            //content://nl.frankkie.bronydays2015/qr/hash/<HASH>
            return CONTENT_URI.buildUpon().appendPath("hash").appendPath(hash).build();
        }
    }

    /**
     * QR Found
     * This table will contain rows for all QR-codes that are found,
     * with the time when the QR is found for the first time.
     * (No row = not found)
     */
    public static final class QrFoundEntry implements BaseColumns {
        public static Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_QRFOUND).build();
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_QRFOUND;
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_QRFOUND;
        public static final String TABLE_NAME = "qrfound";
        public static final String COLUMN_NAME_QR_ID = "qr_id"; //which one
        public static final String COLUMN_NAME_TIME = "time_found"; //record the time when this QR was first found.

        public static Uri buildQrFoundUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
