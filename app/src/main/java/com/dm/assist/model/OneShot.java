package com.dm.assist.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class OneShot implements Parcelable
{
    public String id;
    public String name;
    public String desc;

    public OneShot(String name, String desc)
    {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.desc = desc;
    }

    protected OneShot(Parcel in) {
        id = in.readString();
        name = in.readString();
        desc = in.readString();
    }

    public static final Creator<OneShot> CREATOR = new Creator<OneShot>() {
        @Override
        public OneShot createFromParcel(Parcel in) {
            return new OneShot(in);
        }

        @Override
        public OneShot[] newArray(int size) {
            return new OneShot[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(desc);
    }

    public static OneShot fromJson(JSONObject obj) throws JSONException {
        OneShot wc = new OneShot(
                obj.getString("name"),
                obj.getString("desc")
        );
        wc.id = obj.getString("id");
        return wc;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("name", this.name);
        obj.put("desc", this.desc);
        return obj;
    }
}
