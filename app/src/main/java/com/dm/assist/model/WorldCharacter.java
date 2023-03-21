package com.dm.assist.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class WorldCharacter implements Parcelable {
    public String id;
    public String name;
    public String description;

    public WorldCharacter(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    protected WorldCharacter(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.description);
    }

    public static final Creator<WorldCharacter> CREATOR = new Creator<WorldCharacter>() {
        @Override
        public WorldCharacter createFromParcel(Parcel in) {
            return new WorldCharacter(in);
        }

        @Override
        public WorldCharacter[] newArray(int size) {
            return new WorldCharacter[size];
        }
    };

    public static WorldCharacter fromJson(JSONObject obj) throws JSONException {
        WorldCharacter wc = new WorldCharacter(
            obj.getString("name"),
            obj.getString("description")
        );
        wc.id = obj.getString("id");
        return wc;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("name", this.name);
        obj.put("description", this.description);
        return obj;
    }

}
