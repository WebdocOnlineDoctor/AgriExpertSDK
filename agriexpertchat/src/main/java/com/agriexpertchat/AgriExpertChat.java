package com.agriexpertchat;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Waleed on 10/19/2019.
 */

public class AgriExpertChat {

    static Context ctx;

    public static String sendMessage(String appName, String msg, String sender, String receiver, String msgType) {
        String UsersChatKey = "";
        final String[] response = {""};
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference();
        String messageID = chatReference.push().getKey();
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("message", msg);
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("timestamp", ServerValue.TIMESTAMP);
        hashMap.put("messageID", messageID);
        hashMap.put("type", msgType);
        hashMap.put("MessageStatus", "sent");
        HashMap<String, Object> chat_hashMap = new HashMap<String, Object>();
        chat_hashMap.put("chat", "true");
        chatReference.child("Chat").child(receiver).child(appName).child(sender).updateChildren(chat_hashMap);
        if (appName.equalsIgnoreCase("AgriExpert")) {
            UsersChatKey = sender + "_" + receiver;
        } else {
            UsersChatKey = receiver + "_" + sender;
        }
        chatReference.child("Messages").child(appName).child(UsersChatKey).child(messageID).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    response[0] = "success";
                } else {
                    response[0] = task.getException().getMessage();
                }
            }
        });
        return response[0];
    }

    public static void getMessage(Context context, final String AppName, final String personalEmail, final String chatUserEmail)
    {
        ctx = context;
        final List<MessageDataModel> msgData = new ArrayList();
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference();
        final AgriExpertChatInterface listener = (AgriExpertChatInterface) ctx;
        String chatKey = "";
        if (AppName.equalsIgnoreCase("AgriExpert")) {
            chatKey = personalEmail + "_" + chatUserEmail;
        } else {
            chatKey = chatUserEmail + "_" + personalEmail;
        }
        final String finalChatKey = chatKey;
        chatReference.child("Messages").child(AppName).child(chatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                msgData.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MessageDataModel msg = new MessageDataModel();
                    msg.setSender(snapshot.child("sender").getValue().toString());
                    msg.setReceiver(snapshot.child("receiver").getValue().toString());
                    msg.setMessage(snapshot.child("message").getValue().toString());
                    msg.setTime(snapshot.child("timestamp").getValue().toString());
                    msg.setType(snapshot.child("type").getValue().toString());
                    msg.setMessageStatus(snapshot.child("MessageStatus").getValue().toString());
                    msgData.add(msg);
                    /*Log.e("MESSAGE---",msg.getMessage());*/
                }
                seenStatus(AppName, personalEmail, chatUserEmail, finalChatKey);
                listener.getMessagesResponse(msgData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public static String changeStatus(String AppName, String UserId, String status)
    {
        final AgriExpertChatInterface listener = (AgriExpertChatInterface) ctx;
        final String[] response = {""};
        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference().child("Users").child(AppName).child(UserId);
        HashMap<String, Object> param = new HashMap<>();
        param.put("status", status);
        param.put("timestamp", ServerValue.TIMESTAMP);
        dbReference.updateChildren(param).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    response[0] = "success";
                    listener.onUserStatusChangedResponse(response[0]);
                } else {
                    response[0] = task.getException().getMessage().toString();
                    listener.onUserStatusChangedResponse(response[0]);
                }
            }
        });

        return response[0];
    }

    public static void checkStatus(String AppName, String UserId)
    {
        final AgriExpertChatInterface listener = (AgriExpertChatInterface) ctx;
        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference();
        dbReference.child("Users").child(AppName).child(UserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = (String) dataSnapshot.child("status").getValue();
                long lastSeen = (long) dataSnapshot.child("timestamp").getValue();
                listener.onChangeUserStatusResponse(status, String.valueOf(lastSeen));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onChangeUserStatusResponse(databaseError.getMessage().toString(), "null");
            }
        });
    }

    private static void seenStatus(String AppName, final String personalEmail, final String chatUserEmail, String UsersChatKey)
    {
        DatabaseReference changeMsgSeenStatusReference = FirebaseDatabase.getInstance().getReference();
        changeMsgSeenStatusReference.child("Messages").child(AppName).child(UsersChatKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MessageDataModel messages = snapshot.getValue(MessageDataModel.class);
                    if (messages.getReceiver().equals(personalEmail) && messages.getSender().equals(chatUserEmail)) {
                        HashMap<String, Object> hashMap = new HashMap<String, Object>();
                        hashMap.put("MessageStatus", "seen");
                        snapshot.getRef().updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                }
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
