package com.example.yardenbourg.mindhivepodcast;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yardenbourg on 10/08/16.
 *
 * Fragment containing the ListView displaying the Kinwomen podcasts. All the data retrieval happens here.
 * As the Fragment is loaded it checks for saved titles in SharedPreferences and loads them if found. If not,
 * it continues with the normal flow and downloads the titles, then saves them.
 */
public class KinwomenFragment extends Fragment {

    private ArrayAdapter<String> kinwomenAdapter;
    private static ArrayList<String> kinwomenTitles;
    private AmazonS3Client s3Client;
    private CognitoCachingCredentialsProvider credentialsProvider;

    public KinwomenFragment() {

    }

    public static KinwomenFragment newInstance(CognitoCachingCredentialsProvider credentialsProvider) {

        KinwomenFragment fragment = new KinwomenFragment();
        fragment.credentialsProvider = credentialsProvider;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        s3Client = new AmazonS3Client(getCredentialsProvider());

        // Checking for data
        kinwomenTitles = checkForSavedPodcasts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.kinwomen_list_view_fragment, container, false);
        ListView kinwomenPodcastList = (ListView)rootView.findViewById(R.id.kinwomenListView);

        // If there were no saved titles...
        if (kinwomenTitles.isEmpty()) {

            new FetchKinwomenPodcasts().execute();

        } else {

            Log.v("onCreateView", "Data has been saved!");
            // Otherwise, set the filled ArrayList to the Adapter
            kinwomenAdapter = new PodcastAdapter(getActivity(), kinwomenTitles);

            // and attach it to the ListView
            kinwomenPodcastList.setAdapter(kinwomenAdapter);
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        kinwomenAdapter = null;
    }

    /**
     * Returns the credentialsProvider
     * @return credentialsProvider
     */
    private CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * Checks for a saved ArrayList of podcast titles in SharedPreferences, and returns it if found, or an
     * empty ArrayList if not found.
     */
    private ArrayList<String> checkForSavedPodcasts() {

        Set<String> dataFromSharedPrefs = getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getStringSet("Kinwomen Titles", new HashSet<String>());

        if (!dataFromSharedPrefs.isEmpty()) {

            // Convert it to an ArrayList and display the contents.
            kinwomenTitles = new ArrayList<>(dataFromSharedPrefs);
            return kinwomenTitles;

        }
        return new ArrayList<>();
    }

    /**
     * This method is called from the parent Activity, which handles the checking of the queue for
     * messages, and receives a boolean value representing if the ArrayList needs updating or not.
     */
    public void receiveBucketState(boolean needsUpdating) {

        if (needsUpdating) {
            // Runs the AsyncTask specifically for when the ArrayList needs updating.
            new UpdateKinwomenPodcasts().execute();
        }
    }

    /**
     * Saves the specified ArrayList into SharedPreferences
     * @param listToSave
     */
    private void saveData(ArrayList<String> listToSave) {

        SharedPreferences.Editor prefs = getActivity().getPreferences(Context.MODE_PRIVATE).edit();

        // If there is already an ArrayList in SharedPreferences...
        if (!checkForSavedPodcasts().isEmpty()) {

            // Clear it out...
            prefs.remove("Kinwomen Titles");
            prefs.apply();
        }

        // And set the new one
        Set<String> kinwomenTitlesToSave = new HashSet<>();
        kinwomenTitlesToSave.addAll(listToSave);
        prefs.putStringSet("Kinwomen Titles", kinwomenTitlesToSave);
        prefs.apply();
    }

    public ArrayList<String> formatKinwomenTitles(ArrayList<String> arrayList) {

        ArrayList<String> formattedTitles = new ArrayList<>();

        for (int i = 0; i < arrayList.size(); i++) {

            String title = arrayList.get(i);

            formattedTitles.add(StringUtils.substringBetween(title, "KIN Women Podcast/", ".mp3"));
        }

        return formattedTitles;
    }

    /**
     * Downloads the podcast titles from the S3 bucket with the specified prefix.
     * This class is called when there are no titles in SharedPreferences, and the ArrayAdapter needs
     * to be created from scratch.
     */
    private class FetchKinwomenPodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "KIN Women Podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> kinwomenTitles) {

            if (kinwomenTitles != null) {
                // Formatting the titles to get rid of the bucket prefix and .mp3 suffix.
                kinwomenTitles = formatKinwomenTitles(kinwomenTitles);
                kinwomenAdapter = new PodcastAdapter(getActivity(), kinwomenTitles);
            }

            // Saving the data
            saveData(kinwomenTitles);
        }
    }

    /**
     * Downloads the podcast titles from the S3 bucket with the specified prefix.
     * This class is called when the bucket needs updating, as such, it clears the current ArrayList,
     * adds the new data, and notifies the Adapter of the change.
     */
    private class UpdateKinwomenPodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "KIN Women Podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> arrayList) {

            // Clearing out the old data from the list, formatting the new data,
            // and notifying the Adapter the data has changed.
            kinwomenTitles.clear();
            kinwomenTitles.addAll(formatKinwomenTitles(arrayList));

            kinwomenAdapter.notifyDataSetChanged();

            saveData(kinwomenTitles);
        }
    }
}
