package com.dm.assist.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dm.assist.common.AsyncHook;
import com.dm.assist.common.Observable;
import com.dm.assist.model.Campaign;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CloudDB {
    FirebaseDatabase database;
    FirebaseUser activeUser;

    public CloudDB()
    {
        this.database = FirebaseDatabase.getInstance();
        this.activeUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private DatabaseReference refCampaigns(){
        return this.database.getReference().child("campaign").child(activeUser.getUid());
    }

    private DatabaseReference refTokens(){
        return this.database.getReference().child("token").child(activeUser.getUid());
    }

    public void setCampaign(Campaign c) {
        refCampaigns().child(c.id).setValue(c);
    }

    public ValueEventListener watchAllCampaigns(Observable<List<Campaign>> campaignWatcher) {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Campaign> cs = new ArrayList<>();
                for (DataSnapshot ds: dataSnapshot.getChildren())
                    cs.add(ds.getValue(Campaign.class));
                campaignWatcher.onChange(cs);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        };
        refCampaigns().addValueEventListener(postListener);
        return postListener;
    }

    public ValueEventListener watchTokens(Observable<Long> tokenWatcher){
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tokenWatcher.onChange(snapshot.getValue(long.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        };
        refTokens().addValueEventListener(listener);
        return listener;
    }

    public void unwatchAllCampaigns(ValueEventListener listener)
    {
        refCampaigns().removeEventListener(listener);
    }

    public void unwatchTokens(ValueEventListener listener)
    {
        refTokens().removeEventListener(listener);
    }

    public void deleteCampaign(String campaignID) {
        refCampaigns().child(campaignID).removeValue();
    }
}
