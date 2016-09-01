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
    private boolean arrayDoesExist;
    private boolean bucketHasChanged;


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
            arrayDoesExist = true;
            return kinwomenTitles;

        } else {

            arrayDoesExist = false;
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

    /**
     * Async class that uses the credentialsProvider and the AmazonS3Client to access the bucket
     * the podcasts are stored in, reads each object, and puts the key (basically the file name)
     * into an ArrayList that can be read from later on.
     */
    private class FetchKinwomenPodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "KIN Women Podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> kinwomenTitles) {

            if (kinwomenTitles != null) {
                kinwomenAdapter = new PodcastAdapter(getActivity(), kinwomenTitles);
            }

            saveData(kinwomenTitles);
        }
    }

    private class UpdateKinwomenPodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "KIN Women Podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> arrayList) {

            // Clearing out the old data from the list, and notifying the Adapter the data has changed.
            kinwomenTitles.clear();
            kinwomenTitles.addAll(arrayList);
            kinwomenAdapter.notifyDataSetChanged();

            saveData(arrayList);
        }
    }
}
