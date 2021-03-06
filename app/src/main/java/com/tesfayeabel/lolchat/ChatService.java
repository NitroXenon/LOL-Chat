/*
 * LOL-Chat
 * Copyright (C) 2014  Abel Tesfaye
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tesfayeabel.lolchat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.theholywaffle.lolchatapi.ChatServer;
import com.github.theholywaffle.lolchatapi.FriendRequestPolicy;
import com.github.theholywaffle.lolchatapi.LolChat;
import com.github.theholywaffle.lolchatapi.LolStatus;
import com.github.theholywaffle.lolchatapi.listeners.ChatListener;
import com.github.theholywaffle.lolchatapi.wrapper.Friend;
import com.squareup.picasso.Picasso;
import com.tesfayeabel.lolchat.data.Message;
import com.tesfayeabel.lolchat.ui.ChatActivity;
import com.tesfayeabel.lolchat.ui.MainActivity;
import com.tesfayeabel.lolchat.ui.adapter.MessageAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChatService extends Service {
    public static final int foreground_ID = 69;
    public static final int notification_ID = 79;
    private final IBinder mBinder = new LocalBinder();
    private LolChat lolChat;
    private Handler handler = new Handler();
    private Toast toast;
    private NotificationManager notificationManager;
    private ArrayList<Message> missedMessages;

    /**
     * Saves a message to the messageHistory sharedPreferences file
     * If the message was sent by us, replace the sender with "me"
     *
     * @param message message to save
     */
    public static void saveMessage(Context context, Message message) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("messageHistory", Context.MODE_PRIVATE);
        String messageHistory = sharedPreferences.getString(message.getSender(), "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!messageHistory.equals("")) {
            messageHistory += "\n";
        }
        if (message.getDirection() == MessageAdapter.DIRECTION_INCOMING)
            messageHistory += message.toString();
        if (message.getDirection() == MessageAdapter.DIRECTION_OUTGOING)
            messageHistory += message.toString().replace(message.getSender(), "Me");
        editor.putString(message.getSender(), messageHistory);
        editor.apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        toast = new Toast(getApplicationContext());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        missedMessages = new ArrayList<Message>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String username = intent.getStringExtra("username");
        final String password = intent.getStringExtra("password");
        final String server = intent.getStringExtra("server");
        final boolean savePassword = intent.getBooleanExtra("savePassword", false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                lolChat = new LolChat(ChatServer.getChatServerByName(server), FriendRequestPolicy.REJECT_ALL, getString(R.string.api_riot));
                if (lolChat.login(username, password)) {
                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startForeground(foreground_ID, new Notification.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentText(getString(R.string.app_name) + " is running")
                            .setContentTitle(lolChat.getConnectedSummoner().getName())
                            .setTicker(getString(R.string.app_name) + " is now running")
                            .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setDefaults(Notification.DEFAULT_VIBRATE)
                            .build());
                    LolStatus lolStatus = LOLUtils.getStatus(lolChat.getConnectedSummoner(), lolChat.getRiotApi());
                    lolChat.setStatus(lolStatus
                            .setGameQueueType(LolStatus.Queue.NONE)
                            .setGameStatus(LolStatus.GameStatus.OUT_OF_GAME)
                            .setStatusMessage("USING BETA ABEL CHAT APP"));
                    lolChat.addChatListener(new ChatListener() {
                        @Override
                        public void onMessage(final Friend friend, final String message) {

                            missedMessages.add(new Message(friend.getName(), message, MessageAdapter.DIRECTION_INCOMING, -1));//only used for notification
                            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                            intent.putExtra("friend", friend.getName());
                            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                                    1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            Notification notification = new Notification.Builder(ChatService.this)
                                    .setContentTitle("New message")
                                    .setContentText("Message from " + friend.getName())
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setStyle(getStyle())
                                    .setContentIntent(contentIntent)
                                    .build();
                            notification.flags |= Notification.FLAG_AUTO_CANCEL;
                            notificationManager.notify(notification_ID, notification);

                            saveMessage(getApplicationContext(), new Message(friend.getName(), message.replace("\n", " "), MessageAdapter.DIRECTION_INCOMING, System.currentTimeMillis()));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showFriendToast(friend.getName(), message, friend.getStatus().getProfileIconId());
                                }
                            });
                        }
                    });
                    SharedPreferences.Editor editor = getSharedPreferences("loginData", Context.MODE_PRIVATE).edit();//TODO: ENCRYPTION
                    if (savePassword) {
                        editor.putString("username", username);
                        editor.putString("password", password);
                    } else {
                        editor.remove("username");//remove previously store auth info
                        editor.remove("password");
                    }
                    editor.putString("server", server);
                    editor.apply();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    stopSelf();
                }
                sendBroadcast(new Intent("com.tesfayeabel.lolchat.ui.LoginActivity.LOGIN"));
            }
        }).start();
        return START_NOT_STICKY;
    }

    /**
     * Gets the last 3 messages
     *
     * @return the style that is used for the new message notification
     */
    private Notification.InboxStyle getStyle() {
        Notification.InboxStyle style = new Notification.InboxStyle();
        List<Message> list = missedMessages.subList(Math.max(missedMessages.size() - 3, 0), missedMessages.size());//get list of last three messages
        for (Message m : list) {
            style.addLine(m.getSender() + ": " + m.getMessage());
        }
        style.setSummaryText(missedMessages.size() + " more");
        return style;
    }

    /**
     * Shows a custom toast with the friend name, message and iconId
     *
     * @param friend  name of the friend who sent the message
     * @param message the message that was sent
     * @param iconId  the friend's profileIconId
     */
    private void showFriendToast(String friend, String message, int iconId) {
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.toast_layout, null, false);
        toast.setView(view);
        TextView friendView = (TextView) view.findViewById(R.id.friend);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        friendView.setText(friend);
        messageView.setText(message);
        Picasso.with(getApplicationContext()).load(LOLChatApplication.getProfileIconURL(iconId)).into(imageView);
        toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lolChat != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lolChat.disconnect();
                }
            }).start();
        }
        notificationManager.cancel(notification_ID);
    }

    public LolChat getLolChat() {
        return lolChat;
    }

    public class LocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
        }
    }
}