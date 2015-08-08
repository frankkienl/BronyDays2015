package nl.frankkie.bronydays2015;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import nl.frankkie.bronydays2015.util.Util;

/**
 * Created by fbouwens on 21-11-14.
 */
public class ScheduleListAdapter extends CursorAdapter {

    public ScheduleListAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //I use only 1 view type, unlike Sunshine.
        View view = LayoutInflater.from(context).inflate(R.layout.schedule_listview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        //Set the viewholder as tag, this is for performance gain, when this view is re-used.
        //As you don't have to call findViewById again.
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.titleView.setText(cursor.getString(ScheduleListFragment.COL_TITLE));
        long time = cursor.getLong(ScheduleListFragment.COL_TIME);
        viewHolder.timeView.setText(Util.getDataTimeString(time));
        viewHolder.locationView.setText(cursor.getString(ScheduleListFragment.COL_LOCATION));
        //no image or color in Schedule
    }

    public static class ViewHolder {
        //Using ViewHolder, like in Sunshine.
        //See: https://github.com/udacity/Sunshine/blob/6.10-update-map-intent/app/src/main/java/com/example/android/sunshine/app/ForecastAdapter.java
        //No need for the <strike>FrameLayout</strike> 'View that draws the Background' here, as it is the view_root.
        public final ImageView imageView;
        public final TextView titleView;
        public final TextView timeView;
        public final TextView locationView;

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.schedule_listview_item_backgroudimage);
            titleView = (TextView) view.findViewById(R.id.schedule_listview_item_eventname);
            timeView = (TextView) view.findViewById(R.id.schedule_listview_item_eventtime);
            locationView = (TextView) view.findViewById(R.id.schedule_listview_item_eventlocation);
        }
    }
}
