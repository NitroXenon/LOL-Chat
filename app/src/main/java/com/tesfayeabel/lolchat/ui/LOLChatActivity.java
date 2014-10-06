package com.tesfayeabel.lolchat.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.github.theholywaffle.lolchatapi.LolChat;
import com.tesfayeabel.lolchat.ChatService;

/**
 * Created by Abel Tesfaye on 10/5/2014.
 */
public abstract class LOLChatActivity extends Activity {

    private boolean serviceBound;
    private LolChat lolChat;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService chatService = ((ChatService.LocalBinder) service).getService();
            lolChat = chatService.getLolChat();
            serviceBound = true;
            onChatConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    public abstract void onChatConnected();

    @Override
    public void onStart() {
        super.onStart();
        if (!serviceBound) {
            Intent intent = new Intent(this, ChatService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(mConnection);
            serviceBound = false;
        }
    }

    public LolChat getLolChat() {
        return lolChat;
    }

    public boolean isServiceBound() {
        return serviceBound;
    }
}