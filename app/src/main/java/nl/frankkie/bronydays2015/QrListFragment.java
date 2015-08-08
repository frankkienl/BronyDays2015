package nl.frankkie.bronydays2015;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.acra.ACRA;

import nl.frankkie.bronydays2015.data.EventContract;
import nl.frankkie.bronydays2015.util.GcmUtil;
import nl.frankkie.bronydays2015.util.GoogleApiUtil;
import nl.frankkie.bronydays2015.util.Util;

/**
 * Created by fbouwens on 23-01-15.
 */
public class QrListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
/*
    Rubber Ducky needed.
    How much is the user allowed to know (in advance) ?
    Like:
    - How much QR codes are there in total (to be found)
    - Names of non-found QR codes
    - Descriptions of non-found QR codes
    - Images of non-found QR codes

    So, like, When the game starts (nothing found yet)
    Does the player see:
    - an empty list ; User does not know how many, names or descriptions ; List will fill, show only found.
    - a list of names ; User does know how many and the name, not the descriptions
     */

    public static final String TAG = "BronyDays2015";

    //The booleans are not used yet.
    public static boolean showNumber = true; //User can know how many there are in total
    public static boolean showNames = true; //Show list of names; showNumber has more priority
    public static boolean showDescription = false; //Show description when not found yet.
    public static boolean showImage = false; //Show image when not found yet.

    int QRLIST_LOADER = 0;
    private QrListAdapter mListAdapter;
    private ListView mListView;
    private GoogleApiUtil.GiveMeGoogleApiClient apiClientGetter;
    Handler handler = new Handler();

    public static final int COL_ID = 0;
    public static final int COL_HASH = 1;
    public static final int COL_NAME = 2;
    public static final int COL_DESCRIPTION = 3;
    public static final int COL_IMAGE = 4;
    public static final int COL_FOUND_TIME = 5;

    public static final String[] QRLIST_COLUMNS = {
            EventContract.QrEntry.TABLE_NAME + "." + EventContract.QrEntry._ID,
            EventContract.QrEntry.COLUMN_NAME_HASH,
            EventContract.QrEntry.COLUMN_NAME_NAME,
            EventContract.QrEntry.COLUMN_NAME_DESCRIPTION,
            EventContract.QrEntry.COLUMN_NAME_IMAGE,
            EventContract.QrFoundEntry.TABLE_NAME + "." + EventContract.QrFoundEntry.COLUMN_NAME_TIME
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse("content://nl.frankkie.bronydays2015/qr/"); //list of all QR codes to be found
        CursorLoader cl = new CursorLoader(getActivity(), uri, QRLIST_COLUMNS, null, null, null);
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.swapCursor(null);
    }

    //Mandatory empty constructor
    public QrListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //mListAdapter = new QrListAdapter(getActivity(), null, 0); //Cursor comes later
        View v = inflater.inflate(R.layout.fragment_qr_list, container, false);
        //mListView = (ListView) v.findViewById(android.R.id.list);
        Button scanButton = (Button) v.findViewById(R.id.qr_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQR();
            }
        });
        return v;
    }

    public void scanQR() {
        IntentIntegrator ii = IntentIntegrator.forSupportFragment(this); //yes, this Fragment, not the getActivity(). So it will use the onActivityResult of the Fragment.
        ii.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES); //Only Look for QR Codes
        ii.setCaptureLayout(R.layout.qr_capture);
        ii.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            //Check if correct QR code and stuff
            //Doing this in AsyncTask, becauase database lookup can take time, same goes for hashing.
            CheckQRCodeTask task = new CheckQRCodeTask(getActivity(), intentResult.getContents());
            task.execute();
        } else {
            Toast.makeText(getActivity(), "No QR code scanned", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListAdapter = new QrListAdapter(getActivity(), null, 0);
        mListView = getListView();
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QrListAdapter.ViewHolder viewHolder = (QrListAdapter.ViewHolder) view.getTag();
                AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                b.setTitle(viewHolder.mName.getText().toString());
                b.setMessage(viewHolder.mDescription.getText().toString());
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //remove dialog
                    }
                });
                b.create().show();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(QRLIST_LOADER, null, this);
        apiClientGetter = (GoogleApiUtil.GiveMeGoogleApiClient) getActivity();
    }

    public class CheckQRCodeTask extends AsyncTask<Void, Void, Void> {
        Context context;
        String qrdata;

        public CheckQRCodeTask(Context context, String qrdata) {
            this.context = context;
            this.qrdata = qrdata;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (qrdata == null || "".equals(qrdata)){
                //No data
                return null;                
            }
            //Check 1
            //Does it have the correct uri-scheme
            if (!qrdata.startsWith("http://wofje.8s.nl/bronydays2015/webapp/qrhunt.php?qr=")){
                Log.e(TAG,"QR code does not start with the correct URI-scheme");
                //Not a QR hunt code, but some other code
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast-messages from AsyncTask is a no-no. Use Handler.
                        Toast.makeText(context, R.string.qr_not_on_list, Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }

            //Check 2
            //Does it parse as a uri
            //Get uuid from qr-uri
            String uuid = "";
            try {
                Uri uri = Uri.parse(qrdata);
                //uuid = uri.getLastPathSegment();
                uuid = uri.getQueryParameter("qr");
            } catch (Exception e){
                Log.e(TAG,"QR code does not parse as a correct URI");
                ACRA.getErrorReporter().handleException(e);
                //Not a correct Uri.
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast-messages from AsyncTask is a no-no. Use Handler.
                        Toast.makeText(context, R.string.qr_not_on_list, Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }

            //Check 3
            //Is this QR code in the database
            //Create Hash
            String regId = GcmUtil.gcmGetRegId(context);
            String hash = Util.sha1Hash(uuid + regId); //regId is the Salt.
            Log.e(TAG, uuid);
            Log.e(TAG, hash);
            Cursor cursor = context.getContentResolver().query(
                    EventContract.QrEntry.buildQrByHashUri(hash),
                    QRLIST_COLUMNS, //projection (which columns)
                    null, //selection - intentionally null
                    null, //selectionArgs - intentionally null
                    null //sortorder does not matter with 1 result
            );
            int numRows = cursor.getCount();
            if (numRows == 0) {
                Log.e(TAG,"QR code is not found in the database.");
                //Not on the list
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast-messages from AsyncTask is a no-no. Use Handler.
                        Toast.makeText(context, R.string.qr_not_on_list, Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
            //if not found before, add a row.
            cursor.moveToFirst();
            String timeFound = cursor.getString(COL_FOUND_TIME);
            if (timeFound == null || "".equals(timeFound)) {
                //not found before. Add row
                ContentValues cv = new ContentValues();
                cv.put(EventContract.QrFoundEntry.COLUMN_NAME_QR_ID, cursor.getInt(COL_ID));
                long unixtimestamp = System.currentTimeMillis() / 1000L;
                //http://stackoverflow.com/questions/732034/getting-unixtime-in-java
                //Devide by 1000, to get Unix Timestamp. Server uses timestamp.
                cv.put(EventContract.QrFoundEntry.COLUMN_NAME_TIME, unixtimestamp);
                context.getContentResolver().insert(EventContract.QrFoundEntry.CONTENT_URI, cv);
                //Notify URI, so the list refreshes.
                context.getContentResolver().notifyChange(EventContract.QrEntry.CONTENT_URI,null,false);
                final String nameOfQrCode = cursor.getString(COL_NAME);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.qr_found) + "\n" + nameOfQrCode, Toast.LENGTH_LONG).show();
                    }
                });
                //Sync to cloud
                Util.sendQrFound(getActivity());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Google Play Games
                        try {
                            Games.Achievements.unlock(apiClientGetter.getGoogleApiClient(), getActivity().getString(R.string.achievement_its_a_start));
                            Games.Achievements.increment(apiClientGetter.getGoogleApiClient(), getActivity().getString(R.string.achievement_find_5), 1);
                        } catch (Exception e){
                            ACRA.getErrorReporter().handleException(e);
                        }
                    }
                }, 1000); //wait a second for silent auto login to complete.
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.qr_found_already, Toast.LENGTH_LONG).show();
                    }
                });
            }
            cursor.close();
            return null;
        }
    }    
}
