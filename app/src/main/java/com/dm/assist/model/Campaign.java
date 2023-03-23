package com.dm.assist.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.IgnoreExtraProperties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@IgnoreExtraProperties
public class Campaign implements Parcelable
{
    public String id;
    public String name;
    public String desc;
    public List<WorldCharacter> pcs;
    public List<WorldCharacter> npcs;
    public List<OneShot> oneShots;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public List<WorldCharacter> getPcs() {
        return pcs;
    }

    public List<WorldCharacter> getNpcs() {
        return npcs;
    }

    public List<OneShot> getOneShots() {
        return oneShots;
    }
    public Campaign(){}

    public Campaign(String name, String desc, List<WorldCharacter> pcs, List<WorldCharacter> npcs, List<OneShot> oneShots) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.desc = desc;
        this.pcs = new ArrayList<WorldCharacter>(pcs);
        this.npcs = new ArrayList<WorldCharacter>(npcs);
        this.oneShots = new ArrayList<OneShot>(oneShots);
    }

    protected Campaign(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.desc = in.readString();
        this.pcs = new ArrayList<WorldCharacter>();
        this.npcs = new ArrayList<WorldCharacter>();
        this.oneShots = new ArrayList<OneShot>();

        int numpcs = in.readInt();
        for (int i = 0; i < numpcs; i++)
            this.pcs.add(new WorldCharacter(in));

        int numnpcs = in.readInt();
        for (int i = 0; i < numnpcs; i++)
            this.npcs.add(new WorldCharacter(in));

        int numOneshots = in.readInt();
        for (int i = 0; i < numOneshots; i++)
            this.oneShots.add(new OneShot(in));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.desc);
        dest.writeInt(this.pcs.size());
        for (int i = 0; i < this.pcs.size(); i++)
            this.pcs.get(i).writeToParcel(dest, flags);
        dest.writeInt(this.npcs.size());
        for (int i = 0; i < this.npcs.size(); i++)
            this.npcs.get(i).writeToParcel(dest, flags);
        dest.writeInt(this.oneShots.size());
        for (int i = 0; i < this.oneShots.size(); i++)
            this.oneShots.get(i).writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Campaign> CREATOR = new Creator<Campaign>() {
        @Override
        public Campaign createFromParcel(Parcel in) {
            return new Campaign(in);
        }

        @Override
        public Campaign[] newArray(int size) {
            return new Campaign[size];
        }
    };

    public static Campaign fromJson(String id, JSONObject obj) throws JSONException {
        ArrayList<WorldCharacter> pcs = new ArrayList<WorldCharacter>();
        ArrayList<WorldCharacter> npcs = new ArrayList<WorldCharacter>();
        ArrayList<OneShot> oneShots = new ArrayList<OneShot>();

        JSONArray jpcs = obj.getJSONArray("pcs");
        JSONArray jnpcs = obj.getJSONArray("npcs");
        JSONArray jshots = obj.getJSONArray("oneShots");

        for (int i = 0; i < jpcs.length(); i++)
            pcs.add(WorldCharacter.fromJson(jpcs.getJSONObject(i)));

        for (int i = 0; i < jnpcs.length(); i++)
            npcs.add(WorldCharacter.fromJson(jnpcs.getJSONObject(i)));

        for (int i = 0; i < jshots.length(); i++)
            oneShots.add(OneShot.fromJson(jshots.getJSONObject(i)));

        Campaign c = new Campaign(
            obj.getString("name"),
            obj.getString("desc"),
            pcs,
            npcs,
            oneShots
        );
        c.id = id;
        return c;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("name", this.name);
        obj.put("desc", this.desc);

        JSONArray jpcs = new JSONArray();
        for (WorldCharacter pc: this.pcs)
            jpcs.put(pc.toJson());

        JSONArray jnpcs = new JSONArray();
        for (WorldCharacter npc: this.npcs)
            jnpcs.put(npc.toJson());

        JSONArray jshots = new JSONArray();
        for (OneShot os: this.oneShots)
            jshots.put(os.toJson());

        obj.put("pcs", jpcs);
        obj.put("npcs", jnpcs);
        obj.put("oneShots", jshots);

        return obj;
    }


}
