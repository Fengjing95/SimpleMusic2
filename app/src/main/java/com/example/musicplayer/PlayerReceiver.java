//package com.example.musicplayer;
//
//import android.annotation.SuppressLint;
//import android.app.NotificationManager;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//import com.example.musicplayer.service.MusicService;
//
//public class PlayerReceiver extends BroadcastReceiver {
//
//    public static final String PLAY_PRE = "play_pre";
//    public static final String PLAY_NEXT = "play_next";
//    public static final String PLAY_PAUSE = "play_pause";
//    public static final String PLAY_PLAY = "play_play";
//    public static final String CLOSE = "close";
//
////    public MusicService.MusicServiceBinder serviceBinder;
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction().equals(PLAY_NEXT)){//PLAY_NEXT
//            Log.e("PlayerReceiver", "通知栏点击了下一首");
//            serviceBinder.playNext();
//        }
//        if (intent.getAction().equals(PLAY_PRE)) {
//            Log.e("PlayerReceiver", "通知栏点击了上一首");
//            serviceBinder.playPre();
//        }
//        if (intent.getAction().equals(PLAY_PAUSE)) {
//            Log.e("PlayerReceiver", "通知栏点击了暂停");
//        }
//        if (intent.getAction().equals(PLAY_PLAY)) {
//            Log.e("PlayerReceiver", "通知栏点击了开始");
//            serviceBinder.playOrPause();
//        }
//        if (intent.getAction().equals(CLOSE)) {
//            Log.e("PlayerReceiver", "通知栏点击了close");
//            @SuppressLint("WrongConstant") NotificationManager manager =
//                    (NotificationManager)context.getSystemService("notification");
//            manager.cancelAll();
//            ActivityController.clearAll();
//        }
//    }
//}
