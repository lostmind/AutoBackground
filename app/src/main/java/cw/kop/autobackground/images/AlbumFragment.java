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

package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class AlbumFragment extends Fragment implements ListView.OnItemClickListener {

    private Context appContext;
    private ListView albumListView;
    private AlbumAdapter albumAdapter;

    private String type;
    private int changePosition;
    private ArrayList<String> albumNames;
    private ArrayList<String> albumImages;
    private ArrayList<String> albumLinks;
    private ArrayList<String> albumNums;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        type = bundle.getString("type");
        changePosition = bundle.getInt("position");
        albumNames = bundle.getStringArrayList("album_names");
        albumImages = bundle.getStringArrayList("album_images");
        albumLinks = bundle.getStringArrayList("album_links");
        albumNums = bundle.getStringArrayList("album_nums");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.album_list_layout,
                container,
                false);

        albumListView = (ListView) view.findViewById(R.id.album_listview);

        TextView emptyText = new TextView(getActivity());
        emptyText.setText("No Albums");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(getActivity());
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            albumListView.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else {
            albumListView.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }

        ((ViewGroup) albumListView.getParent()).addView(emptyLayout, 0);

        albumListView.setEmptyView(emptyLayout);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (albumAdapter == null) {
            albumAdapter = new AlbumAdapter(appContext, albumNames, albumImages, albumLinks);
        }

        albumListView.setAdapter(albumAdapter);
        albumListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {

        Intent returnEntryIntent = new Intent();
        if (changePosition > -1) {
            returnEntryIntent.setAction(SourceListFragment.SET_ENTRY);
            returnEntryIntent.putExtra("position", changePosition);
        }
        else {
            returnEntryIntent.setAction(SourceListFragment.ADD_ENTRY);
        }

        returnEntryIntent.putExtra("type", type);
        returnEntryIntent.putExtra("title", albumNames.get(positionInList));
        returnEntryIntent.putExtra("data", albumLinks.get(positionInList));
        returnEntryIntent.putExtra("num", albumNums.get(positionInList));

        LocalBroadcastManager.getInstance(appContext).sendBroadcast(returnEntryIntent);

        getActivity().onBackPressed();
    }
}
