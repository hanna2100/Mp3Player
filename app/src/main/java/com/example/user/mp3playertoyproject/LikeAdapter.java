package com.example.user.mp3playertoyproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class LikeAdapter extends RecyclerView.Adapter<LikeAdapter.CustomViewHolder> {

    private OnListItemSelectedInterface mListener;
    private OnListItemLongSelectedInterface mLongListener;

    private Activity activity;
    private int layout;
    private ArrayList<MusicData> list;
    public static int position;
    private static final BitmapFactory.Options options = new BitmapFactory.Options();

    public LikeAdapter(Activity activity
            , OnListItemSelectedInterface listener
            , OnListItemLongSelectedInterface longListener
            , int layout, ArrayList<MusicData> list) {
        this.activity = activity;
        this.mListener = listener;
        this.mLongListener = longListener;
        this.layout = layout;
        this.list = list;
    }

    public interface OnListItemLongSelectedInterface {
        void onItemLongSelected(View v, int position);
    }

    public interface OnListItemSelectedInterface {
        void onItemSelected(View v, int position);
    }

    @NonNull
    @Override
    public LikeAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false);

        LikeAdapter.CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final LikeAdapter.CustomViewHolder customViewHolder, final int i) {


        //앨범이미지를 비트맵으로 만들기
        Bitmap albumImg = getAlbumImg(activity, Integer.parseInt(list.get(i).getAlbumArt()), 200);
        if (albumImg != null) {
            customViewHolder.d_ivAlbum.setImageBitmap(albumImg);
        }
        customViewHolder.d_tvTitle.setText(list.get(i).getTitle());
        customViewHolder.d_tvArtist.setText(list.get(i).getArtist());
        customViewHolder.d_tvClick.setText(String.valueOf(list.get(i).getClick()));

        customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.playingLiked=true;
                mListener.onItemSelected(v, i);
                position = i;

            }
        });
        customViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mLongListener.onItemLongSelected(v, i);
                position = i;

                return false;
            }
        });


    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }


    public static Bitmap getAlbumImg(Context context, int albumArt, int imgMaxSize) {

        /*컨텐트 프로바이더(Content Provider)는 앱 간의 데이터 공유를 위해 사용됨.
        특정 앱이 다른 앱의 데이터를 직접 접근해서 사용할 수 없기 때문에
        무조건 컨텐트 프로바이더를 통해 다른 앱의 데이터를 사용해야만 한다.
        다른 앱의 데이터를 사용하고자 하는 앱에서는 URI를 이용하여 컨텐트 리졸버(Content Resolver)를 통해 다른 앱의 컨텐트 프로바이더에게 데이터를 요청하게 되는데
        요청받은 컨텐트 프로바이더는 URI를 확인하고 내부에서 데이터를 꺼내어 컨텐트 리졸버에게 전달한다.
        */

        ContentResolver contentResolver = context.getContentResolver();

        //앨범아트는 uri를 제공하지 않으므로, 별도로 생성한다.
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumArt);
        if (uri != null) {
            ParcelFileDescriptor fd = null;
            try {
                fd = contentResolver.openFileDescriptor(uri, "r");
                options.inJustDecodeBounds = true;
                //true면 비트맵객체에 메모리를 할당하지 않아서 비트맵을 반환하지 않음.
                //다만 options fields는 값이 채워지기 때문에 Load 하려는 이미지의 크기를 포함한 정보들을 얻어올 수 있다.

                BitmapFactory.decodeFileDescriptor(
                        fd.getFileDescriptor(), null, options);

                int scale = 0;
                if (options.outHeight > imgMaxSize || options.outWidth > imgMaxSize) {
                    scale = (int) Math.pow(2, (int) Math.round(Math.log(imgMaxSize / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
                }
                options.inJustDecodeBounds = false; //true면 비트맵을 만들지 않고 해당이미지의 가로, 세로, Mime type등의 정보만 가져옴
                options.inSampleSize = scale; //이미지의 원본사이즈를 설정된 스케일정도로 줄임.


                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(
                        fd.getFileDescriptor(), null, options);

                if (bitmap != null) {
                    //정확하게 사이즈를 맞춤
                    if (options.outWidth != imgMaxSize || options.outHeight != imgMaxSize) {
                        Bitmap tmp = Bitmap.createScaledBitmap(bitmap, imgMaxSize, imgMaxSize, true);
                        bitmap.recycle();
                        bitmap = tmp;
                    }
                }

                return bitmap;

            } catch (FileNotFoundException e) {
            } finally {
                try {
                    if (fd != null)
                        fd.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        public ImageView d_ivAlbum;
        public TextView d_tvTitle;
        public TextView d_tvArtist;
        public TextView d_tvClick;

        public CustomViewHolder(@NonNull final View itemView) {
            super(itemView);
            d_ivAlbum = itemView.findViewById(R.id.d_ivAlbum);
            d_tvTitle = itemView.findViewById(R.id.d_tvTitle);
            d_tvArtist = itemView.findViewById(R.id.d_tvArtist);
            d_tvClick = itemView.findViewById(R.id.d_tvClick);


        }

    }

}
