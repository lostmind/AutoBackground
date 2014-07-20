package cw.kop.autobackground.notification;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import afzkl.development.colorpickerview.view.ColorPickerView;
import cw.kop.autobackground.Downloader;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 7/17/2014.
 */
public class NotificationSettingsFragment extends PreferenceFragment implements View.OnClickListener {

    private final static long CONVERT_MILLES_TO_MIN = 60000;
    private static final int SELECT_PHOTO = 4;
    private Context context;

    private ListView preferenceList;
    private RecyclerView recyclerView;
    private RelativeLayout notificationPreview;
    private ImageView notificationIcon;
    private ImageView notificationIconHighlight;
    private TextView notificationTitle;
    private TextView notificationSummary;
    private ImageView notificationTitleHighlight;
    private ImageView notificationSummaryHighlight;
    private View notificationBuffer;
    private ImageView notificationPreviewHighlight;
    private RelativeLayout optionOne;
    private RelativeLayout optionTwo;
    private RelativeLayout optionThree;
    private ImageView optionOneImage;
    private ImageView optionTwoImage;
    private ImageView optionThreeImage;
    private TextView optionOneText;
    private TextView optionTwoText;
    private TextView optionThreeText;
    private ImageView optionOneHighlight;
    private ImageView optionTwoHighlight;
    private ImageView optionThreeHighlight;
    private ShowcaseView previewTutorial;

    public NotificationSettingsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (AppSettings.useNotificationIconFile()) {
            Log.i("NSF", "Loading file");
            File image = new File(AppSettings.getNotificationIconFile());

            if (image.exists() && image.isFile()) {
                int imageSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics()));
                Picasso.with(context).load(image).resize(imageSize, imageSize).into(notificationIcon);
                Log.i("NSF", "Loading custom image");
            }

        }
        else {
            Log.i("NSF", "Loading default image");
            notificationIcon.setImageResource(AppSettings.getNotificationIcon());
        }

        notificationPreview.setBackgroundColor(AppSettings.getNotificationColor());


        if (AppSettings.getNotificationTitle().equals("Location")) {
            if (Downloader.getCurrentBitmapFile() != null) {
                notificationTitle.setText(Downloader.getCurrentBitmapFile().getAbsolutePath());
            }
            else {
                notificationTitle.setText(AppSettings.getNotificationTitle());
            }
        }
        if (AppSettings.getNotificationSummary().equals("Location")) {
            if (Downloader.getCurrentBitmapFile() != null) {
                notificationSummary.setText(Downloader.getCurrentBitmapFile().getAbsolutePath());
            }
            else {
                notificationSummary.setText(AppSettings.getNotificationSummary());
            }
        }

        notificationTitle.setTextColor(AppSettings.getNotificationTitleColor());
        notificationSummary.setTextColor(AppSettings.getNotificationSummaryColor());

        Drawable coloredImageOne = context.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(0)));
        Drawable coloredImageTwo = context.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(1)));
        Drawable coloredImageThree = context.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(2)));

        coloredImageOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(0), PorterDuff.Mode.MULTIPLY);
        coloredImageTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(1), PorterDuff.Mode.MULTIPLY);
        coloredImageThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(2), PorterDuff.Mode.MULTIPLY);

        optionOneImage.setImageDrawable(coloredImageOne);
        optionTwoImage.setImageDrawable(coloredImageTwo);
        optionThreeImage.setImageDrawable(coloredImageThree);

        optionOneText.setText(AppSettings.getNotificationOptionTitle(0));
        optionOneText.setTextColor(AppSettings.getNotificationOptionColor(0));
        optionTwoText.setText(AppSettings.getNotificationOptionTitle(1));
        optionTwoText.setTextColor(AppSettings.getNotificationOptionColor(1));
        optionThreeText.setText(AppSettings.getNotificationOptionTitle(2));
        optionThreeText.setTextColor(AppSettings.getNotificationOptionColor(2));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.notification_settings_layout, container, false);

        preferenceList = (ListView) view.findViewById(android.R.id.list);

        recyclerView = (RecyclerView) view.findViewById(R.id.notification_options_list);

        notificationPreview = (RelativeLayout) view.findViewById(R.id.notification_preview);

        notificationIcon = (ImageView) view.findViewById(R.id.notification_options_icon);

        notificationIconHighlight = (ImageView) view.findViewById(R.id.notification_icon_highlight);

        notificationTitle = (TextView) view.findViewById(R.id.notification_options_title);
        notificationSummary = (TextView) view.findViewById(R.id.notification_options_summary);

        notificationTitleHighlight = (ImageView) view.findViewById(R.id.notification_title_highlight);
        notificationSummaryHighlight = (ImageView) view.findViewById(R.id.notification_summary_highlight);

        notificationBuffer = view.findViewById(R.id.notification_options_buffer);

        notificationPreviewHighlight = (ImageView) view.findViewById(R.id.notification_preview_highlight);

        optionOne = (RelativeLayout) view.findViewById(R.id.notification_option_one);
        optionTwo = (RelativeLayout) view.findViewById(R.id.notification_option_two);
        optionThree = (RelativeLayout) view.findViewById(R.id.notification_option_three);

        optionOneImage = (ImageView) view.findViewById(R.id.notification_option_one_image);
        optionTwoImage = (ImageView) view.findViewById(R.id.notification_option_two_image);
        optionThreeImage = (ImageView) view.findViewById(R.id.notification_option_three_image);

        optionOneText = (TextView) view.findViewById(R.id.notification_option_one_text);
        optionTwoText = (TextView) view.findViewById(R.id.notification_option_two_text);
        optionThreeText = (TextView) view.findViewById(R.id.notification_option_three_text);

        optionOneHighlight = (ImageView) view.findViewById(R.id.notification_option_one_highlight);
        optionTwoHighlight = (ImageView) view.findViewById(R.id.notification_option_two_highlight);
        optionThreeHighlight = (ImageView) view.findViewById(R.id.notification_option_three_highlight);

        notificationIcon.setOnClickListener(this);

        notificationTitle.setOnClickListener(this);
        notificationSummary.setOnClickListener(this);

        notificationBuffer.setOnClickListener(this);

        optionOne.setOnClickListener(this);
        optionTwo.setOnClickListener(this);
        optionThree.setOnClickListener(this);

        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Preference tutorialPref = findPreference("show_tutorial_notification");
        tutorialPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                previewTutorial = new ShowcaseView.Builder(getActivity())
                        .setContentTitle("Notification Customization")
                        .setContentText("This is where you can change \n" +
                                "how the persistent notification looks. \n" +
                                "To customize a part, simply click on it \n" +
                                "inside this preview.")
                        .setStyle(R.style.ShowcaseStyle)
                        .setTarget(new ViewTarget(notificationPreview))
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AppSettings.setTutorial(false, "notification");
                            }
                        })
                        .build();
                return true;
            }
        });

        Log.i("NSF", "Options shown");

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_notification);
    }

    private void hide(ShowcaseView view) {
        if (view != null) {
            view.hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (AppSettings.useNotificationTutorial()) {
            previewTutorial = new ShowcaseView.Builder(getActivity())
                    .setContentTitle("Notification Customization")
                    .setContentText("This is where you can change \n" +
                            "how the persistent notification looks. \n" +
                            "To customize a part, simply click on it \n" +
                            "inside this preview.")
                    .setStyle(R.style.ShowcaseStyle)
                    .setTarget(new ViewTarget(notificationPreview))
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hide(previewTutorial);
                            AppSettings.setTutorial(false, "notification");
                        }
                    })
                    .build();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intent.putExtra("use", true);
        context.sendBroadcast(intent);
    }

    @Override
    public void onClick(View v) {

        if (previewTutorial != null) {
            previewTutorial.hide();
        }

        if (v.getId() == R.id.notification_option_one) {
            clearHighlights();
            optionOneHighlight.setVisibility(View.VISIBLE);
            showOptionList(0);
        }
        else if (v.getId() == R.id.notification_option_two) {
            clearHighlights();
            optionTwoHighlight.setVisibility(View.VISIBLE);
            showOptionList(1);
        }
        else if (v.getId() == R.id.notification_option_three) {
            clearHighlights();
            optionThreeHighlight.setVisibility(View.VISIBLE);
            showOptionList(2);
        }
        else if (v.getId() == R.id.notification_options_title) {
            clearHighlights();
            notificationTitleHighlight.setVisibility(View.VISIBLE);
            showTitlesList(4);
        }
        else if (v.getId() == R.id.notification_options_summary) {
            clearHighlights();
            notificationSummaryHighlight.setVisibility(View.VISIBLE);
            showTitlesList(5);
        }
        else if (v.getId() == R.id.notification_options_icon) {
            clearHighlights();
            notificationIconHighlight.setVisibility(View.VISIBLE);
            showIconList(6);
        }
        else if (v.getId() == R.id.notification_options_buffer) {
            clearHighlights();
            notificationPreviewHighlight.setVisibility(View.VISIBLE);
            showBackgroundColorDialog();
        }

        preferenceList.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

    }

    private void showBackgroundColorDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        dialogBuilder.setTitle("Enter background color:");

        final ColorPickerView colorPickerView = new ColorPickerView(context);
        colorPickerView.setAlphaSliderVisible(true);
        colorPickerView.setColor(AppSettings.getNotificationOptionPreviousColor());

        dialogBuilder.setView(colorPickerView);

        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                AppSettings.setNotificationColor(colorPickerView.getColor());
                notificationPreview.setBackgroundColor(AppSettings.getNotificationColor());
                clearHighlights();
                recyclerView.setAdapter(null);
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        AlertDialog dialog = dialogBuilder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                clearHighlights();
                recyclerView.setAdapter(null);
            }
        });

        dialog.show();

    }

    private int getWhiteDrawable(int drawable) {

        if (drawable == R.drawable.ic_action_copy) {
            return R.drawable.ic_action_copy_dark;
        }
        else if (drawable == R.drawable.ic_action_refresh) {
            return R.drawable.ic_action_refresh_dark;
        }
        else if (drawable == R.drawable.ic_action_discard) {
            return R.drawable.ic_action_discard_dark;
        }
        else if (drawable == R.drawable.ic_action_picture) {
            return R.drawable.ic_action_picture_dark;
        }
        else if (drawable == R.drawable.ic_action_make_available_offline) {
            return R.drawable.ic_action_make_available_offline_dark;
        }
        else if (drawable == R.drawable.ic_action_back) {
            return R.drawable.ic_action_back_dark;
        }
        else if (drawable == R.drawable.ic_action_share) {
            return R.drawable.ic_action_share_dark;
        }
        else if (drawable == R.drawable.ic_action_backspace) {
            return R.drawable.ic_action_backspace_dark;
        }
        else {
            return drawable;
        }
    }

    private void clearHighlights() {

        notificationIconHighlight.setVisibility(View.GONE);
        notificationPreviewHighlight.setVisibility(View.GONE);
        notificationTitleHighlight.setVisibility(View.GONE);
        notificationSummaryHighlight.setVisibility(View.GONE);
        optionOneHighlight.setVisibility(View.GONE);
        optionTwoHighlight.setVisibility(View.GONE);
        optionThreeHighlight.setVisibility(View.GONE);

        recyclerView.setVisibility(View.GONE);
        preferenceList.setVisibility(View.VISIBLE);
    }

    private void showIconList(int position) {

        String[] iconTitles = context.getResources().getStringArray(R.array.notification_icon);
        String[] iconSummaries = context.getResources().getStringArray(R.array.notification_icon_descriptions);
        TypedArray iconIcons;
        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            iconIcons = context.getResources().obtainTypedArray(R.array.notification_icon_icons);
        }
        else {
            iconIcons = context.getResources().obtainTypedArray(R.array.notification_icon_icons_dark);
        }

        ArrayList<NotificationOptionData> optionsList = new ArrayList<NotificationOptionData>();

        for (int i = 0; i < iconTitles.length; i++) {
            optionsList.add(new NotificationOptionData(iconTitles[i], iconSummaries[i], iconIcons.getResourceId(i, -1), iconSummaries[i]));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("Application")) {
                    AppSettings.setNotificationIcon(R.drawable.app_icon);
                    notificationIcon.setImageResource(R.drawable.app_icon);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("Image")) {
                    AppSettings.setNotificationIcon(drawable);
                    int imageSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics()));
                    Picasso.with(context).load(Downloader.getCurrentBitmapFile()).resize(imageSize, imageSize).into(notificationIcon);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("None")) {
                    AppSettings.setNotificationIcon(R.drawable.icon_blank);
                    notificationIcon.setImageResource(R.drawable.icon_blank);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("Custom")) {
                    Intent imageIntent = new Intent(Intent.ACTION_PICK);
                    imageIntent.setType("image/*");
                    startActivityForResult(imageIntent, SELECT_PHOTO);
                }

            }
        };

        NotificationListAdapter titlesAdapter = new NotificationListAdapter(optionsList, position, listener);

        recyclerView.setAdapter(titlesAdapter);

        iconIcons.recycle();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = context.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            File image = new File(filePath);

            if (image.exists() && image.isFile()) {
                AppSettings.setNotificationIconFile(filePath);
                AppSettings.setUseNotificationIconFile(true);
                int imageSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics()));
                Picasso.with(context).load(image).resize(imageSize, imageSize).into(notificationIcon);
            }
            clearHighlights();
            recyclerView.setAdapter(null);
        }

    }

    private void showTitlesList(int position) {

        String[] titleTitles = context.getResources().getStringArray(R.array.notification_titles);
        String[] titleSummaries = context.getResources().getStringArray(R.array.notification_titles_descriptions);
        TypedArray titlesIcons;
        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            titlesIcons = context.getResources().obtainTypedArray(R.array.notification_titles_icons);
        }
        else {
            titlesIcons = context.getResources().obtainTypedArray(R.array.notification_titles_icons_dark);
        }

        ArrayList<NotificationOptionData> optionsList = new ArrayList<NotificationOptionData>();

        for (int i = 0; i < titleTitles.length; i++) {
            optionsList.add(new NotificationOptionData(titleTitles[i], titleSummaries[i], titlesIcons.getResourceId(i, -1), titleSummaries[i]));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("None")){
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    switch (position) {
                        case 4:
                            AppSettings.setNotificationTitle("");
                            notificationTitle.setText("");
                            break;
                        case 5:
                                AppSettings.setNotificationSummary("");
                                notificationSummary.setText("");
                            break;
                    }
                }
                else if (title.equals("Custom")) {
                    showDialogForText(position, drawable);
                }
                else {
                    showTitleColorDialog(position, title, drawable);
                }
            }
        };

        NotificationListAdapter titlesAdapter = new NotificationListAdapter(optionsList, position, listener);

        recyclerView.setAdapter(titlesAdapter);

        titlesIcons.recycle();

    }

    private void showOptionList(int position) {

        String[] optionsTitles = context.getResources().getStringArray(R.array.notification_options);
        String[] optionsSummaries = context.getResources().getStringArray(R.array.notification_options_descriptions);
        TypedArray optionsIcons;
        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            optionsIcons = context.getResources().obtainTypedArray(R.array.notification_options_icons);
        }
        else {
            optionsIcons = context.getResources().obtainTypedArray(R.array.notification_options_icons_dark);
        }

        ArrayList<NotificationOptionData> optionsList = new ArrayList<NotificationOptionData>();

        for (int i = 0; i < optionsTitles.length; i++) {
            optionsList.add(new NotificationOptionData(optionsTitles[i], optionsSummaries[i], optionsIcons.getResourceId(i, -1), optionsTitles[i]));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("None")) {
                    title = "";
                    drawable = R.color.TRANSPARENT_BACKGROUND;

                    clearHighlights();
                    AppSettings.setNotificationOptionTitle(position, title);
                    AppSettings.setNotificationOptionDrawable(position, drawable);
                    recyclerView.setAdapter(null);

                    switch (position) {
                        case 0:
                            optionOneText.setText(title);
                            optionOneImage.setImageResource(drawable);
                            break;
                        case 1:
                            optionTwoText.setText(title);
                            optionTwoImage.setImageResource(drawable);
                            break;
                        case 2:
                            optionThreeText.setText(title);
                            optionThreeImage.setImageResource(drawable);
                            break;
                    }
                }
                else if (title.equals("Pin")) {
                    showDialogForPin(position, title, drawable);
                }
                else {
                    showOptionColorDialog(position, title, drawable);
                }
            }
        };

        NotificationListAdapter optionsAdapter = new NotificationListAdapter(optionsList, position, listener);

        recyclerView.setAdapter(optionsAdapter);


        optionsIcons.recycle();

    }

    private void showTitleColorDialog(final int position, final String title, final int drawable) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle("Enter text color:");

        final ColorPickerView colorPickerView = new ColorPickerView(context);
        colorPickerView.setAlphaSliderVisible(true);
        colorPickerView.setColor(AppSettings.getNotificationOptionPreviousColor());

        dialog.setView(colorPickerView);

        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                clearHighlights();
                recyclerView.setAdapter(null);
                switch (position) {
                    case 4:
                        AppSettings.setNotificationTitleColor(colorPickerView.getColor());
                        if (title.equals("Application")) {
                            AppSettings.setNotificationTitle("AutoBackground");
                            notificationTitle.setText("AutoBackground");
                        }
                        else if (title.equals("Location")) {
                            AppSettings.setNotificationTitle(title);
                            if (Downloader.getCurrentBitmapFile() != null) {
                                notificationTitle.setText(Downloader.getCurrentBitmapFile().getAbsolutePath());
                            }
                            else {
                                notificationTitle.setText(title);
                            }
                        }
                        else if (title.equals("None")) {
                            AppSettings.setNotificationTitle("");
                            notificationTitle.setText("");
                        }
                        else {
                            AppSettings.setNotificationTitle(title);
                            notificationTitle.setText(title);
                        }
                        notificationTitle.setTextColor(AppSettings.getNotificationTitleColor());
                        break;
                    case 5:
                        AppSettings.setNotificationSummaryColor(colorPickerView.getColor());
                        if (title.equals("Application")) {
                            AppSettings.setNotificationTitle("AutoBackground");
                            notificationSummary.setText("AutoBackground");
                        }
                        else if (title.equals("None")) {
                            AppSettings.setNotificationSummary("");
                            notificationSummary.setText("");
                        }
                        else {
                            AppSettings.setNotificationSummary(title);
                            notificationSummary.setText(title);
                        }
                        notificationSummary.setTextColor(AppSettings.getNotificationSummaryColor());
                        break;
                }
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dialog.show();
    }

    private void showOptionColorDialog(final int position, final String title, final int drawable) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle("Enter icon and text color:");

        final ColorPickerView colorPickerView = new ColorPickerView(context);
        colorPickerView.setAlphaSliderVisible(true);
        colorPickerView.setColor(AppSettings.getNotificationOptionPreviousColor());

        dialog.setView(colorPickerView);

        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                clearHighlights();
                AppSettings.setNotificationOptionTitle(position, title);
                AppSettings.setNotificationOptionDrawable(position, drawable);
                AppSettings.setNotificationOptionColor(position, colorPickerView.getColor());
                recyclerView.setAdapter(null);

                int whiteDrawable = getWhiteDrawable(drawable);

                switch (position) {
                    case 0:
                        optionOneText.setText(title);
                        optionOneText.setTextColor(AppSettings.getNotificationOptionColor(0));
                        Drawable coloredDrawableOne = context.getResources().getDrawable(whiteDrawable);
                        coloredDrawableOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(0), PorterDuff.Mode.MULTIPLY);
                        optionOneImage.setImageDrawable(coloredDrawableOne);
                        break;
                    case 1:
                        optionTwoText.setText(title);
                        optionTwoText.setTextColor(AppSettings.getNotificationOptionColor(1));
                        Drawable coloredDrawableTwo = context.getResources().getDrawable(whiteDrawable);
                        coloredDrawableTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(1), PorterDuff.Mode.MULTIPLY);
                        optionTwoImage.setImageDrawable(coloredDrawableTwo);
                        break;
                    case 2:
                        optionThreeText.setText(title);
                        optionThreeText.setTextColor(AppSettings.getNotificationOptionColor(2));
                        Drawable coloredDrawableThree = context.getResources().getDrawable(whiteDrawable);
                        coloredDrawableThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(2), PorterDuff.Mode.MULTIPLY);
                        optionThreeImage.setImageDrawable(coloredDrawableThree);
                        break;
                }
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dialog.show();

    }

    private void showDialogForText(final int position, final int drawable) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle("Enter text:");

        final EditText input = new EditText(context);
        dialog.setView(input);

        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                showTitleColorDialog(position, input.getText().toString(), drawable);
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dialog.show();
    }

    private void showDialogForPin(final int position, final String title, final int drawable) {

//        int themeId;
//
//        if(AppSettings.getTheme() == R.style.FragmentLightTheme) {
//            themeId = R.style.LightDialogTheme;
//        }
//        else {
//            themeId = R.style.DarkDialogTheme;
//        }

        AppSettings.setIntervalDuration(0);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        dialogBuilder.setItems(R.array.pin_entry_menu, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case 0:
                        AppSettings.setPinDuration(0);
                        break;
                    case 1:
                        AppSettings.setPinDuration(5 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 2:
                        AppSettings.setPinDuration(15 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 3:
                        AppSettings.setPinDuration(30 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 4:
                        AppSettings.setPinDuration(AlarmManager.INTERVAL_HOUR);
                        break;
                    case 5:
                        AppSettings.setPinDuration(2 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 6:
                        AppSettings.setPinDuration(6 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 7:
                        AppSettings.setPinDuration(AlarmManager.INTERVAL_HALF_DAY);
                        break;
                    default:
                }

                showOptionColorDialog(position, title, drawable);

            }
        });

        AlertDialog dialog = dialogBuilder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
            }

        });

        dialog.show();

    }

}