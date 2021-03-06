package com.chickenkiller.upods2.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chickenkiller.upods2.R;
import com.chickenkiller.upods2.activity.ActivityPlayer;
import com.chickenkiller.upods2.controllers.adaperts.MediaItemsAdapter;
import com.chickenkiller.upods2.controllers.internet.BackendManager;
import com.chickenkiller.upods2.controllers.player.SmallPlayer;
import com.chickenkiller.upods2.interfaces.IFragmentsManager;
import com.chickenkiller.upods2.interfaces.IMediaItemView;
import com.chickenkiller.upods2.interfaces.IRequestCallback;
import com.chickenkiller.upods2.interfaces.IToolbarHolder;
import com.chickenkiller.upods2.models.Podcast;
import com.chickenkiller.upods2.models.RadioItem;
import com.chickenkiller.upods2.utils.GlobalUtils;
import com.chickenkiller.upods2.utils.ServerApi;
import com.chickenkiller.upods2.utils.enums.MediaItemType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by Alon Zilberman on 7/10/15.
 */
public class FragmentSearch extends Fragment implements SearchView.OnQueryTextListener {


    public static final String TAG = "search_results";
    public static boolean isActive = false;

    private static String lastQuery = "";

    private RecyclerView rvSearchResults;
    private SmallPlayer smallPlayer;
    private MediaItemsAdapter mediaItemsAdapter;
    private ProgressBar pbLoadingSearch;
    private TextView tvSearchNoResults;
    private TextView tvStartTyping;
    private MediaItemType mediaItemType;
    private LinearLayout lnInternetError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((IToolbarHolder) getActivity()).getToolbar().setVisibility(View.VISIBLE);

        //Init fragments views
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);
        lnInternetError = (LinearLayout) view.findViewById(R.id.lnInternetError);
        pbLoadingSearch = (ProgressBar) view.findViewById(R.id.pbLoadingSearch);
        rvSearchResults = (RecyclerView) view.findViewById(R.id.rvSearchResults);
        tvSearchNoResults = (TextView) view.findViewById(R.id.tvSearchNoResults);
        tvStartTyping = (TextView) view.findViewById(R.id.tvSearchStart);
        smallPlayer = new SmallPlayer(view, getActivity());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(OrientationHelper.VERTICAL);

        //Toolbar
        MenuItem searchMenuItem = ((IToolbarHolder) getActivity()).getToolbar().getMenu().findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        ((IToolbarHolder) getActivity()).getToolbar().setTitle(R.string.search_results);

        //Featured adapter
        mediaItemsAdapter = new MediaItemsAdapter(getActivity(), R.layout.card_media_item_horizontal, R.layout.media_item_title);
        if (getActivity() instanceof IFragmentsManager) {
            mediaItemsAdapter.setFragmentsManager((IFragmentsManager) getActivity());
        }

        //Featured recycle view
        rvSearchResults.setAdapter(mediaItemsAdapter);
        rvSearchResults.setLayoutManager(layoutManager);
        rvSearchResults.setVisibility(View.INVISIBLE);
        tvStartTyping.setVisibility(View.VISIBLE);
        pbLoadingSearch.setVisibility(View.GONE);

        FragmentSearch.isActive = true;
        if (lastQuery != null && !lastQuery.isEmpty()) {
            tvSearchNoResults.setVisibility(View.GONE);
            lnInternetError.setVisibility(View.GONE);
            tvStartTyping.setVisibility(View.GONE);
            loadSearchResults(lastQuery);
        }
        return view;
    }

    public void setSearchType(MediaItemType mediaItemType) {
        this.mediaItemType = mediaItemType;
    }

    private void loadSearchResults(String query) {
        lastQuery = query;
        rvSearchResults.setVisibility(View.GONE);
        pbLoadingSearch.setVisibility(View.VISIBLE);
        if (mediaItemType == MediaItemType.RADIO) {
            query = ServerApi.RADIO_SEARCH + query;
        } else {
            query = ServerApi.PODCAST_SEARCH + query + ServerApi.PODCAST_SEARCH_PARAM;
        }
        BackendManager.getInstance().doSearch(query, new IRequestCallback() {
                    @Override
                    public void onRequestSuccessed(final JSONObject jResponse) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ArrayList<IMediaItemView> resultMediaItems = new ArrayList<IMediaItemView>();
                                    if (mediaItemType == MediaItemType.RADIO) {
                                        resultMediaItems.addAll(RadioItem.withJsonArray(jResponse.getJSONArray("result"), getActivity()));
                                    } else {
                                        resultMediaItems.addAll(Podcast.withJsonArray(jResponse.getJSONArray("results")));
                                    }
                                    mediaItemsAdapter.clearItems();
                                    mediaItemsAdapter.addItems(resultMediaItems);
                                    pbLoadingSearch.setVisibility(View.GONE);
                                    rvSearchResults.setVisibility(View.VISIBLE);
                                    if (resultMediaItems.size() == 0) {
                                        tvSearchNoResults.setVisibility(View.VISIBLE);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onRequestFailed() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbLoadingSearch.setVisibility(View.GONE);
                                rvSearchResults.setVisibility(View.GONE);
                                tvStartTyping.setVisibility(View.GONE);
                                if (GlobalUtils.isInternetConnected()) {
                                    tvSearchNoResults.setVisibility(View.GONE);
                                } else {
                                    lnInternetError.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }

                }
        );
    }

    @Override
    public void onResume() {
        if (smallPlayer != null) {
            smallPlayer.onResume();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (smallPlayer != null) {
            smallPlayer.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (smallPlayer != null) {
            smallPlayer.destroy();
        }
        BackendManager.getInstance().clearSearchQueue();
        FragmentSearch.isActive = false;
        super.onDestroy();
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        tvSearchNoResults.setVisibility(View.GONE);
        lnInternetError.setVisibility(View.GONE);
        tvStartTyping.setVisibility(View.GONE);
        if (query.equals(lastQuery) || query.isEmpty()) {
            return false;
        }
        loadSearchResults(query);
        return false;
    }

    public static void openFromIntent(Activity activity) {
        int startedFrom = activity.getIntent().getIntExtra(ActivityPlayer.ACTIVITY_STARTED_FROM_IN_DEPTH, -1);
        if (startedFrom == MediaItemType.RADIO_SEARCH.ordinal()) {
            FragmentSearch fragmentSearch = new FragmentSearch();
            fragmentSearch.setSearchType(MediaItemType.RADIO);
            ((IFragmentsManager) activity).showFragment(R.id.fl_content, fragmentSearch, FragmentSearch.TAG);
            activity.getIntent().removeExtra(ActivityPlayer.ACTIVITY_STARTED_FROM_IN_DEPTH);
        } else if (startedFrom == MediaItemType.PODCAST_SEARCH.ordinal()) {
            FragmentSearch fragmentSearch = new FragmentSearch();
            fragmentSearch.setSearchType(MediaItemType.PODCAST);
            ((IFragmentsManager) activity).showFragment(R.id.fl_content, fragmentSearch, FragmentSearch.TAG);
            activity.getIntent().removeExtra(ActivityPlayer.ACTIVITY_STARTED_FROM_IN_DEPTH);
        }
    }
}
