package nl.frankkie.bronydays2015.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.acra.ACRA;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.frankkie.bronydays2015.AboutActivity;
import nl.frankkie.bronydays2015.MapActivity;
import nl.frankkie.bronydays2015.R;
import nl.frankkie.bronydays2015.ScheduleActivity;

/**
 * Created by fbouwens on 10-12-14.
 */
public class Util {
    public static final int navigationDrawerIntentFlags = Intent.FLAG_ACTIVITY_CLEAR_TOP;

    public static final String DATE_FORMAT = "E, HH:mm"; //example: Sunday, 16:30
    public static SimpleDateFormat displayDataFormat = new SimpleDateFormat(DATE_FORMAT);

    public static String getDataTimeString(long timestamp) {
        //*1000, because
        // http://www.onlineconversion.com/unix_time.htm
        // uses SECONDS from 1970, but Date uses MILLISECONDS from 1970
        return displayDataFormat.format(new Date(timestamp * 1000));
    }

    public static void navigateFromNavDrawer(Activity from, Intent to) {
        //Inspired by NavUtils.navigateToUp
        //see: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/master/v4/java/android/support/v4/app/NavUtils.java
        to.addFlags(navigationDrawerIntentFlags);
        from.startActivity(to);
        from.finish();
    }

    public static final int DRAWER_SCHEDULE=0,DRAWER_MAP=1,DRAWER_ABOUT=2;

    public static void navigateFromNavDrawer(Activity thisAct, int position) {
        switch (position) {
            case DRAWER_SCHEDULE: {
                if (!(thisAct instanceof ScheduleActivity))
                    navigateFromNavDrawer(thisAct, new Intent(thisAct, ScheduleActivity.class));
                break;
            }
            case DRAWER_MAP: {
                if (!(thisAct instanceof MapActivity))
                    navigateFromNavDrawer(thisAct, new Intent(thisAct, MapActivity.class));
                break;
            }
            case DRAWER_ABOUT: {
                if (!(thisAct instanceof AboutActivity))
                    navigateFromNavDrawer(thisAct, new Intent(thisAct, AboutActivity.class));
                break;
            }
        }
    }

    public static Account createDummyAccount(Context context) {
        //TODO: Change domain when using for a different convention
        Account account = new Account("dummyaccount", "nl.frankkie.bronydays2015");
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        boolean success = accountManager.addAccountExplicitly(account, null, null);
        if (!success) {
            Log.e(context.getString(R.string.app_name), "Cannot create account for Sync.");
        }
        return account;
    }

    public static final int SYNCFLAG_NONE = 0;
    public static final int SYNCFLAG_CONVENTION_DATA = 1;

    public static void syncData(Context context, int syncWhatFlags) {
        //Create Account needed for SyncAdapter
        Account acc = createDummyAccount(context);
        //Sync
        Bundle syncBundle = new Bundle();
        syncBundle.putInt("syncflags", syncWhatFlags);
        syncBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        syncBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); //as in: run NOW.
        ContentResolver.requestSync(acc, "nl.frankkie.bronydays2015", syncBundle);
    }

    public static void syncConventionData(Context context) {
        syncData(context, SYNCFLAG_CONVENTION_DATA);
    }

    public static String httpDownload(String urlToDownload) {
        //<editor-fold desc="boring http downloading code">
        String json = null;
        HttpURLConnection urlConnection = null;
        BufferedReader br = null;

        try {
            URL url = new URL(urlToDownload);
            //sending regId to update the lastConnected-status on the database
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //<rant>
            //Read it with streams, using much boilerplate,
            //because apparently that is more awesome compared to just using the Apache HttpClient Libs.
            //Srsly, this is 2014, we have libs to do this for us now.
            //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L118
            //</rant>
            InputStream is = urlConnection.getInputStream();
            if (is == null) {
                //Apparently there is no inputstream.
                //We're done here
                return null;
            }

            //Why does Sunshine use a StringBuffer instead of a StringBuilder?
            //A StringBuffer is ThreadSafe (Synchronised) but has worse performance.
            //There is no need to use a ThreadSafe StringBuffer here, this sync-option will never be called multiple times from other threads.
            //Because of 'android:allowParallelSyncs="false"' in R.xml.syncadapter
            //See:
            //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L120
            //http://stackoverflow.com/questions/355089/stringbuilder-and-stringbuffer
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                //Chained append is better than concat
                //https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/sync/SunshineSyncAdapter.java#L132
                sb.append(line).append("\n");
            }
            if (sb.length() == 0) {
                //empty
                sendACRAReport("Util.httpDownload", "Empty Response", urlToDownload);
                return null;
            }

            json = sb.toString();
        } catch (IOException e) {
            Log.e("Convention", "Error while downloading http data from " + urlToDownload, e);
            sendACRAReport("Util.httpDownload", "Error while downloading (IOException)", urlToDownload, e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (br != null) {
                //*cough* boilerplate *cough*
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e("Convention", "Error closing BufferedReader", e);
                    sendACRAReport("Util.httpDownload", "Error closing BufferedReader (IOException)", urlToDownload, e);
                }
            }
        }
        //</editor-fold>
        return json;
    }

    public static String httpPost(Context context, String urlString, String postData) throws IOException {
        HttpURLConnection urlConnection = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            //http://stackoverflow.com/questions/4205980/java-sending-http-parameters-via-post-method-easily
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true); //output, because post-data
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //urlConnection.setRequestProperty("charset","utf-8");
            urlConnection.setRequestProperty("Content-Length", "" + postData.getBytes().length); //simple int to String casting.
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());
            pw = new PrintWriter(os);
            pw.print(postData);
            pw.flush();
            pw.close();
            InputStream is = urlConnection.getInputStream();
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            if (sb.length() == 0) {
                Log.e(context.getString(R.string.app_name), "Util.httpPost: Empty Response\n");
                sendACRAReport("Util.httpPost", "Empty Response", urlString);
            } else {
                Log.e(context.getString(R.string.app_name), "Util.httpPost response:\n" + sb.toString().trim());
                return sb.toString();
            }
        } catch (IOException ioe) {
            Log.e(context.getString(R.string.app_name), "Util.httpPost: IOException");
            sendACRAReport("Util.httpPost", "IOException", "error", ioe);
            ioe.printStackTrace();
            throw new IOException(ioe); //throw to method that called this.
        } finally {
            //*cough* boilerplate *cough*
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(context.getString(R.string.app_name), "Error closing BufferedReader", e);
                    sendACRAReport("Util.httpPost", "Error closing BufferedReader (IOException)", urlString, e);
                    e.printStackTrace();
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        return null;
    }

    /**
     * Send ACRA report
     *
     * @param errormethod
     * @param errorname
     * @param errordata
     */
    public static void sendACRAReport(String errormethod, String errorname, String errordata) {
        ACRA.getErrorReporter().putCustomData("custom_errormethod", errormethod);
        ACRA.getErrorReporter().putCustomData("custom_errorname", errorname);
        ACRA.getErrorReporter().putCustomData("custom_errordata", errordata);
        ACRA.getErrorReporter().handleException(new RuntimeException(errormethod + ": " + errorname + " \n" + errordata));
    }

    /**
     * Send ACRA report
     *
     * @param errormethod
     * @param errorname
     * @param errordata
     */
    public static void sendACRAReport(String errormethod, String errorname, String errordata, Exception e) {
        ACRA.getErrorReporter().putCustomData("custom_errormethod", errormethod);
        ACRA.getErrorReporter().putCustomData("custom_errorname", errorname);
        ACRA.getErrorReporter().putCustomData("custom_errordata", errordata);
        ACRA.getErrorReporter().handleException(e);
    }

    /**
     * SHA1 hash
     * @param toHash
     * @return hash
     */
    public static String sha1Hash(String toHash) {
        // http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android
        String hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();

            // This is ~55x faster than looping and String.formating()
            hash = bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return hash;
    }

    //using lower-case now.
    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        // http://stackoverflow.com/questions/5980658/how-to-sha1-hash-a-string-in-android
        // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
