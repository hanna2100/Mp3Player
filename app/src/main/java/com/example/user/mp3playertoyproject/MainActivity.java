package com.example.user.mp3playertoyproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnListItemLongSelectedInterface
        , MusicAdapter.OnListItemSelectedInterface, LikeAdapter.OnListItemSelectedInterface, LikeAdapter.OnListItemLongSelectedInterface {

    static final String TAG = "MUSIC_PLAYER";
    final static String SHARED_MAIN = "mainActivity";

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private RecyclerView recyclerLike;

    public static MusicAdapter musicAdapter;
    public static LikeAdapter likeAdapter;

    public static int lastPosition;
    public static boolean loadMp3 = true;
    private boolean notFoundMp3 = false;
    public static boolean playingLiked = false;
    private long backBtnTime = 0l;

    public static ArrayList<MusicData> musicDataList;
    public static ArrayList<MusicData> likeList;

    private LinearLayoutManager linearLayoutManager;
    private LinearLayoutManager likeLayoutManager;
    private CustomDBHelper customDBHelper;

    private FrameLayout frameLayout;
    private Fragment frg;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("iriver");

        Log.d("TAG", "onCreate()");

        //sdcard 외부접근권한 설정
        ActivityCompat.requestPermissions(this
                , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE}
                , MODE_PRIVATE);

        drawerLayout = findViewById(R.id.drawerLayout);
        frameLayout = findViewById(R.id.frameLayout);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerLike = findViewById(R.id.recyclerLike);
        musicDataList = new ArrayList<>();
        likeList = new ArrayList<>();
        customDBHelper = new CustomDBHelper(this);
        linearLayoutManager = new LinearLayoutManager(this);
        likeLayoutManager = new LinearLayoutManager(this);
        musicAdapter = new MusicAdapter(this, this, this, R.layout.recycler_item, musicDataList);
        likeAdapter = new LikeAdapter(this, this, this, R.layout.recycler_item, likeList);


        //Shared Preferences 를 통해 앱 최초실행시 모든 mp3파일 재생리스트에 등록하기
        SharedPreferences sp = getSharedPreferences(SHARED_MAIN, 0);
        loadMp3 = sp.getBoolean("loadMp3", true);

        Log.d(TAG, "loadMp3 = " + loadMp3);
        if (loadMp3 == true) {//
            //음악파일 전부 가져와서 db에 정보 저장하기
            insertMusicDataToDatabase();
        }

        //db에 있는 음악파일을 리스트에 세팅하기
        setMusicDataList(customDBHelper);

        //리사이클러 뷰 설정
        recyclerView.setAdapter(musicAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerLike.setAdapter(likeAdapter);
        recyclerLike.setLayoutManager(likeLayoutManager);

        //프레임레이아웃에 나올 화면 세팅(음악파일을 못찾을경우 notFoundMp3Activity화면)
        frameLayoutViewSetting();

        //Shared Preferences 로부터 최근 재생한 곡 position 받아오기
        lastPosition = sp.getInt("lastPosition", 0);

        //Shared Preferences 로부터 최근 재생을 '좋아요목록'에서 했는지 확인
        playingLiked = sp.getBoolean( "isPlayingLiked", false);

        //프레임레이아웃 드래그 이벤트 (drawer 오픈이벤트)
        frameLayout.setOnTouchListener(new View.OnTouchListener() {
            float x1, x2, y1, y2, dx, dy;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        y1 = event.getY();
                        break;

                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        y2 = event.getY();

                        dx = x2 - x1;
                        dy = y2 - y1;

                        if (Math.abs(dx) > Math.abs(dy)) {
                            if (dx > 0)
                                drawerLayout.openDrawer(Gravity.LEFT, true);
                            else
                                drawerLayout.openDrawer(Gravity.RIGHT, true);

                        }
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {

        long current = System.currentTimeMillis();
        long getTime = current - backBtnTime;

        if (getTime >= 0 && getTime < 2000) {
            super.onBackPressed();
        } else {
            backBtnTime = current;
            Toast.makeText(this, "두번 누르면 종료됩니다", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onItemLongSelected(View v, final int position) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("리스트에서 제거합니다");
        dialog.setIcon(R.drawable.ic_remove);

        if (playingLiked)
            dialog.setMessage(likeList.get(position).getTitle());
        else
            dialog.setMessage(musicDataList.get(position).getTitle());

        dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteMusicOnDatabase(customDBHelper, position);
                setMusicDataList(customDBHelper);
            }
        });

        dialog.show();


    }

    @Override
    public void onItemSelected(View v, int position) {
        Log.d(TAG, "온아이템 셀렉티드 playingLiked = " + playingLiked);

        if (playingLiked == true) {
            increaseNumberOfClickOnDB(customDBHelper, likeList, position);
            setMusicDataList(customDBHelper);
            likeAdapter.notifyDataSetChanged();
            drawerLayout.closeDrawer(Gravity.RIGHT, true);

        } else {
            lastPosition = position;
            increaseNumberOfClickOnDB(customDBHelper, musicDataList, position);
            setMusicDataList(customDBHelper);
            musicAdapter.notifyDataSetChanged();
            drawerLayout.closeDrawer(Gravity.LEFT, true);

        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (frg != null)
            ft.remove(frg);

        frg = new PlayerActivity();
        ft.replace(R.id.frameLayout, frg);
        ft.commit();


    }

    private void frameLayoutViewSetting() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (notFoundMp3 == true)
            frg = new NotFoundMp3Activity();
         else
            frg = new DefaultActivity();

        ft.replace(R.id.frameLayout, frg);
        ft.commit();

    }

    public static void increaseNumberOfClickOnDB(CustomDBHelper customDBHelper, ArrayList<MusicData> list, int position) {
        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();
        //클릭 수 증가
        String query = "UPDATE musicTBL SET click = click+1 WHERE id = '"
                + list.get(position).getId() + "';";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();

    }

    private void deleteMusicOnDatabase(CustomDBHelper customDBHelper, int position) {
        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();
        //db에서 음악삭제
        String query = "DELETE FROM musicTBL WHERE id = '"
                + musicDataList.get(position).getId() + "';";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    private void insertMusicDataToDatabase() {

        String[] data = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DURATION};

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                data, null, null, MediaStore.Audio.Media.TITLE + " ASC");

        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                //음악데이터 가져오기
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String albumArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                String year = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                Log.d(TAG, "id = " + id);

                String query = "INSERT INTO musicTBL VALUES("
                        + "'" + id + "',"
                        + "'" + artist + "',"
                        + "'" + title + "',"
                        + "'" + albumArt + "',"
                        + "'" + year + "',"
                        + "'" + duration + "',"
                        + 0 + "," + 0 + ");";
                sqLiteDatabase.execSQL(query);

            }
            loadMp3 = false; //다음 시작시 mp3파일을 검색하지 않음
        } else {
            //cursor가 null일경우 (음악파일이 없을경우)
            loadMp3 = true; //다음 시작시 mp3파일 재검색
            notFoundMp3 = true;
        }
        sqLiteDatabase.close();
        cursor.close();

    }


    public static void setMusicDataList(CustomDBHelper customDBHelper) {

        musicDataList.clear();
        likeList.clear();

        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM musicTBL;", null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String artist = cursor.getString(1);
            String title = cursor.getString(2);
            String albumArt = cursor.getString(3);
            String year = cursor.getString(4);
            String duration = cursor.getString(5);
            int click = cursor.getInt(6);
            int liked = cursor.getInt(7);

            MusicData musicData = new MusicData(id, artist, title, albumArt, year, duration, click, liked);
            musicDataList.add(musicData);

            if (liked == 1) {
                likeList.add(musicData);
            }
        }

        sqLiteDatabase.close();
        cursor.close();

        musicAdapter.notifyDataSetChanged();
        likeAdapter.notifyDataSetChanged();

    }

    public static void setLikeDataList(CustomDBHelper customDBHelper) {

        likeList.clear();

        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM musicTBL WHERE liked = " + 1 + ";", null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String artist = cursor.getString(1);
            String title = cursor.getString(2);
            String albumArt = cursor.getString(3);
            String year = cursor.getString(4);
            String duration = cursor.getString(5);
            int click = cursor.getInt(6);
            int liked = cursor.getInt(7);

            MusicData musicData = new MusicData(id, artist, title, albumArt, year, duration, click, liked);
            likeList.add(musicData);

        }

        sqLiteDatabase.close();
        cursor.close();

        likeAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onDestroy() {

        SharedPreferences sp = getSharedPreferences(SHARED_MAIN, 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("loadMp3", loadMp3);
        editor.putInt("lastPosition", lastPosition);
        editor.putBoolean("isPlayingLiked", playingLiked);

        editor.commit();
        super.onDestroy();

    }

}
