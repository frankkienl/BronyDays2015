package nl.frankkie.bronydays2015.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


/**
 * Created by FrankkieNL on 6-12-2014.
 */
public class ConventionAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private ConventionAuthenticator mAuthenticator;
    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new ConventionAuthenticator(this);
    }
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
