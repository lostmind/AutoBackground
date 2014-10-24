/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground.sources;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.R;
import cw.kop.autobackground.accounts.GoogleAccount;
import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.images.AlbumFragment;
import cw.kop.autobackground.images.LocalImageFragment;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListFragment extends ListFragment {

    public static final String ADD_ENTRY = "cw.kop.autobackground.SourceListFragment.ADD_ENTRY";
    public static final String SET_ENTRY = "cw.kop.autobackground.SourceListFragment.SET_ENTRY";

    private SourceListAdapter listAdapter;
    private Context appContext;
    private Handler handler;
    private Button setButton;
    private ImageButton addButton;
    private ImageView downloadButton;
    private ImageView sortButton;
    private ImageView cycleButton;

    private ShowcaseView sourceListTutorial;
    private ShowcaseView addSourceTutorial;
    private ShowcaseView downloadTutorial;
    private ShowcaseView setTutorial;
    private ShowcaseView settingsTutorial;
    private RelativeLayout.LayoutParams buttonParams;
    private boolean setShown = false;
    private boolean tutorialShowing = false;

    private BroadcastReceiver downloadButtonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resetActionBarDownload();
        }
    };

    public SourceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = getActivity();
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sources, container, false);

        buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.setMargins(0, 0, 0, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, appContext.getResources().getDisplayMetrics())));

        addButton = (ImageButton) view.findViewById(R.id.floating_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSourceMenu();
            }

        });

        setButton = (Button) view.findViewById(R.id.set_button);
        setButton.setText("Set Wallpaper");
        setButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (setTutorial != null) {
                    hide(setTutorial);
                    showTutorial(4);
                }
                setWallpaper();
            }

        });

        downloadButton = (ImageView) getActivity().getActionBar().getCustomView().findViewById(R.id.download_wallpaper);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload();
            }
        });
        downloadButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Download images", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        sortButton = (ImageView) getActivity().getActionBar().getCustomView().findViewById(R.id.sort_sources);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceSortMenu();
            }
        });
        sortButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Sort sources", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        cycleButton = (ImageView) getActivity().getActionBar().getCustomView().findViewById(R.id.cycle_wallpaper);
        cycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleWallpaper();
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Cycling wallpaper...", Toast.LENGTH_SHORT).show();
                }
            }
        });
        cycleButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Cycle wallpaper", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        if (!AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            downloadButton.setImageResource(R.drawable.ic_action_download_white);
            sortButton.setImageResource(R.drawable.ic_action_storage_white);
            cycleButton.setImageResource(R.drawable.ic_action_refresh_white);
            addButton.setBackgroundResource(R.drawable.floating_button_white);
        }

        return view;
    }

    private void cycleWallpaper() {
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        appContext.sendBroadcast(intent);
    }

    private void startDownload() {
        listAdapter.saveData();
        if (downloadTutorial != null) {
            hide(downloadTutorial);
            showTutorial(3);
        }
        if (Downloader.download(appContext)) {
            if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                downloadButton.setImageResource(R.drawable.ic_action_cancel);
            }
            else {
                downloadButton.setImageResource(R.drawable.ic_action_cancel_white);
            }

            if (AppSettings.resetOnManualDownload() && AppSettings.useTimer() && AppSettings.getTimerDuration() > 0) {
                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getTimerDuration(), AppSettings.getTimerDuration(), pendingIntent);
            }

        }
        else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

            dialogBuilder.setTitle("Cancel download?");

            dialogBuilder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Downloader.cancel(appContext);
                    resetActionBarDownload();
                }
            });
            dialogBuilder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            dialogBuilder.show();
        }
    }

    private void showImageFragment(boolean setPath, String viewPath, int position) {
        LocalImageFragment localImageFragment = new LocalImageFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean("set_path", setPath);
        arguments.putString("view_path", viewPath);
        arguments.putInt("position", position);
        localImageFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, localImageFragment, "image_fragment")
                .addToBackStack(null)
                .commit();
    }

    private void showAlbumFragment(String type, int position, ArrayList<String> names,
                                   ArrayList<String> images, ArrayList<String> links,
                                   ArrayList<String> nums) {
        AlbumFragment albumFragment = new AlbumFragment();
        Bundle arguments = new Bundle();
        arguments.putString("type", type);
        arguments.putInt("position", position);
        arguments.putStringArrayList("album_names", names);
        arguments.putStringArrayList("album_images", images);
        arguments.putStringArrayList("album_links", links);
        arguments.putStringArrayList("album_nums", nums);
        albumFragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .add(R.id.content_frame, albumFragment, "album_fragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN) {
            if (intent != null && responseCode == Activity.RESULT_OK) {
                final String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AppSettings.setGoogleAccountName(accountName);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(appContext, accountName, "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            AppSettings.setGoogleAccount(true);
                            new PicasaAlbumTask(-1).execute();
                        }
                        catch (IOException transientEx) {
                            return null;
                        }
                        catch (UserRecoverableAuthException e) {
                            e.printStackTrace();
                            startActivityForResult(e.getIntent(), GoogleAccount.GOOGLE_AUTH_CODE);
                            return null;
                        }
                        catch (GoogleAuthException authEx) {
                            return null;
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }.execute();
            }
        }
        if (requestCode == GoogleAccount.GOOGLE_AUTH_CODE) {
            if (responseCode == Activity.RESULT_OK) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            String authToken = GoogleAuthUtil.getToken(appContext, AppSettings.getGoogleAccountName(), "oauth2:https://picasaweb.google.com/data/");
                            AppSettings.setGoogleAccountToken(authToken);
                            AppSettings.setGoogleAccount(true);
                            new PicasaAlbumTask(-1).execute();
                        }
                        catch (IOException transientEx) {
                            return null;
                        }
                        catch (UserRecoverableAuthException e) {
                            return null;
                        }
                        catch (GoogleAuthException authEx) {
                            return null;
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }.execute();
            }
        }
    }

    private void showSourceMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(appContext);

        dialog.setItems(R.array.source_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showInputDialog(AppSettings.WEBSITE,
                                "",
                                "URL",
                                "",
                                "",
                                "",
                                "",
                                "Enter website:",
                                -1);
                        break;
                    case 1:
                        showImageFragment(false, "", -1);
                        break;
                    case 2:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "Subreddit",
                                "imgur.com/r/",
                                "",
                                "",
                                "",
                                "Enter Imgur subreddit:",
                                -1);
                        break;
                    case 3:
                        showInputDialog(AppSettings.IMGUR,
                                "",
                                "Album ID",
                                "imgur.com/a/",
                                "",
                                "",
                                "",
                                "Enter Imgur album:",
                                -1);
                        break;
                    case 4:
                        if (AppSettings.getGoogleAccountName().equals("")) {
                            startActivityForResult(GoogleAccount.getPickerIntent(), GoogleAccount.GOOGLE_ACCOUNT_SIGN_IN);
                        }
                        else {
                            new PicasaAlbumTask(-1).execute();
                        }
                        break;
                    case 5:
                        showInputDialog(AppSettings.TUMBLR_BLOG,
                                "",
                                "Blog name",
                                "",
                                "",
                                ".tumblr.com",
                                "",
                                "Enter Tumblr blog:",
                                -1);
                        break;
                    case 6:
                        showInputDialog(AppSettings.TUMBLR_TAG,
                                "",
                                "Tag",
                                "",
                                "",
                                "",
                                "",
                                "Enter Tumblr tag:",
                                -1);
                        break;
                    default:
                }
            }
        });

        dialog.show();
    }

    private void showSourceSortMenu() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(appContext);

        dialog.setTitle("Sort by:");

        dialog.setItems(R.array.source_sort_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        listAdapter.sortData("use");
                        break;
                    case 1:
                        listAdapter.sortData("data");
                        break;
                    case 2:
                        listAdapter.sortData("title");
                        break;
                    case 3:
                        listAdapter.sortData("num");
                        break;
                    default:
                }

            }
        });

        dialog.show();
    }

    public void addEntry(String type, String title, String data, String num) {
        if (listAdapter.addItem(type, title, data, true, num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(appContext, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }

    }

    public void setEntry(int position, String type, String title, String path, String num) {
        if (listAdapter.setItem(position, type, title, path, true, num)) {
            listAdapter.saveData();
        }
        else {
            Toast.makeText(appContext, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputDialog(final String type, String title, String hint, final String prefix,
                                 String data, final String suffix, String num, String mainTitle,
                                 final int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);

        View dialogView = View.inflate(appContext, R.layout.add_source_dialog, null);

        builder.setView(dialogView);

        final EditText sourceTitle = (EditText) dialogView.findViewById(R.id.source_title);
        final TextView sourcePrefix = (TextView) dialogView.findViewById(R.id.source_data_prefix);
        final EditText sourceData = (EditText) dialogView.findViewById(R.id.source_data);
        final TextView sourceSuffix = (TextView) dialogView.findViewById(R.id.source_data_suffix);
        final EditText sourceNum = (EditText) dialogView.findViewById(R.id.source_num);
        TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText(mainTitle);
        sourceTitle.setText(title);
        sourcePrefix.setText(prefix);
        sourceData.setHint(hint);
        sourceData.setText(data);
        sourceSuffix.setText(suffix);
        sourceNum.setText(num);

        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Empty due to overriding action further down
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        final AlertDialog dialog = builder.create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sourceData.getText().toString().equals("") && !sourceTitle.getText().toString().equals("")) {

                    if (sourceNum.getText().toString().equals("")) {
                        sourceNum.setText("1");
                    }

                    String newTitle = sourceTitle.getText().toString();
                    String data = prefix + sourceData.getText().toString() + suffix;

                    if ((type.equals(AppSettings.WEBSITE) ||
                            type.equals(AppSettings.IMGUR) ||
                            type.equals(AppSettings.PICASA) ||
                            type.equals(AppSettings.TUMBLR_BLOG))
                            && !data.contains("http")) {
                        data = "http://" + data;
                    }
                    else if (type.equals(AppSettings.TUMBLR_TAG)) {
                        data = "Tumblr Tag: " + data;
                    }

                    if (position >= 0) {
                        String previousTitle = AppSettings.getSourceTitle(position);
                        if (listAdapter.setItem(position, type, newTitle, data.trim(), AppSettings.useSource(position), sourceNum.getText().toString())) {
                            if (!previousTitle.equals(newTitle)) {
                                AppSettings.setSourceSet(newTitle, AppSettings.getSourceSet(previousTitle));
                                Downloader.renameFiles(appContext, previousTitle, newTitle);
                            }
                            listAdapter.saveData();
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(appContext, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        if (listAdapter.addItem(type, newTitle, data.trim(), true, sourceNum.getText().toString())) {
                            listAdapter.saveData();
                            AppSettings.setSourceSet(newTitle, new HashSet<String>());
                            hide(addSourceTutorial);
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(appContext, "Error: Title in use.\nPlease use a different title.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    private void showDialogMenu(final int position) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        listAdapter.saveData();
        dialog.setTitle(AppSettings.getSourceTitle(position));

        dialog.setItems(R.array.source_edit_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                HashMap<String, String> item = listAdapter.getItem(position);
                String type = item.get("type");
                switch (which) {
                    case 0:
                        String directory;
                        if (type.equals(AppSettings.WEBSITE) ||
                                type.equals(AppSettings.IMGUR) ||
                                type.equals(AppSettings.PICASA) ||
                                type.equals(AppSettings.TUMBLR_BLOG) ||
                                type.equals(AppSettings.TUMBLR_TAG)) {
                            directory = AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(position) + " " + AppSettings.getImagePrefix();
                        }
                        else {
                            directory = AppSettings.getSourceData(position);
                        }
                        showImageFragment(false, directory, position);
                        break;
                    case 1:
                        switch (type) {
                            case AppSettings.WEBSITE:
                                showInputDialog(AppSettings.WEBSITE,
                                        AppSettings.getSourceTitle(position),
                                        "",
                                        "",
                                        AppSettings.getSourceData(position),
                                        "",
                                        "" + AppSettings.getSourceNum(position),
                                        "Edit website:",
                                        position);
                                break;
                            case AppSettings.IMGUR: {
                                String prefix = "", hint = "";
                                String data = AppSettings.getSourceData(position);
                                if (data.contains("imgur.com/a/")) {
                                    prefix = "imgur.com/a/";
                                    hint = "Album ID";
                                }
                                else if (data.contains("imgur.com/r/")) {
                                    prefix = "imgur.com/r/";
                                    hint = "Subreddit";
                                }

                                showInputDialog(AppSettings.IMGUR,
                                        AppSettings.getSourceTitle(position),
                                        hint,
                                        prefix,
                                        data.substring(data.indexOf(prefix) + prefix.length()),
                                        "",
                                        "" + AppSettings.getSourceNum(position),
                                        "Edit Imgur source:",
                                        position);
                                break;
                            }
                            case AppSettings.PICASA:
                                new PicasaAlbumTask(position).execute();
                                break;
                            case AppSettings.TUMBLR_BLOG:
                                showInputDialog(AppSettings.TUMBLR_BLOG,
                                        AppSettings.getSourceTitle(position),
                                        "Blog name",
                                        "",
                                        AppSettings.getSourceData(position),
                                        "",
                                        "" + AppSettings.getSourceNum(position),
                                        "Edit Tumblr Blog:",
                                        position);
                                break;
                            case AppSettings.TUMBLR_TAG: {
                                String data = AppSettings.getSourceData(position);

                                if (data.length() > 12) {
                                    data = data.substring(12);
                                }

                                showInputDialog(AppSettings.TUMBLR_TAG,
                                        AppSettings.getSourceTitle(position),
                                        "Tag",
                                        "",
                                        data,
                                        "",
                                        "" + AppSettings.getSourceNum(position),
                                        "Edit Tumblr Tag:",
                                        position);
                                break;
                            }
                            case AppSettings.FOLDER:
                                showImageFragment(false, "", position);
                                break;
                        }
                        break;
                    case 2:
                        if (type.equals(AppSettings.WEBSITE) ||
                                type.equals(AppSettings.IMGUR) ||
                                type.equals(AppSettings.PICASA) ||
                                type.equals(AppSettings.TUMBLR_BLOG) ||
                                type.equals(AppSettings.TUMBLR_TAG)) {
                            listAdapter.saveData();
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(appContext);

                            deleteDialog.setTitle("Delete images associated with this source?");
                            deleteDialog.setMessage("This cannot be undone.");

                            deleteDialog.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Downloader.deleteBitmaps(appContext, position);
                                    Toast.makeText(appContext, "Deleting " + AppSettings.getSourceTitle(position) + " images", Toast.LENGTH_SHORT).show();
                                    listAdapter.removeItem(position);
                                    listAdapter.saveData();
                                }
                            });
                            deleteDialog.setNeutralButton(R.string.no_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    listAdapter.removeItem(position);
                                    AppSettings.setSourceSet(AppSettings.getSourceTitle(position), new HashSet<String>());
                                    listAdapter.saveData();
                                }
                            });
                            deleteDialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });

                            deleteDialog.show();
                        }
                        else {
                            listAdapter.removeItem(position);
                            listAdapter.saveData();
                        }
                        break;
                    default:
                }

            }
        });

        dialog.show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (listAdapter == null) {
            listAdapter = new SourceListAdapter(getActivity());
            for (int i = 0; i < AppSettings.getNumSources(); i++) {
                listAdapter.addItem(AppSettings.getSourceType(i), AppSettings.getSourceTitle(i), AppSettings.getSourceData(i), AppSettings.useSource(i), "" + AppSettings.getSourceNum(i));
                Log.i("WLF", "Added: " + AppSettings.getSourceTitle(i));
            }
        }
        setListAdapter(listAdapter);

        TextView emptyText = new TextView(getActivity());
        emptyText.setText("List is empty. Please add a new source entry.");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        ((ViewGroup) getListView().getParent()).addView(emptyLayout, 0);

        getListView().setEmptyView(emptyLayout);
        getListView().setDividerHeight(1);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDialogMenu(position);
                return true;
            }
        });

        listAdapter.updateNum();

    }

    private void hide(ShowcaseView view) {
        if (view != null) {
            view.hide();
        }
    }

    private void showTutorial(int page) {

        switch (page) {
            case 0:
                View.OnClickListener websiteListListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(sourceListTutorial);
                        showTutorial(1);
                        sourceListTutorial = null;
                        Log.i("SLF", "Shown");
                    }
                };

                sourceListTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Sources List")
                        .setContentText("This is a list of your sources. \n" +
                                "These can include both sources and your \n" +
                                "own image folders. You can edit them by \n" +
                                "tapping on their boxes.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(websiteListListener)
                        .setTarget((new ViewTarget(getActivity().getActionBar().getCustomView().findViewById(R.id.action_bar_title))))
                        .build();
                sourceListTutorial.setButtonPosition(buttonParams);
                break;
            case 1:
                View.OnClickListener addWebsiteListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(addSourceTutorial);
                        showTutorial(2);
                        addSourceTutorial = null;
                    }
                };

                addSourceTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Adding Sources")
                        .setContentText(
                                "To add a new source entry, \n" +
                                        "click the plus (+) sign. \n" +
                                        "\n" +
                                        "Not all sources will work, \n" +
                                        "so if there are no images, \n" +
                                        "try a different source. \n" +
                                        "\n" +
                                        "Provided is a page \n" +
                                        "of some landscape photos \n" +
                                        "taken by Kai Lehnberg.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(addWebsiteListener)
                        .setTarget(new ViewTarget(addButton))
                        .build();
                addSourceTutorial.setButtonPosition(buttonParams);
                break;
            case 2:
                View.OnClickListener downloadListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(downloadTutorial);
                        showTutorial(3);
                        downloadTutorial = null;
                    }
                };
                downloadTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Downloading Images")
                        .setContentText("Once you have a website entered, \n" +
                                "click this download button to start \n" +
                                "downloading some images. \n" +
                                "\n" +
                                "The app will only use WiFi to \n" +
                                "download as a default. If you \n" +
                                "wish to change this setting, \n" +
                                "go into the Downloader settings \n" +
                                "and enable mobile data.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setTarget(new ViewTarget(downloadButton))
                        .setOnClickListener(downloadListener)
                        .build();
                downloadTutorial.setButtonPosition(buttonParams);
                break;
            case 3:
                View.OnClickListener setListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(setTutorial);
                        showTutorial(4);
                        setTutorial = null;
                    }
                };
                if (setButton.getVisibility() == View.VISIBLE) {
                    setTutorial = new ShowcaseView.Builder(getActivity())
                            .setContentTitle("Setting the wallpaper")
                            .setContentText("Now that it's downloading, \n" +
                                    "it's time to set the app \n" +
                                    "as your system wallpaper. \n" +
                                    "Click the set button and \n" +
                                    "hit apply on next page.")
                            .setStyle(R.style.ShowcaseStyle)
                            .setTarget(new ViewTarget(setButton))
                            .setOnClickListener(setListener)
                            .build();
                    setTutorial.setButtonPosition(buttonParams);
                    setShown = true;
                }
                else {
                    showTutorial(4);
                }
                break;
            case 4:
                if (setShown) {
                    hide(setTutorial);
                }
                View.OnClickListener settingsListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hide(settingsTutorial);
                        showTutorial(5);
                        settingsTutorial = null;
                    }
                };
                settingsTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Accessing Settings")
                        .setContentText("To open the other settings, \n" +
                                "click the entry in the top left, \n" +
                                "which opens a list of settings.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setOnClickListener(settingsListener)
                        .setTarget((new ViewTarget(getActivity().getActionBar().getCustomView().findViewById(R.id.drawer_indicator))))
                        .build();
                settingsTutorial.setButtonPosition(buttonParams);
                ((MainActivity) appContext).toggleDrawer();
                break;
            case 5:
                AppSettings.setTutorial(false, "source");
                break;
            default:
        }

    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        showDialogMenu(position);
    }

    protected void setWallpaper() {

        final Intent i = new Intent();
        if (Build.VERSION.SDK_INT >= 16) {
            i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            final String p = LiveWallpaperService.class.getPackage().getName();
            final String c = LiveWallpaperService.class.getCanonicalName();
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
        }
        else {
            i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        }

        startActivityForResult(i, 0);
    }

    @Override
    public void onDestroyView() {
        setListAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        sortButton.setVisibility(View.VISIBLE);
        downloadButton.setVisibility(View.VISIBLE);
        cycleButton.setVisibility(View.VISIBLE);

        LocalBroadcastManager.getInstance(appContext).registerReceiver(downloadButtonReceiver, new IntentFilter(Downloader.DOWNLOAD_TERMINATED));

        if (Downloader.isDownloading) {
            if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                downloadButton.setImageResource(R.drawable.ic_action_cancel);
            }
            else {
                downloadButton.setImageResource(R.drawable.ic_action_cancel_white);
            }
        }
        else {
            resetActionBarDownload();
        }

        if (isServiceRunning(LiveWallpaperService.class.getName())) {
            setButton.setVisibility(View.GONE);
        }
        else {
            setButton.setVisibility(View.VISIBLE);
        }

        if (AppSettings.useSourceTutorial() && sourceListTutorial == null && !tutorialShowing) {
            Log.i("WLF", "Showing tutorial");
            tutorialShowing = true;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(appContext);

            dialogBuilder.setMessage("Show Sources Tutorial?");

            dialogBuilder.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    showTutorial(0);
                }
            });

            dialogBuilder.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            });

            AlertDialog dialog = dialogBuilder.create();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    AppSettings.setTutorial(false, "source");
                    tutorialShowing = false;
                }
            });

            dialog.show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        listAdapter.saveData();
        sortButton.setVisibility(View.GONE);
        downloadButton.setVisibility(View.GONE);
        cycleButton.setVisibility(View.GONE);

        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(downloadButtonReceiver);
    }

    public void resetActionBarDownload() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                    downloadButton.setImageResource(R.drawable.ic_action_download);
                }
                else {
                    downloadButton.setImageResource(R.drawable.ic_action_download_white);
                }
                downloadButton.postInvalidate();
            }
        });
    }

    public void resetDownload() {
        resetActionBarDownload();
    }

    private boolean isServiceRunning(final String className) {
        final ActivityManager manager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    class PicasaAlbumTask extends AsyncTask<Void, String, Void> {

        int changePosition;
        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<String> albumImageLinks = new ArrayList<>();
        ArrayList<String> albumLinks = new ArrayList<>();
        ArrayList<String> albumNums = new ArrayList<>();

        public PicasaAlbumTask(int position) {
            changePosition = position;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Toast.makeText(appContext, values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress("Loading albums...");
            String authToken = null;
            try {
                authToken = GoogleAuthUtil.getToken(appContext, AppSettings.getGoogleAccountName(), "oauth2:https://picasaweb.google.com/data/");
            }
            catch (IOException e) {
                publishProgress("Error loading albums");
                return null;
            }
            catch (GoogleAuthException e) {
                publishProgress("Error loading albums");
                return null;
            }
            AppSettings.setGoogleAccountToken(authToken);

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet("https://picasaweb.google.com/data/feed/api/user/" + AppSettings.getGoogleAccountName());
            httpGet.setHeader("Authorization", "OAuth " + authToken);
            httpGet.setHeader("X-GData-Client", AppSettings.PICASA_CLIENT_ID);
            httpGet.setHeader("GData-Version", "2");

            InputStream inputStream = null;
            BufferedReader reader = null;
            String result = null;
            try {
                inputStream = httpClient.execute(httpGet).getEntity().getContent();
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder stringBuilder = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                result = stringBuilder.toString();

            }
            catch (Exception e) {
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Document albumDoc = Jsoup.parse(result);

            for (Element link : albumDoc.select("entry")) {
                albumNames.add(link.select("title").text());
                albumImageLinks.add(link.select("media|group").select("media|content").attr("url"));
                albumLinks.add(link.select("id").text().replace("entry", "feed"));
                albumNums.add(link.select("gphoto|numphotos").text());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            showAlbumFragment(AppSettings.PICASA, changePosition, albumNames, albumImageLinks, albumLinks, albumNums);
        }
    }

}
