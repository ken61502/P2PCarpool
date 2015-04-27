package edu.cmu.group08.p2pcarpool;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;

import edu.cmu.group08.p2pcarpool.fragment.HostFragment;
import edu.cmu.group08.p2pcarpool.fragment.SearchFragment;
import edu.cmu.group08.p2pcarpool.fragment.HomeFragment;
import edu.cmu.group08.p2pcarpool.fragment.NavigationDrawerFragment;
import edu.cmu.group08.p2pcarpool.fragment.ProfileFragment;


public class HomeActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
                   ProfileFragment.OnFragmentInteractionListener,
                   SearchFragment.OnGroupedSelectedListener {

    private static final String TAG = "HomeActivity";
    /**
     * Components for UI.
     */
    private Button mSearchButton = null;
    private Button mSettingButton = null;
    private Button mGroupRoomButton = null;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                HomeFragment homeFragment = new HomeFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, homeFragment)
                        .commit();
                break;
            case 1:
                HostFragment hostFragment = new HostFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, hostFragment)
                        .commit();
                break;
            case 2:
                ProfileFragment profileFragment = new ProfileFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, profileFragment)
                        .commit();
               break;
            case 3:
                SearchFragment searchFragment = new SearchFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, searchFragment)
                        .commit();
                break;
            default:
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section4);
                break;
            case 3:
                mTitle = getString(R.string.title_section2);
                break;
            case 4:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.home, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onGroupedSelected(Integer id) {

    }
}
