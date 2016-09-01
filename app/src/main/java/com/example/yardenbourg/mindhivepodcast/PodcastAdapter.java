package com.example.yardenbourg.mindhivepodcast;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Yardenbourg on 29/08/16.
 */
public class PodcastAdapter extends ArrayAdapter<String> {

    private ArrayList<String> podcastArray;
    private LayoutInflater inflater;

    // Class that holds a reference to the TextView so we do not have to call
    // findViewById every time getView() is called, only when there are no Views to recycle.
    private static class ViewHolder {
        TextView podcastName;
    }

    public PodcastAdapter(Context context, ArrayList<String> arrayList) {
        super(context, R.layout.fragment_text_view);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        podcastArray = arrayList;
    }

    public PodcastAdapter getInstance() {
        return this;
    }

    @Override
    public int getCount() {
        return podcastArray.size();
    }

    @Override
    public String getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        ViewHolder viewHolder;

        // If there are no Views to recycle
        if (convertView == null) {

            // Instantiating a new ViewHolder and TextView
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.fragment_text_view, viewGroup, false);

            // Getting the TextView from the View and saving it in the ViewHolder
            viewHolder.podcastName = (TextView) convertView.findViewById(R.id.podcastListViewText);

            // Saving the ViewHolder on the View as a tag to use later
            convertView.setTag(viewHolder);

        } else {

            // If there is a View to recycle, instantiate the ViewHolder from the one we
            // saved earlier as a Tag in the View
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // The item to display from the ArrayList will correlate to which position we are at
        // in the ListView
        String podcastListItem = podcastArray.get(position);

        // Setting the TextView text
        if (podcastListItem != null) {
            viewHolder.podcastName.setText(podcastListItem);

        } else {

            // Having a default value to fall back to, when there are no more entries to display.
            viewHolder.podcastName.setText("Coming soon...");
        }

        return convertView;
    }

    /**
     * Sets the ArrayList of this Adapter
     * @param array
     */
    public void setPodcastArray(ArrayList<String> array) {
        this.podcastArray = array;
    }
}
