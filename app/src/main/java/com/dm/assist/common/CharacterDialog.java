package com.dm.assist.common;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class CharacterDialog implements Parcelable {
    public String systemPrompt;
    public ArrayList<String> conversation;

    public CharacterDialog(String systemPrompt, String firstMessage)
    {
        this.systemPrompt = systemPrompt;
        this.conversation = new ArrayList<>();
        this.conversation.add(firstMessage);
    }

    protected CharacterDialog(Parcel in) {
        systemPrompt = in.readString();
        conversation = in.createStringArrayList();
    }

    public static final Creator<CharacterDialog> CREATOR = new Creator<CharacterDialog>() {
        @Override
        public CharacterDialog createFromParcel(Parcel in) {
            return new CharacterDialog(in);
        }

        @Override
        public CharacterDialog[] newArray(int size) {
            return new CharacterDialog[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(systemPrompt);
        parcel.writeStringList(conversation);
    }
}
