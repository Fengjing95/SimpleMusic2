package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class MusicAdapter extends BaseAdapter {

    private List<MusicDTO> mData;
    private LayoutInflater mInflater;
    private int mResource;
    private onMoreButtonListener monMoreButtonListener;

    public MusicAdapter(Context context, int resId, List<MusicDTO> data)
    {
        mData = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mData != null ? mData.get(position): null ;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        MusicDTO item = mData.get(position);
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.title = view.findViewById(R.id.music_title);
            holder.artist = view.findViewById(R.id.music_artist);
            holder.more = view.findViewById(R.id.more);
            view.setTag(holder);
        }
        else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        holder.title.setText(item.getTitle());
        holder.artist.setText(item.getArtist());
        holder.more.setOnClickListener(v -> monMoreButtonListener.onClick(position));
        return view;
    }

    class ViewHolder{
        TextView title;
        TextView artist;
        LinearLayout more;
    }

    public interface onMoreButtonListener {
        void onClick(int i);
    }

    public void setOnMoreButtonListener(onMoreButtonListener monMoreButtonListener) {
        this.monMoreButtonListener = monMoreButtonListener;
    }

}
