/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.main;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.about.AboutActivity;
import ca.rmen.android.scrumchatter.export.DBExport;
import ca.rmen.android.scrumchatter.export.FileExport;
import ca.rmen.android.scrumchatter.export.MeetingsExport;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.meeting.list.MeetingsListFragment;
import ca.rmen.android.scrumchatter.member.list.Members;
import ca.rmen.android.scrumchatter.member.list.MembersListFragment;
import ca.rmen.android.scrumchatter.provider.DBImport;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.team.Teams.Team;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment.ScrumChatterDialogButtonListener;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment.ScrumChatterDialogItemListener;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragmentFactory;
import ca.rmen.android.scrumchatter.ui.ScrumChatterInputDialogFragment.ScrumChatterDialogInputListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * The main screen of the app. Part of this code was generated by the ADT
 * plugin.
 */
public class MainActivity extends SherlockFragmentActivity implements ActionBar.TabListener, ScrumChatterDialogButtonListener, ScrumChatterDialogItemListener, // NO_UCD (use default)
        ScrumChatterDialogInputListener { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + MainActivity.class.getSimpleName();
    private static final String EXTRA_IMPORT_URI = "import_uri";
    private static final String EXTRA_IMPORT_RESULT = "import_result";
    private static final String ACTION_IMPORT_COMPLETE = "action_import_complete";
    private static final int ACTIVITY_REQUEST_CODE_IMPORT = 1;
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Teams mTeams = new Teams(this);
    private Meetings mMeetings = new Meetings(this);
    private Members mMembers = new Members(this);
    private Team mTeam = null;
    private int mTeamCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //StrictMode.setThreadPolicy(new ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().penaltyDeath().build());
        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        onTeamChanged();
        // If our activity was opened by choosing a file from a mail attachment, file browser, or other program, 
        // import the database from this file.
        Intent intent = getIntent();
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) importDB(intent.getData());
        }
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        getContentResolver().registerContentObserver(TeamColumns.CONTENT_URI, true, mContentObserver);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPrefsListener);
        IntentFilter filter = new IntentFilter(ACTION_IMPORT_COMPLETE);
        registerReceiver(mBroadcastReceiver, filter);
    }


    @Override
    protected void onResumeFragments() {
        Log.v(TAG, "onResumeFragments: intent = " + getIntent());
        super.onResumeFragments();
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Uri importUri = getIntent().getExtras().getParcelable(EXTRA_IMPORT_URI);
            if (importUri != null) {
                getIntent().removeExtra(EXTRA_IMPORT_URI);
                importDB(importUri);
            }
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPrefsListener);
        getContentResolver().unregisterContentObserver(mContentObserver);
        unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.v(TAG, "onPrepareOptionsMenu " + menu);
        // Only enable the "delete team" menu item if we have at least two teams.
        MenuItem deleteItem = menu.findItem(R.id.action_team_delete);
        deleteItem.setEnabled(mTeamCount > 1);
        // Add the current team name to the delete and rename menu items
        if (mTeam != null) {
            deleteItem.setTitle(getString(R.string.action_team_delete_name, mTeam.teamName));
            MenuItem renameItem = menu.findItem(R.id.action_team_rename);
            renameItem.setTitle(getString(R.string.action_team_rename_name, mTeam.teamName));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_team_switch:
                mTeams.selectTeam(mTeam);
                return true;
            case R.id.action_team_rename:
                mTeams.promptRenameTeam(mTeam);
                return true;
            case R.id.action_team_delete:
                mTeams.confirmDeleteTeam(mTeam);
                return true;
            case R.id.action_import:
                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.setType("file/*");
                startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.action_import)), ACTIVITY_REQUEST_CODE_IMPORT);
                return true;
            case R.id.action_share:
                // Build a chooser dialog for the file format.
                ScrumChatterDialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title),
                        getResources().getStringArray(R.array.export_choices), -1, R.id.action_share);
                return true;
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", intent = " + intent);
        if (requestCode == ACTIVITY_REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            if (intent == null || intent.getData() == null) {
                Toast.makeText(this, R.string.import_result_no_file, Toast.LENGTH_SHORT).show();
                return;
            }
            final String filePath = intent.getData().getPath();
            if (TextUtils.isEmpty(filePath)) {
                Toast.makeText(this, R.string.import_result_no_file, Toast.LENGTH_SHORT).show();
                return;
            }
            final File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, getString(R.string.import_result_file_does_not_exist, file.getName()), Toast.LENGTH_SHORT).show();
                return;
            }
            getIntent().putExtra(EXTRA_IMPORT_URI, Uri.fromFile(file));
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }


    /**
     * Import the given database file. This will replace the current database.
     */
    private void importDB(final Uri uri) {
        Bundle extras = new Bundle(1);
        extras.putParcelable(EXTRA_IMPORT_URI, uri);
        ScrumChatterDialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                getString(R.string.import_confirm_message, uri.getEncodedPath()), R.id.action_import, extras);
    }

    /**
     * Share a file using an intent chooser.
     * 
     * @param fileExport The object responsible for creating the file to share.
     */
    private void shareFile(final FileExport fileExport) {
        final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.progress_dialog_message), true);
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return fileExport.export();
            }

            @Override
            protected void onPreExecute() {
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Boolean success) {
                progressDialog.dismiss();
                if (!success) Toast.makeText(MainActivity.this, R.string.export_error, Toast.LENGTH_LONG).show();
            }
        };
        asyncTask.execute();
    }

    /**
     * Called when the current team was changed. Update our cache of the current team and update the ui (menu items, action bar title).
     */
    private void onTeamChanged() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                mTeam = mTeams.getCurrentTeam();
                // If for some reason we have no current team, select any available team
                // Should not really happen, so perhaps this check should be removed?
                if (mTeam == null) {
                    mTeams.selectFirstTeam();
                }
                mTeamCount = mTeams.getTeamCount();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                // If the user has renamed the default team or added other teams, show the current team name in the title
                if (mTeamCount > 1 || (mTeam != null && !mTeam.teamName.equals(Constants.DEFAULT_TEAM_NAME))) getSupportActionBar().setTitle(mTeam.teamName);
                // otherwise the user doesn't care about team management: just show the app title.
                else
                    getSupportActionBar().setTitle(R.string.app_name);
                supportInvalidateOptionsMenu();
            }
        };
        task.execute();
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked: actionId = " + actionId + ", extras = " + extras);
        if (actionId == R.id.action_delete_meeting) {
            long meetingId = extras.getLong(Meetings.EXTRA_MEETING_ID);
            mMeetings.delete(meetingId);
        } else if (actionId == R.id.btn_delete) {
            long memberId = extras.getLong(Members.EXTRA_MEMBER_ID);
            mMembers.deleteMember(memberId);
        } else if (actionId == R.id.action_team_delete) {
            Uri teamUri = extras.getParcelable(Teams.EXTRA_TEAM_URI);
            mTeams.deleteTeam(teamUri);
        } else if (actionId == R.id.action_import) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                private String mDialogTag;
                private ScrumChatterDialogFragment mDialog;

                @Override
                protected void onPreExecute() {
                    mDialog = ScrumChatterDialogFragmentFactory.showProgressDialog(MainActivity.this, getString(R.string.progress_dialog_message),
                            PROGRESS_DIALOG_FRAGMENT_TAG);
                    mDialogTag = mDialog.getTag();
                    Log.v(TAG, "dialog tag is " + mDialogTag);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    boolean result = false;
                    try {
                        Log.v(TAG, "Importing db from " + uri);
                        DBImport.importDB(MainActivity.this, uri);
                        result = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error importing db: " + e.getMessage(), e);
                    }
                    Intent intent = new Intent(ACTION_IMPORT_COMPLETE);
                    intent.putExtra(EXTRA_IMPORT_RESULT, result);
                    sendBroadcast(intent);
                    return null;
                }
            };
            task.execute();
        }
    }


    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId = " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        if (actionId == R.id.action_team_switch) {
            mTeams.switchTeam(choices, which);
        } else if (actionId == R.id.action_share) {
            FileExport fileExport = null;
            if (getString(R.string.export_format_excel).equals(choices[which])) fileExport = new MeetingsExport(MainActivity.this);
            else if (getString(R.string.export_format_db).equals(choices[which])) fileExport = new DBExport(MainActivity.this);
            shareFile(fileExport);
        }
    }

    @Override
    public void onInputEntered(int actionId, String input, Bundle extras) {
        Log.v(TAG, "onInputEntered: actionId = " + actionId + ", input = " + input + ", extras = " + extras);
        if (actionId == R.id.action_new_member) {
            long teamId = extras.getLong(Teams.EXTRA_TEAM_ID);
            mMembers.createMember(teamId, input);
        } else if (actionId == R.id.action_team) {
            mTeams.createTeam(input);
        } else if (actionId == R.id.action_team_rename) {
            Uri teamUri = extras.getParcelable(Teams.EXTRA_TEAM_URI);
            mTeams.renameTeam(teamUri, input);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = null;
            if (position == 1) fragment = new MembersListFragment();
            else
                fragment = new MeetingsListFragment();
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section_meetings).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section_team).toUpperCase(l);
            }
            return null;
        }
    }

    private ContentObserver mContentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            onTeamChanged();
        }
    };

    private OnSharedPreferenceChangeListener mSharedPrefsListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            onTeamChanged();
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: intent = " + intent);
            if (ACTION_IMPORT_COMPLETE.equals(intent.getAction())) {
                Boolean result = intent.getExtras().getBoolean(EXTRA_IMPORT_RESULT);
                ScrumChatterDialogFragment dialogFragment = (ScrumChatterDialogFragment) getSupportFragmentManager().findFragmentByTag(
                        PROGRESS_DIALOG_FRAGMENT_TAG);
                Log.v(TAG, "DialogFragment with tag " + dialogFragment);
                if (dialogFragment != null) dialogFragment.dismiss();
                Toast.makeText(MainActivity.this, result ? R.string.import_result_success : R.string.import_result_failed, Toast.LENGTH_SHORT).show();
            }


        }
    };



}
