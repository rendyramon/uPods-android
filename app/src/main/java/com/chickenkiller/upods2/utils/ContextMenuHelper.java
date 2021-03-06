package com.chickenkiller.upods2.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.chickenkiller.upods2.R;
import com.chickenkiller.upods2.controllers.app.ProfileManager;
import com.chickenkiller.upods2.controllers.app.SettingsManager;
import com.chickenkiller.upods2.controllers.internet.BackendManager;
import com.chickenkiller.upods2.controllers.player.UniversalPlayer;
import com.chickenkiller.upods2.dialogs.DialogFragmentAddMediaItem;
import com.chickenkiller.upods2.dialogs.DialogFragmentConfarmation;
import com.chickenkiller.upods2.dialogs.DialogFragmentMessage;
import com.chickenkiller.upods2.fragments.FragmentPlayer;
import com.chickenkiller.upods2.interfaces.IFragmentsManager;
import com.chickenkiller.upods2.interfaces.IOperationFinishCallback;
import com.chickenkiller.upods2.interfaces.IRequestCallback;
import com.chickenkiller.upods2.models.Episode;
import com.chickenkiller.upods2.models.MediaItem;
import com.chickenkiller.upods2.models.Podcast;
import com.chickenkiller.upods2.models.RadioItem;
import com.chickenkiller.upods2.models.Track;
import com.chickenkiller.upods2.utils.enums.MediaItemType;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

/**
 * Created by Alon Zilberman on 10/22/15.
 * Put all logic for context menu clicks here
 */
public class ContextMenuHelper {

    public static void showAboutPodcastDialog(Podcast podcast, IFragmentsManager fragmentsManager) {
        DialogFragmentMessage dialogFragmentMessage = new DialogFragmentMessage();
        dialogFragmentMessage.setTitle(podcast.getName());
        dialogFragmentMessage.setMessage(podcast.getDescription());
        fragmentsManager.showDialogFragment(dialogFragmentMessage);
    }

    public static void showPodcastInFolder(MediaItem mediaItemData, Activity activity) {
        Uri selectedUri = Uri.parse(((Podcast) mediaItemData).getDownloadedDirectory());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "*/*");
        activity.startActivity(intent);
    }

    public static void removeAllDonwloadedEpisods(Activity activity, final Podcast podcast, final IOperationFinishCallback contextItemSelected) {
        DialogFragmentConfarmation dialogFragmentConfarmation = new DialogFragmentConfarmation();
        dialogFragmentConfarmation.setTitle(podcast.getName());
        dialogFragmentConfarmation.setMessage(activity.getString(R.string.remove_podcast_conformation));
        dialogFragmentConfarmation.setPositiveClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = podcast.getDownloadedDirectory();
                GlobalUtils.deleteDirectory(new File(path));
                for (Episode episode : podcast.getEpisodes()) {
                    ProfileManager.getInstance().removeDownloadedTrack(podcast, episode);
                }
                podcast.getEpisodes().clear();
                contextItemSelected.operationFinished();
            }
        });
        ((IFragmentsManager) activity).showDialogFragment(dialogFragmentConfarmation);
    }

    public static void removeDonwloadedTrack(Activity activity, Track track, MediaItem mediaItem,
                                             final IOperationFinishCallback contextItemSelected) {
        ProfileManager.getInstance().removeDownloadedTrack(mediaItem, track);
        Podcast podcast = (Podcast) mediaItem;
        for (int i = 0; i < podcast.getEpisodes().size(); i++) {
            if (GlobalUtils.safeTitleEquals(podcast.getEpisodes().get(i).getTitle(), track.getTitle())) {
                podcast.getEpisodes().remove(i);
            }
        }
        contextItemSelected.operationFinished();
        Toast.makeText(activity, R.string.episod_removed, Toast.LENGTH_SHORT).show();
    }

    public static void showAddMediaDialog(Activity activity, MediaItemType mediaItemType) {
        DialogFragmentAddMediaItem dialogFragmentAddMediaItem = new DialogFragmentAddMediaItem();
        dialogFragmentAddMediaItem.setMediaItemType(mediaItemType);
        ((IFragmentsManager) activity).showDialogFragment(dialogFragmentAddMediaItem);
    }

    public static void selectRadioStreamQuality(final Activity activity, final FragmentPlayer fragmentPlayer, final RadioItem currentMediaItem) {
        final RadioItem playableMediaItem = (RadioItem) UniversalPlayer.getInstance().getPlayingMediaItem();
        String[] availableStreams = (playableMediaItem).getAvailableStreams();
        for (int i = 0; i < availableStreams.length; i++) {
            availableStreams[i] = availableStreams[i] + activity.getString(R.string.kbps);
        }
        if (availableStreams.length == 0) {
            Toast.makeText(activity, activity.getString(R.string.not_available_for_stream), Toast.LENGTH_SHORT).show();
        } else {
            new MaterialDialog.Builder(activity).title(R.string.select_stream_quality)
                    .items(availableStreams)
                    .itemsCallbackSingleChoice(playableMediaItem.getSelectedStreamAsNumber(availableStreams),
                            new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    String selectedStream = text.toString().replace(activity.getString(R.string.kbps), "");
                                    SettingsManager.getInstace().saveStreamQualitySelection(playableMediaItem, selectedStream);
                                    playableMediaItem.selectStreamUrl(selectedStream);
                                    currentMediaItem.selectStreamUrl(selectedStream);
                                    UniversalPlayer.getInstance().softRestart();
                                    fragmentPlayer.initPlayerStateUI();
                                    return true;
                                }
                            })
                    .positiveText(R.string.select)
                    .show();
        }
    }

    public static void showStreamInfoDialog(final Activity activity) {
        String stremLink = UniversalPlayer.getInstance().getPlayingMediaItem().getAudeoLink();
        final MaterialDialog progressDialog = new MaterialDialog.Builder(activity)
                .title(R.string.fetching_info)
                .content(R.string.please_wait)
                .progress(true, 0)
                .show();
        BackendManager.getInstance().sendRequest(ServerApi.STREAM_INFO + stremLink, new IRequestCallback() {
                    @Override
                    public void onRequestSuccessed(final JSONObject jResponse) {
                        activity.runOnUiThread(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       try {
                                                           StringBuilder info = new StringBuilder();
                                                           Iterator<String> iter = jResponse.keys();
                                                           while (iter.hasNext()) {
                                                               String key = iter.next();
                                                               String value = jResponse.has(key) ? jResponse.getString(key) : "";
                                                               value = value.equals("null") || value.isEmpty() ?
                                                                       activity.getString(R.string.unknown) : value;
                                                               info.append("<b>" + GlobalUtils.upperCase(key)
                                                                       + ":</b>  " + value + "<br>");
                                                           }
                                                           new MaterialDialog.Builder(activity)
                                                                   .title(R.string.stream_info)
                                                                   .content(Html.fromHtml(info.toString()))
                                                                   .positiveText(R.string.ok)
                                                                   .show();
                                                       } catch (Exception e) {
                                                           Toast.makeText(activity, R.string.cant_get_info, Toast.LENGTH_SHORT).show();
                                                           e.printStackTrace();
                                                       }
                                                       progressDialog.dismiss();
                                                   }
                                               }
                        );
                    }

                    @Override
                    public void onRequestFailed() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });
                    }
                }

        );
    }
}
