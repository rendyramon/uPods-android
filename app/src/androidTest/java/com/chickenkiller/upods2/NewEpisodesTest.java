package com.chickenkiller.upods2;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.chickenkiller.upods2.controllers.app.UpodsApplication;
import com.chickenkiller.upods2.models.Episode;
import com.chickenkiller.upods2.models.Feed;
import com.chickenkiller.upods2.models.Podcast;
import com.chickenkiller.upods2.utils.HelpFunctions;
import com.chickenkiller.upods2.utils.Logger;
import com.chickenkiller.upods2.utils.ServerApi;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * Created by Alon Zilberman on 3/8/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NewEpisodesTest {

    private static final String REAL_PODCAST_FEED_RU = "http://wylsa.com/PODCAST/podcast.xml";

    private static final String REAL_PODCAST_FEED_ENG = "https://feeds.audiometric.io/2372467022";
    private static final String REAL_PODCAST_FEED_ENG2 = "http://www.npr.org/rss/podcast.php?id=510019&uid=n1qe4e85742c986fdb81d2d38ffa0d5d53";

    private static final String TEST_FEED = ServerApi.DOMAIN + "/static/podcasts/feed/test_podcast.xml";
    private static final String TEST_FEED_CONTROL = ServerApi.DOMAIN + "/upods/api/v1.0/podcasts/test/feed?task=";
    private static final int NEW_EPISODES_ADDED = 2;
    private static final int EPISODES_TO_REMOVE = 1;


    public static class ResultBucket {
        boolean hasUpdates;
        long newEpisodesCount;
    }


    private ResultBucket checkNoUpdates(String feed) {
        ResultBucket resultBucket = new ResultBucket();
        try {
            String podcastName = String.valueOf(System.currentTimeMillis()) + "_podcast";
            Podcast podcast = new Podcast(podcastName, feed);

            ArrayList<Episode> parsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            Feed.saveAsFeed(podcast.getFeedUrl(), parsedEpisodes);
            Logger.printInfo("checkNoUpdatesENGScenario", "Feed saved locally...");

            ArrayList<Episode> afterUpdateparsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            resultBucket.hasUpdates = Feed.handleUpdates(afterUpdateparsedEpisodes, podcast);
            resultBucket.newEpisodesCount = podcast.getNewEpisodsCount();
            Logger.printInfo("checkNoUpdatesENGScenario", "Checked for updates...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultBucket;
    }

    @Test
    public void checkNoUpdatesENGScenario() {
        UpodsApplication.initAllResources();

        ResultBucket resultBucket = checkNoUpdates(REAL_PODCAST_FEED_ENG2);
        assertTrue(!resultBucket.hasUpdates);
        assertTrue(resultBucket.newEpisodesCount == 0);
    }

    @Test
    public void checkNoUpdatesRUScenario() {
        UpodsApplication.initAllResources();

        ResultBucket resultBucket = checkNoUpdates(REAL_PODCAST_FEED_RU);
        assertTrue(!resultBucket.hasUpdates);
        assertTrue(resultBucket.newEpisodesCount == 0);
    }

    //Old tests should be enable after some backend problems will be resolved.
    /*
    @Test
    public void checkBasicNewEpisodesScenario() {
        UpodsApplication.initAllResources();

        boolean hasUpdates = false;
        long newEpisodesCount = -1;

        try {
            Request resetRequest = new Request.Builder().url(TEST_FEED_CONTROL + "reset").get().build();
            BackendManager.getInstance().sendSynchronicRequest(resetRequest);
            Logger.printInfo("checkBasicNewEpisodesScenario", "Cleaned the remote test feed...");

            String podcastName = String.valueOf(System.currentTimeMillis()) + "_podcast";
            Podcast podcast = new Podcast(podcastName, TEST_FEED);

            ArrayList<Episode> parsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            Logger.printInfo("checkBasicNewEpisodesScenario", "Parsed episodes...");

            Feed.saveAsFeed(podcast.getFeedUrl(), parsedEpisodes);
            Logger.printInfo("checkBasicNewEpisodesScenario", "Feed saved locally...");

            Request addRequest = new Request.Builder().url(TEST_FEED_CONTROL + "add").get().build();
            BackendManager.getInstance().sendSynchronicRequest(addRequest);
            BackendManager.getInstance().sendSynchronicRequest(addRequest);
            Logger.printInfo("checkBasicNewEpisodesScenario", "2 new episodes added to remote feed...");

            ArrayList<Episode> afterUpdateparsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            podcast.setTracks(afterUpdateparsedEpisodes);

            hasUpdates = Feed.handleUpdates(afterUpdateparsedEpisodes, podcast);
            newEpisodesCount = podcast.getNewEpisodsCount();
            Logger.printInfo("checkBasicNewEpisodesScenario", "Checked for updates...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(hasUpdates);
        assertTrue(newEpisodesCount == NEW_EPISODES_ADDED);
    }

    @Test
    public void checkRealPodcastNewEpisodesScenario() {
        UpodsApplication.initAllResources();

        boolean hasUpdates = false;
        long newEpisodesCount = -1;

        try {
            String podcastName = String.valueOf(System.currentTimeMillis()) + "_podcast";
            Podcast podcast = new Podcast(podcastName, REAL_PODCAST_FEED_ENG);

            ArrayList<Episode> parsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            parsedEpisodes.remove(parsedEpisodes.size() - EPISODES_TO_REMOVE);

            Logger.printInfo("checkRealPodcastNewEpisodesScenario", "Parsed episodes + removed last one...");

            Feed.saveAsFeed(podcast.getFeedUrl(), parsedEpisodes);
            Logger.printInfo("checkRealPodcastNewEpisodesScenario", "Feed saved locally...");

            ArrayList<Episode> afterUpdateparsedEpisodes = HelpFunctions.parseEpisodes(podcast.getFeedUrl());
            hasUpdates = Feed.handleUpdates(afterUpdateparsedEpisodes, podcast);
            podcast.setTracks(afterUpdateparsedEpisodes);

            newEpisodesCount = podcast.getNewEpisodsCount();

            Logger.printInfo("checkRealPodcastNewEpisodesScenario", "Checked for updates...");

        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(hasUpdates);
        assertTrue(newEpisodesCount == EPISODES_TO_REMOVE);
    }*/
}
