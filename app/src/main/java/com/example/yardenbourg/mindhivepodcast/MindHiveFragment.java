package com.example.yardenbourg.mindhivepodcast;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yardenbourg on 10/08/16.
 *
 * Fragment containing the ListView displaying the MindHive podcasts.
 */

public class MindHiveFragment extends Fragment {

    private ArrayAdapter<String> mindhiveAdapter;
    private static ArrayList<String> mindhiveTitles;
    private AmazonS3Client s3Client;
    private CognitoCachingCredentialsProvider credentialsProvider;

    public MindHiveFragment() {

    }

    public static MindHiveFragment newInstance(CognitoCachingCredentialsProvider credentialsProvider) {

        MindHiveFragment fragment = new MindHiveFragment();
        fragment.credentialsProvider = credentialsProvider;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        s3Client = new AmazonS3Client(getCredentialsProvider());
        // This method will fill the ArrayList with the saved data if found, or return an empty
        // ArrayList if no data was found.
        mindhiveTitles = checkForSavedPodcasts();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.mindhive_list_view_fragment, container, false);
        ListView mindHivePodcastList = (ListView) rootView.findViewById(R.id.mindhiveListView);

        // If there were no saved titles...
        if (mindhiveTitles.isEmpty()) {

            // New data needs fetching...
            new FetchMindHivePodcasts().execute();

        } else {

            Log.v("onCreateView", "Data has been saved!");
            // Otherwise, set the filled ArrayList to the Adapter
            mindhiveAdapter = new PodcastAdapter(getActivity(), mindhiveTitles);

            // and attach it to the ListView
            mindHivePodcastList.setAdapter(mindhiveAdapter);
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
        mindhiveAdapter = null;
    }

    /**
     * Returns the credentialsProvider
     * @return credentialsProvider
     */
    public CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    /**
     * Checks for an ArrayList of podcast titles in SharedPreferences, and returns it if found, or
     * an empty ArrayList if not found.
     */
    private ArrayList<String> checkForSavedPodcasts() {

        Set<String> dataFromSharedPrefs = getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getStringSet("Mindhive Titles", new HashSet<String>());

        if (!dataFromSharedPrefs.isEmpty()) {

            // Convert it to an ArrayList and display the contents.
            mindhiveTitles = new ArrayList<>(dataFromSharedPrefs);

            Collections.sort(mindhiveTitles, new FragmentUtilities.NumericalPodcastSort());
            return mindhiveTitles;
        }

        return new ArrayList<>();
    }

    /**
     * This method is called from the parent Activity, which handles the checking of the queue for
     * messages, and updates bucketHasChanged when it's done.
     */
    public void receiveBucketState(boolean needsUpdating) {

        if (needsUpdating) {

            // Runs the AsyncTask specifically for when the ArrayList needs updating.
            new UpdateMindHivePodcasts().execute();
        }
    }

    /**
     * Saves the specified ArrayList into SharedPreferences
     * @param listToSave
     */
    private void saveData(ArrayList<String> listToSave) {

        SharedPreferences.Editor prefs = getActivity().getPreferences(Context.MODE_PRIVATE).edit();

        // If there is already an ArrayList in SharedPreferences
        if (!checkForSavedPodcasts().isEmpty()) {

            // Remove it
            prefs.remove("Mindhive Titles");
            prefs.apply();
        }

        // Putting the new ArrayList into SharedPreferences
        Set<String> mindhiveTitlesToSave = new HashSet<>();
        mindhiveTitlesToSave.addAll(listToSave);
        prefs.putStringSet("Mindhive Titles", mindhiveTitlesToSave);
        prefs.apply();
    }

    /**
     * Removes prefix and suffix from each podcast title and sorts by episode number
     * @param arrayList
     * @return
     */
    public ArrayList<String> formatMindhiveTitles(ArrayList<String> arrayList) {

        ArrayList<String> formattedTitles = new ArrayList<>();

        for (int i = 0; i < arrayList.size(); i++) {

            // Removing the prefix and suffix from each String
            String title = arrayList.get(i);
            String formattedTitle = StringUtils.substringBetween(title, "mindhive podcast/", ".mp3");

            // substringBetween can return null in many occasions, so check before adding to array.
            if (formattedTitle != null) {
                formattedTitles.add(formattedTitle);
            }
        }

        // Sorting.
        Collections.sort(formattedTitles, new FragmentUtilities.NumericalPodcastSort());

        return formattedTitles;
    }

    /**
     * Downloads the podcast titles from the S3 bucket with the specified prefix.
     * This class is called when there are no titles in SharedPreferences, and the ArrayAdapter needs
     * to be created from scratch.
     */
    private class FetchMindHivePodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "mindhive podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> mindhiveTitles) {

            // If no Adapter has been set, i.e. this is the first time the podcasts are loading.
            if (mindhiveTitles != null) {
                mindhiveTitles = formatMindhiveTitles(mindhiveTitles);
            }

            mindhiveAdapter = new PodcastAdapter(getActivity(), mindhiveTitles);

            saveData(mindhiveTitles);
        }
    }

    /**
     * Downloads the podcast titles from the S3 bucket with the specified prefix.
     * This class is called when the bucket needs updating, and so, it clears the current ArrayList,
     * adds the new data, formats it, and notifies the Adapter of the change.
     */
    private class UpdateMindHivePodcasts extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {

            return FragmentUtilities.downloadPodcasts(s3Client, "themindhive", "mindhive podcast/");
        }

        @Override
        protected void onPostExecute(ArrayList<String> arrayList) {

            if (arrayList != null) {
                // Clearing the old data, formatting the new data, and notifying the Adapter that the data has changed.
                mindhiveTitles.clear();
                mindhiveTitles.addAll(formatMindhiveTitles(arrayList));
                mindhiveAdapter.notifyDataSetChanged();
                saveData(mindhiveTitles);
            }
        }
    }
}
