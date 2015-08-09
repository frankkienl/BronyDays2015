package nl.frankkie.bronydays2015;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

/**
 * Created by fbouwens on 23-01-15.
 */
public class QrListAdapter extends CursorAdapter {

    public QrListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.qr_listview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String imageStr = cursor.getString(QrListFragment.COL_IMAGE);
        int foundTime = cursor.getInt(QrListFragment.COL_FOUND_TIME);
        final String name = (QrListFragment.showNames || foundTime > 0) ? cursor.getString(QrListFragment.COL_NAME) : "???";
        final String description = (QrListFragment.showDescription || foundTime > 0) ? cursor.getString(QrListFragment.COL_DESCRIPTION) : "???";
        if (foundTime > 0 || QrListFragment.showImage) { //is found? non-zero is yes.
            //When found, show image.
            Ion.with(context)
                    .load(imageStr)
                    .withBitmap()
                    .error(R.drawable.ic_bronydays2015_launcher)
                    .placeholder(R.drawable.ic_bronydays2015_launcher)
                    .intoImageView(viewHolder.mImage);
        } else {            
            //Not found, and not allowed to show image, dont show.
            viewHolder.mImage.setImageResource(R.drawable.ic_bronydays2015_launcher_grey);
        }

        viewHolder.mName.setText(name);
        viewHolder.mDescription.setText(description);

        //This does not work somehow. Maybe use ListView.setOnItemClickListener instead?
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AlertDialog.Builder b = new AlertDialog.Builder(context);
//                b.setTitle(name);
//                b.setMessage(description);
//                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //nothing, just close dialog
//                    }
//                });
//            }
//        });
    }

    public class ViewHolder {
        public final ImageView mImage;
        public final TextView mName;
        public final TextView mDescription;

        public ViewHolder(View view) {
            this.mImage = (ImageView) view.findViewById(R.id.qrlistitem_image);
            this.mName = (TextView) view.findViewById(R.id.qrlistitem_name);
            this.mDescription = (TextView) view.findViewById(R.id.qrlistitem_description);
        }
    }
}
