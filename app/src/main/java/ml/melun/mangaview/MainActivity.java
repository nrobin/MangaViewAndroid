package ml.melun.mangaview;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.mainAdapter;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WAKE_LOCK;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Preference p;
    int startTab;
    //variables
    private ViewFlipper contentHolder;
    FloatingActionButton advSearchBtn;
    TextView noresult;
    private EditText searchBox;
    public Context context = this;
    ProgressDialog pd;
    Search search;
    TitleAdapter searchAdapter, recentAdapter, favoriteAdapter, offlineAdapter;
    RecyclerView searchResult, recentResult, favoriteResult, savedList, mainRecycler;
    private int version;
    int mode = 0;
    int selectedPosition=-1;
    MenuItem versionItem;
    String homeDirStr;
    SwipyRefreshLayout swipe;
    Boolean dark;
    Intent viewer;
    Spinner searchMode;
    NavigationView navigationView;
    NotificationManagerCompat notificationManagerc;
    NotificationManager notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        p.init(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // toolbar.getRootView().setBackgroundColor(getResources().getColor(R.color.colorDark));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if(dark) {
            int[][] states = new int[][]{
                    new int[]{-android.R.attr.state_enabled}, // disabled
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{-android.R.attr.state_checked}, // unchecked
                    new int[]{android.R.attr.state_pressed}  // pressed
            };

            int[] colors = new int[]{
                    Color.parseColor("#565656"),
                    Color.parseColor("#a2a2a2"),
                    Color.WHITE,
                    Color.WHITE
            };
            ColorStateList colorStateList = new ColorStateList(states, colors);
            navigationView.setItemTextColor(colorStateList);
        }

        //custom var init starts here
        contentHolder = this.findViewById(R.id.contentHolder);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //SharedPreferences preferences = getSharedPreferences("mangaView",MODE_PRIVATE);

        homeDirStr = p.getHomeDir();

        versionItem = navigationView.getMenu().findItem(R.id.nav_version_display);
        versionItem.setTitle("v."+version);
        swipe = this.findViewById(R.id.searchSwipe);
        //check for permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck== PackageManager.PERMISSION_DENIED){
            // 권한 없음
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE},
                        132322);
            }
        }else{
            //
        }
        savedList = findViewById(R.id.savedList);
        //show downloaded manga list


        //set startTab and refresh views
        startTab = p.getStartTab();
        contentHolder.setDisplayedChild(startTab);
        refreshViews(getTabId(startTab));
        navigationView.getMenu().getItem(startTab).setChecked(true);

        //check update upon startup
        updateCheck u = new updateCheck();
        u.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //check for notice
        noticeCheck n = new noticeCheck();
        n.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel("UtaiteBox Player", "UtaiteBox Player", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("channel description"); notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN); notificationChannel.enableVibration(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);
        }else{
            notificationManagerc = NotificationManagerCompat.from(MainActivity.this);
        }
    }

    public int getTabId(int i){
        switch(i){
            case 0:
                return(R.id.nav_main);
            case 1:
                return(R.id.nav_search);
            case 2:
                return(R.id.nav_recent);
            case 3:
                return(R.id.nav_favorite);
            case 4:
                return(R.id.nav_download);
        }
        return 0;
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(contentHolder.getDisplayedChild()==startTab){
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                MainActivity.super.onBackPressed();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(this,R.style.darkDialog);
                else builder = new AlertDialog.Builder(this);
                builder.setMessage("정말로 종료 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener).show();
            }else{
                contentHolder.setDisplayedChild(startTab);
                navigationView.getMenu().getItem(startTab).setChecked(true);
                refreshViews(getTabId(startTab));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(context, SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        }else if(id == R.id.action_debug){
            Intent debug = new Intent(context, DebugActivity.class);
            startActivity(debug);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_main) {
            // Handle the main action
            contentHolder.setDisplayedChild(0);
            mode = 0;
        } else if (id == R.id.nav_search) {
            // Handle the search action
            contentHolder.setDisplayedChild(1);
            mode = 1;
        }else if(id==R.id.nav_recent) {
            // Handle the recent action
            contentHolder.setDisplayedChild(2);
            mode = 2;
        }else if(id==R.id.nav_favorite) {
            // Handle the favorite action
            contentHolder.setDisplayedChild(3);
            mode = 3;
        }else if(id==R.id.nav_download){
            contentHolder.setDisplayedChild(4);
            mode = 4;
        }else{
            //don't refresh views
            if(id==R.id.nav_update) {
                //check update
                updateCheck u = new updateCheck();
                u.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else if(id==R.id.nav_notice){
                Intent noticesIntent = new Intent(context, NoticesActivity.class);
                startActivity(noticesIntent);
                return true;
            }else if(id==R.id.nav_kakao){
                Toast.makeText(getApplicationContext(), "오픈톡방에 참가합니다.", Toast.LENGTH_LONG).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/gL4yY57"));
                startActivity(browserIntent);
            }else if(id==R.id.nav_settings){
                Intent settingIntent = new Intent(context, SettingsActivity.class);
                startActivity(settingIntent);
                return true;
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        refreshViews(id);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshViews(int id){
        //set views according to selected layout
        if(id==R.id.nav_main){
            //main content
            // 최근 추가된 만화
            mainRecycler = this.findViewById(R.id.main_recycler);
            mainAdapter mainadapter = new mainAdapter(context);
            mainRecycler.setLayoutManager(new LinearLayoutManager(context));
            mainRecycler.setAdapter(mainadapter);
            mainadapter.setMainClickListener(new mainAdapter.onItemClick() {
                @Override
                public void clickedManga(Manga m) {
                    //get title from manga m and start intent for manga m
                    //getTitleFromManga intentStarter = new getTitleFromManga();
                    //intentStarter.execute(m);
                    switch (p.getViewerType()){
                        case 0:
                        case 2:
                            viewer = new Intent(context, ViewerActivity.class);
                            break;
                        case 1:
                            viewer = new Intent(context, ViewerActivity2.class);
                            break;
                    }
                    viewer.putExtra("manga",new Gson().toJson(m));
                    startActivity(viewer);
                }

                @Override
                public void clickedTag(String t) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t);
                    i.putExtra("mode",2);
                    startActivity(i);
                }

                @Override
                public void clickedName(int t) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t+"");
                    i.putExtra("mode",3);
                    startActivity(i);
                }

                @Override
                public void clickedRelease(int t) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t+"");
                    i.putExtra("mode",4);
                    startActivity(i);
                }

                @Override
                public void clickedMoreUpdated() {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("mode",5);
                    startActivity(i);
                }
            });

        }else if(id==R.id.nav_search){
            //search content
            noresult = this.findViewById(R.id.noResult);
            searchBox = this.findViewById(R.id.searchBox);
            searchResult = this.findViewById(R.id.searchResult);
            searchResult.setLayoutManager(new LinearLayoutManager(this));
            searchMode = this.findViewById(R.id.searchMode);
            advSearchBtn = this.findViewById(R.id.advSearchBtn);
            if(dark) searchMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);

            advSearchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent advSearch = new Intent(context, AdvSearchActivity.class);
                    startActivity(advSearch);
                }
            });

            searchBox.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode ==KeyEvent.KEYCODE_ENTER){
                        String query = searchBox.getText().toString();
                        if(query.length()>0) {
                            swipe.setRefreshing(true);
                            if(searchAdapter != null) searchAdapter.removeAll();
                            else searchAdapter = new TitleAdapter(context);
                            search = new Search(query,searchMode.getSelectedItemPosition());
                            searchManga sm = new searchManga();
                            sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                        return true;
                    }
                    return false;
                }
            });

            swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if(search==null) swipe.setRefreshing(false);
                    else {
                        if (!search.isLast()) {
                            searchManga sm = new searchManga();
                            sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else swipe.setRefreshing(false);
                    }
                }
            });
        }else if(id==R.id.nav_recent){
            recentResult = this.findViewById(R.id.recentList);
            recentAdapter = new TitleAdapter(context);
            recentAdapter.addData(p.getRecent());
            recentResult.setLayoutManager(new LinearLayoutManager(this));
            recentResult.setAdapter(recentAdapter);
            recentAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onLongClick(View view, int position) {
                    //longclick
                    popup(view, position, recentAdapter.getItem(position), 1);
                }

                @Override
                public void onResumeClick(int position, int id) {
                    selectedPosition = position;
                    p.addRecent(recentAdapter.getItem(position));
                    switch (p.getViewerType()){
                        case 0:
                        case 2:
                            viewer = new Intent(context, ViewerActivity.class);
                            break;
                        case 1:
                            viewer = new Intent(context, ViewerActivity2.class);
                            break;
                    }
                    viewer.putExtra("manga",new Gson().toJson(new Manga(id,"","")));
                    viewer.putExtra("recent",true);
                    startActivityForResult(viewer, 2);
                }

                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    Title selected = recentAdapter.getItem(position);
                    selectedPosition = position;
                    p.addRecent(selected);
                    Intent episodeView = new Intent(context, EpisodeActivity.class);
                    episodeView.putExtra("title", new Gson().toJson(selected));
                    episodeView.putExtra("recent",true);
                    startActivityForResult(episodeView,2);
                }
            });
        }else if(id==R.id.nav_favorite){
            favoriteResult = this.findViewById(R.id.favoriteList);
            favoriteAdapter = new TitleAdapter(context);
            favoriteAdapter.addData(p.getFavorite());
            favoriteResult.setLayoutManager(new LinearLayoutManager(this));
            favoriteResult.setAdapter(favoriteAdapter);
            favoriteAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onResumeClick(int position, int id) {
                    switch (p.getViewerType()){
                        case 0:
                        case 2:
                            viewer = new Intent(context, ViewerActivity.class);
                            break;
                        case 1:
                            viewer = new Intent(context, ViewerActivity2.class);
                            break;
                    }
                    viewer.putExtra("manga",new Gson().toJson(new Manga(id,"","")));
                    startActivity(viewer);
                }

                @Override
                public void onLongClick(View view, int position) {
                    popup(view, position, favoriteAdapter.getItem(position), 2);
                }

                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    Title selected = favoriteAdapter.getItem(position);
                    p.addRecent(selected);
                    //start intent for result : has to know if favorite has been removed or not
                    Intent episodeView = new Intent(context, EpisodeActivity.class);
                    episodeView.putExtra("position", position);
                    episodeView.putExtra("title", new Gson().toJson(selected));
                    episodeView.putExtra("favorite",true);
                    selectedPosition = position;
                    startActivityForResult(episodeView,1);
                }
            });
        }else if(id==R.id.nav_download){
            //downloaded list
            //그냥 코드 개더러워져도 액티비티 한개로 다할거임.. 귀찮고 이미 더러움...
            //원래 viewFlipper 도 비효율적이라 바꿔야지 했는데 이미 늦음
            //todo: viewflipper 갖다 버리고 fragment 사용하기
            offlineAdapter = new TitleAdapter(context);
            offlineAdapter.noResume();
            savedList.setLayoutManager(new LinearLayoutManager(this));
            savedList.setAdapter(offlineAdapter);
            offlineAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    Intent i = new Intent(context, EpisodeActivity.class);
                    i.putExtra("title",new Gson().toJson(offlineAdapter.getItem(position)));
                    i.putExtra("online", false);
                    startActivity(i);
                }

                @Override
                public void onLongClick(View view, int position) {
                    popup(view, position, offlineAdapter.getItem(position),3);
                }

                @Override
                public void onResumeClick(int position, int id) {
                    //not used
                }
            });
            getSavedTitles get = new getSavedTitles();
            get.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case 1:
                    //favorite result
                    Boolean favorite_after = data.getBooleanExtra("favorite",true);
                    if(!favorite_after) favoriteAdapter.notifyItemRemoved(selectedPosition);
                    break;
                case 2:
                    //recent result
                    recentAdapter.moveItemToTop(selectedPosition);
                    break;

            }
        }
    }

    private class searchManga extends AsyncTask<String,String,String>{
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected String doInBackground(String... params){
            search.fetch(p.getUrl());
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(searchAdapter.getItemCount()==0) {
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onLongClick(View view, int position) {
                        //none
                        popup(view, position, searchAdapter.getItem(position), 0);
                    }

                    @Override
                    public void onResumeClick(int position, int id) {
                        switch (p.getViewerType()){
                            case 0:
                            case 2:
                                viewer = new Intent(context, ViewerActivity.class);
                                break;
                            case 1:
                                viewer = new Intent(context, ViewerActivity2.class);
                                break;
                        }
                        viewer.putExtra("id",id);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = searchAdapter.getItem(position);
                        p.addRecent(selected);
                        System.out.println("onItemClick position: " + position);

                        Intent episodeView = new Intent(context, EpisodeActivity.class);
                        episodeView.putExtra("title", new Gson().toJson(selected));
                        startActivity(episodeView);
                    }
                });
            }else{
                searchAdapter.addData(search.getResult());
            }

            if(searchAdapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }

            swipe.setRefreshing(false);
        }
    }


    private class getSavedTitles extends AsyncTask<Void, Void, Integer>{
        List<Title> titles;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            offlineAdapter.addData(titles);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            titles = new ArrayList<>();
            homeDirStr = p.getHomeDir();
            List<Title> savedTitles = new ArrayList<>();
            File homeDir = new File(homeDirStr);
            if(homeDir.exists()){
                File[] files = homeDir.listFiles();
                for(File f:files){
                    if(f.isDirectory()){
                        File data = new File(f.getPath()+"/title.data");
                        if(data.exists()){
                            StringBuilder raw = new StringBuilder();
                            try {
                                BufferedReader br = new BufferedReader(new FileReader(data));
                                String line;
                                while ((line = br.readLine()) != null) {
                                    raw.append(line);
                                }
                                br.close();
                                JSONObject json = new JSONObject(raw.toString());
                                Title title = new Gson().fromJson(json.getJSONObject("title").toString(),new TypeToken<Title>(){}.getType());
                                if(title.getThumb().length()>0) title.setThumb(f.getAbsolutePath()+'/'+title.getThumb());
                                System.out.println("pppppppppppp "+title.getThumb());
                                titles.add(title);
                            }catch (Exception e){
                                e.printStackTrace();
                                titles.add(new Title(f.getName(),"","",new ArrayList<String>(),-1));
                            }
                        }else titles.add(new Title(f.getName(),"","",new ArrayList<String>(),-1));
                    }
                }
                //add titles to adapter
            }
            return null;
        }
    }


    public String httpsGet(String urlin){
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlin);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class updateCheck extends AsyncTask<Void, Integer, Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "업데이트 확인중..", Toast.LENGTH_SHORT).show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/version.json");
                JSONObject data = new JSONObject(rawdata);
                int lver = data.getInt("version");
                String link = data.getString("link");
                if(version<lver){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                    return 1;
                }
            }catch(Exception e){
                return -1;
            }return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    Toast.makeText(getApplicationContext(), "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Toast.makeText(getApplicationContext(), "최신버전 입니다.", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "새로운 버전을 찾았습니다. 다운로드 페이지로 이동합니다.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class noticeCheck extends AsyncTask<Void, Void, Integer> {
        String title, content, date;
        int nid = 0;
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/notice.json");
                JSONObject data = new JSONObject(rawdata);
                title = data.getString("title");
                System.out.println("pppppp"+title);
                content = data.getString("content");
                date = data.getString("date");
                nid = data.getInt("id");
            }catch(Exception e){
                e.printStackTrace();
            }
            return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(nid>0) showNotice(nid,title,content,date);
        }
    }

    void showNotice(int nid, String title, String content, String date){
        //공지 표시
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            JSONObject notices = new JSONObject(sharedPref.getString("notices", "{}"));
            try {
                if (notices.getJSONObject(nid + "").getString("content").length() > 0) {
                    //notice already exists
                }
            }catch (Exception e) {
                JSONObject notice = new JSONObject();
                notice.put("title", title).put("content", content).put("date", date);
                notices.put(nid + "", notice);
                sharedPref.edit().putString("notices", notices.toString()).commit();
                //show notice
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                };
                AlertDialog.Builder builder;
                if (dark) builder = new AlertDialog.Builder(this, R.style.darkDialog);
                else builder = new AlertDialog.Builder(this);
                builder.setTitle("공지")
                        .setMessage(title + "\n\n" + content)
                        .setPositiveButton("확인", dialogClickListener)
                        .show();
            }
        }catch (Exception e){e.printStackTrace();}
    }

    void checkNew(){
        //favorite adapter

    }

    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(MainActivity.this, view);
        //Inflating the Popup using xml file
        //todo: clean this part

        popup.getMenuInflater()
                .inflate(R.menu.title_options, popup.getMenu());
        switch(m){
            case 1:
                //최근
                popup.getMenu().findItem(R.id.del).setVisible(true);
            case 0:
                //검색
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 2:
                //좋아요
                break;
            case 3:
                //저장됨
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                popup.getMenu().findItem(R.id.remove).setVisible(true);
                break;
        }
        //좋아요 추가/제거 중 하나만 남김
        if(p.findFavorite(title)>-1) popup.getMenu().removeItem(R.id.favAdd);
        else popup.getMenu().removeItem(R.id.favDel);


        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.del:
                        //delete (only in recent)
                        recentAdapter.remove(position);
                        p.removeRecent(position);
                        recentAdapter.notifyItemRemoved(position);
                        break;
                    case R.id.favAdd:
                    case R.id.favDel:
                        //toggle favorite
                        p.toggleFavorite(title,0);
                        if(m==2){
                            favoriteAdapter.remove(position);
                            favoriteAdapter.notifyItemRemoved(position);
                        }
                        break;
                    case R.id.remove:
                        //저장된 만화에서 삭제
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        File folder = new File(homeDirStr+'/'+title.getName());
                                        deleteRecursive(folder);
                                        offlineAdapter.remove(position);
                                        offlineAdapter.notifyItemRemoved(position);
                                        Toast.makeText(context,"삭제가 완료되었습니다.",Toast.LENGTH_SHORT);
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                            }
                        };
                        AlertDialog.Builder builder;
                        if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                        else builder = new AlertDialog.Builder(context);
                        builder.setMessage("정말로 삭제 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                                .setNegativeButton("아니오", dialogClickListener).show();
                }
                return true;
            }
        });
        popup.show(); //showing popup menu
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

}
