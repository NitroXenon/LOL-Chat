package com.tesfayeabel.lolchat;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.theholywaffle.lolchatapi.ChatMode;
import com.github.theholywaffle.lolchatapi.LolStatus;
import com.github.theholywaffle.lolchatapi.wrapper.Friend;

import java.util.concurrent.TimeUnit;

/**
 * Created by Abel Tesfaye on 9/16/2014.
 */
public class StaticFriend implements Comparable<StaticFriend>, Parcelable {
    public static final Parcelable.Creator<StaticFriend> CREATOR
            = new Parcelable.Creator<StaticFriend>() {
        public StaticFriend createFromParcel(Parcel in) {
            return new StaticFriend(in);
        }

        public StaticFriend[] newArray(int size) {
            return new StaticFriend[size];
        }
    };
    private int profileIconId;
    private LolStatus.GameStatus gameStatus;
    private String statusMessage;
    private String skin;
    private long timeStamp;
    private ChatMode chatMode;
    private String name;
    private String userId;
    private boolean online;

    public StaticFriend(String name, String userId, ChatMode chatMode, LolStatus lolStatus, boolean online) {
        this.name = name;
        this.userId = userId;
        this.gameStatus = lolStatus.getGameStatus();
        if(gameStatus == null) {
            this.gameStatus = LolStatus.GameStatus.OUT_OF_GAME;
        }
        if(gameStatus == LolStatus.GameStatus.IN_GAME) {
            this.skin = lolStatus.getSkin();
            this.timeStamp = lolStatus.getTimestamp().getTime();
        }
        this.statusMessage = lolStatus.getStatusMessage();
        this.profileIconId = lolStatus.getProfileIconId();
        this.chatMode = chatMode;
        this.online = online;
    }

    public StaticFriend(Friend friend) {
        this(friend.getName(), friend.getUserId(), friend.getChatMode(), friend.getStatus(), friend.isOnline());
    }

    private StaticFriend(Parcel in) {
        profileIconId = in.readInt();
        gameStatus = LolStatus.GameStatus.values()[in.readInt()];
        statusMessage = in.readString();
        skin = in.readString();
        timeStamp = in.readLong();
        name = in.readString();
        userId = in.readString();
        chatMode = ChatMode.values()[in.readInt()];
        online = in.readInt() == 1;
    }

    public ChatMode getChatMode() {
        return chatMode;
    }

    public String getName() {
        return name;
    }

    public int getProfileIconId() {
        return profileIconId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isOnline() {
        return online;
    }

    public String getFullStatus() {
        StringBuilder fullStatus = new StringBuilder();
        fullStatus.append(statusMessage + "\n");
        fullStatus.append(LOLUtils.getStatus(gameStatus));
        if (gameStatus == LolStatus.GameStatus.IN_GAME) {
            fullStatus.append(" as " + skin);
            fullStatus.append(" for " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timeStamp) + " minutes");
        }
        return fullStatus.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StaticFriend))
            return false;
        StaticFriend other = (StaticFriend) object;
        return getUserId().equals(other.getUserId());
    }

    @Override
    public int compareTo(StaticFriend staticFriend) {
        if (isOnline() && getChatMode() != staticFriend.getChatMode()) {//if online and different chatmodes, compare chatmode, else compare name
            return getChatMode().compareTo(staticFriend.getChatMode());
        }
        return getName().toLowerCase().compareTo(staticFriend.getName().toLowerCase());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(profileIconId);
        out.writeInt(gameStatus.ordinal());
        out.writeString(statusMessage);
        out.writeString(skin);
        out.writeLong(timeStamp);
        out.writeString(name);
        out.writeString(userId);
        out.writeInt(chatMode.ordinal());
        out.writeInt(online ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }
}
