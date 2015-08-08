package nl.frankkie.bronydays2015.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by FrankkieNL on 6-12-2014.
 */
public class ConventionSyncService extends Service {

    private static ConventionSyncAdapter sSyncAdapter= null;
    private static final Object sSyncAdapterLock = new Object();


    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock){
            if (sSyncAdapter == null){
                sSyncAdapter = new ConventionSyncAdapter(getApplicationContext(),true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
