package com.example.user.mp3playertoyproject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DefaultActivity extends Fragment implements View.OnClickListener {

    static final String TAG = "MUSIC_PLAYER";


    private View view;
    private ImageView ivAlbum;
    private TextView tvClick, tvArtist, tvTitle, tvPlaytime, tvDuration;
    private SeekBar seekBar;
    private ImageButton ibPlay, ibPause, ibBefore, ibNext, ibLike, ibDontLike;

    private MediaPlayer mediaPlayer;
    private CustomDBHelper customDBHelper;

    private Context mContext;
    private Activity activity;
    private int position;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.frg_default, container, false);

        ivAlbum = view.findViewById(R.id.ivAlbum);
        tvClick = view.findViewById(R.id.tvClick);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvPlaytime = view.findViewById(R.id.tvPlaytime);
        tvDuration = view.findViewById(R.id.tvDuration);
        seekBar = view.findViewById(R.id.seekBar);
        ibPlay = view.findViewById(R.id.ibPlay);
        ibPause = view.findViewById(R.id.ibPause);
        ibBefore = view.findViewById(R.id.ibBefore);
        ibNext = view.findViewById(R.id.ibNext);
        ibLike = view.findViewById(R.id.ibLike);
        ibDontLike = view.findViewById(R.id.ibDontLike);
        customDBHelper = new CustomDBHelper(mContext);

        if (MainActivity.playingLiked)
            prepareForLastMusicPlayback(MainActivity.likeList, MainActivity.lastPosition);
        else
            prepareForLastMusicPlayback(MainActivity.musicDataList, MainActivity.lastPosition);

        //재생,정지 UI 세팅
        playAndPauseButtonSetting();

        //시크바 체인지리스너
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)  // 사용자가 시크바를 움직이면
                    mediaPlayer.seekTo(progress);   // 재생위치를 바꿔준다(움직인 곳에서의 음악재생)
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ibPlay.setOnClickListener(this);
        ibPause.setOnClickListener(this);
        ibBefore.setOnClickListener(this);
        ibNext.setOnClickListener(this);
        ibLike.setOnClickListener(this);
        ibDontLike.setOnClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*프래그먼트는 다른 액티비티에서 만들어지는 객체므로
        프레그먼트를 부르는 context를 onAttach함수에서 찾아서 필드에 저장 (하면 프래그먼트 안에서 context와 관련된 함수 사용 가능)*/
        mContext = context;
        if (context instanceof Activity)
            activity = (Activity) context;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibPlay:
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
                mediaPlayer.start();
                playAndPauseButtonSetting();
                seekBarThread();
                break;

            case R.id.ibPause:
                mediaPlayer.pause();
                playAndPauseButtonSetting();
                break;

            case R.id.ibBefore:
                mediaPlayer.stop();

                if (MainActivity.playingLiked) {
                    if (position - 1 < MainActivity.likeList.size() && position - 1 >= 0) {
                        position--;
                        MainActivity.increaseNumberOfClickOnDB(customDBHelper, MainActivity.likeList, position);
                        MainActivity.setMusicDataList(customDBHelper);
                        selectedMusicSetting(MainActivity.likeList, position);
                    } else {
                        Toast.makeText(mContext, "첫번째 곡 입니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (position - 1 < MainActivity.musicDataList.size() && position - 1 >= 0) {
                        position--;
                        MainActivity.increaseNumberOfClickOnDB(customDBHelper, MainActivity.musicDataList, position);
                        MainActivity.setMusicDataList(customDBHelper);
                        selectedMusicSetting(MainActivity.musicDataList, position);

                    } else {
                        Toast.makeText(mContext, "첫번째 곡 입니다", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.ibNext:
                mediaPlayer.stop();

                if (MainActivity.playingLiked) {
                    if (position + 1 < MainActivity.likeList.size()) {
                        position++;
                        MainActivity.increaseNumberOfClickOnDB(customDBHelper, MainActivity.likeList, position);
                        MainActivity.setMusicDataList(customDBHelper);
                        selectedMusicSetting(MainActivity.likeList, position);
                    } else {
                        Toast.makeText(mContext, "마지막 곡 입니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (position + 1 < MainActivity.musicDataList.size()) {
                        position++;
                        MainActivity.increaseNumberOfClickOnDB(customDBHelper, MainActivity.musicDataList, position);
                        MainActivity.setMusicDataList(customDBHelper);
                        selectedMusicSetting(MainActivity.musicDataList, position);

                    } else {
                        Toast.makeText(mContext, "마지막 곡 입니다", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.ibLike:
                updateLikedColumnOnDB(0);
                break;

            case R.id.ibDontLike:
                updateLikedColumnOnDB(1);
                break;
        }
    }

    private void updateLikedColumnOnDB(int state) {
        //db열기
        SQLiteDatabase sqLiteDatabase = customDBHelper.getWritableDatabase();
        //좋아요 칼럼 수정
        String query = null;
        if (MainActivity.playingLiked) {
            query = "UPDATE musicTBL SET liked = " + state + " WHERE id = '"
                    + MainActivity.likeList.get(position).getId() + "';";
        } else {
            query = "UPDATE musicTBL SET liked = " + state + " WHERE id = '"
                    + MainActivity.musicDataList.get(position).getId() + "';";
        }
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
        MainActivity.setLikeDataList(customDBHelper);

        if (state == 0) {
            ibLike.setVisibility(View.GONE);
            ibDontLike.setVisibility(View.VISIBLE);
        } else {
            ibLike.setVisibility(View.VISIBLE);
            ibDontLike.setVisibility(View.GONE);
        }
    }

    private void playAndPauseButtonSetting() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    ibPlay.setVisibility(View.GONE);
                    ibPause.setVisibility(View.VISIBLE);
                } else {
                    ibPlay.setVisibility(View.VISIBLE);
                    ibPause.setVisibility(View.GONE);
                }
            }
        });
    }

    private void prepareForLastMusicPlayback(ArrayList<MusicData> musicDataList, int position) {
        this.position = position;
        MainActivity.lastPosition = position;

        mediaPlayer = new MediaPlayer();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");

        Bitmap albumImg = MusicAdapter.getAlbumImg(mContext, Integer.parseInt(musicDataList.get(position).getAlbumArt()), 200);
        if (albumImg != null) {
            ivAlbum.setImageBitmap(albumImg);
        }
        tvArtist.setText(musicDataList.get(position).getArtist());
        tvTitle.setText(musicDataList.get(position).getTitle());
        tvDuration.setText(musicDataList.get(position).getDuration());
        tvClick.setText(String.valueOf(musicDataList.get(position).getClick()));
        tvDuration.setText(simpleDateFormat.format(Integer.parseInt(musicDataList.get(position).getDuration())));

        if(musicDataList.get(position).getLiked()==0){
            ibLike.setVisibility(View.GONE);
            ibDontLike.setVisibility(View.VISIBLE);
        } else {
            ibLike.setVisibility(View.VISIBLE);
            ibDontLike.setVisibility(View.GONE);
        }

        try {
            Uri musicURI = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicDataList.get(position).getId());
            mediaPlayer.setDataSource(mContext, musicURI);
            mediaPlayer.prepare();

            seekBar.setProgress(0);
            seekBar.setMax(Integer.parseInt(musicDataList.get(position).getDuration()));

            seekBarThread();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void selectedMusicSetting(ArrayList<MusicData> musicDataList, int position) {
        this.position = position;
        MainActivity.lastPosition = position;

        mediaPlayer = new MediaPlayer();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");

        Log.d(TAG, "DA_musicDataList.size() = "+musicDataList.size());

        Bitmap albumImg = MusicAdapter.getAlbumImg(mContext, Integer.parseInt(musicDataList.get(position).getAlbumArt()), 200);
        if (albumImg != null) {
            ivAlbum.setImageBitmap(albumImg);
        }
        tvArtist.setText(musicDataList.get(position).getArtist());
        tvTitle.setText(musicDataList.get(position).getTitle());
        tvDuration.setText(musicDataList.get(position).getDuration());
        tvClick.setText(String.valueOf(musicDataList.get(position).getClick()));
        tvDuration.setText(simpleDateFormat.format(Integer.parseInt(musicDataList.get(position).getDuration())));

        if(musicDataList.get(position).getLiked()==0){
            ibLike.setVisibility(View.GONE);
            ibDontLike.setVisibility(View.VISIBLE);
        } else {
            ibLike.setVisibility(View.VISIBLE);
            ibDontLike.setVisibility(View.GONE);
        }

        try {
            Uri musicURI = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicDataList.get(position).getId());
            mediaPlayer.setDataSource(mContext, musicURI);
            mediaPlayer.prepare();
            mediaPlayer.start();

            seekBar.setProgress(0);
            seekBar.setMax(Integer.parseInt(musicDataList.get(position).getDuration()));

            seekBarThread();
            //음악이 끝났을때 다음곡재생
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    ibNext.callOnClick();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void seekBarThread() {

        Thread thread = new Thread(new Runnable() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");

            @Override
            public void run() {
                while (mediaPlayer.isPlaying()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            tvPlaytime.setText(simpleDateFormat.format(mediaPlayer.getCurrentPosition()));
                        }
                    });
                    SystemClock.sleep(200);
                }//end of while

                return;
            }
        });

        thread.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

    }

}
