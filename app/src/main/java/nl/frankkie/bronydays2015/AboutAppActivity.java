package nl.frankkie.bronydays2015;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import org.acra.ACRA;

/**
 * Created by FrankkieNL on 13-1-2015.
 */
public class AboutAppActivity extends ActionBarActivity {
    
    int timesClicked = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        // Show the Up button in the action bar.
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getTitle());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        //Fill data
        TextView version = (TextView) findViewById(R.id.aboutapp_version);
        String versionString = "";
        try {
            versionString = "Version: " + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName + "-" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException nnfe){
            nnfe.printStackTrace();
            ACRA.getErrorReporter().handleException(nnfe);
        }
        version.setText(versionString);

        WebView wv = (WebView) findViewById(R.id.aboutapp_webview);
        wv.loadUrl("file:///android_asset/licences.html");
        
        //Easter Egg stuff
        ImageView image = (ImageView) findViewById(R.id.aboutapp_frankkienl_image);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timesClicked++;
                if (timesClicked >= 10){
                    Intent i = new Intent(AboutAppActivity.this,EasterEggActivity.class);
                    startActivity(i);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, EventListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
