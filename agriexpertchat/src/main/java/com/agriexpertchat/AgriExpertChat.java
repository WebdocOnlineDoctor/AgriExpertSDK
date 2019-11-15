package com.agriexpertchat;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.agriexpertchat.Models.ChatUserModel;
import com.agriexpertchat.NotificationManager.APIService;
import com.agriexpertchat.NotificationManager.Client;
import com.agriexpertchat.NotificationManager.Data;
import com.agriexpertchat.NotificationManager.MyResponse;
import com.agriexpertchat.NotificationManager.Sender;
import com.agriexpertchat.NotificationManager.Token;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Waleed on 10/19/2019.
 */

public class AgriExpertChat {

    public static void uploadFile(final String senderAppName, final String receiverAppName, Uri fileUri, final String sender, final String receiver, final String type) {

        final Uri[] downloadUri = new Uri[1];
        StorageReference filePath = null;
        StorageTask uploadTask;

        if (fileUri != null) {
            //progressDialog.show();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Messages");
            String messageID = ref.push().getKey();

            //String name = Environment.getExternalStorageDirectory().getAbsolutePath();

            switch (type) {
                case "image":
                    filePath = FirebaseStorage.getInstance().getReference().child("Images/").child(messageID + ".jpg");
                    break;

                case "pdf":
                    filePath = FirebaseStorage.getInstance().getReference().child("PDF Files/").child(messageID + "." + type);
                    break;

                case "docx":
                    filePath = FirebaseStorage.getInstance().getReference().child("Docx Files/").child(messageID + "." + type);
                    break;
            }
            uploadTask = filePath.putFile(fileUri);
            final StorageReference finalFilePath = filePath;
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return finalFilePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        downloadUri[0] = task.getResult();
                        messageSend(senderAppName, receiverAppName, downloadUri[0].toString(), sender, receiver, type);
                    } else {
                        // Handle failures
                        //Toast.makeText(MessagesActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
    }

    public static void sendMessage(final String senderAppName, final String receiverAppName, Uri fileUri, final String sender, final String receiver, String msgType) {
        uploadFile(senderAppName, receiverAppName, fileUri, sender, receiver, msgType);
    }

    public static void sendMessage(final String senderAppName, final String receiverAppName, final String msg, final String sender, final String receiver, String msgType) {
        messageSend(senderAppName, receiverAppName, msg, sender, receiver, msgType);
    }

    public static void getMessage(Context ctx, final String AppName, final String personalEmail, final String chatUserEmail) {
        final List<MessageDataModel> msgData = new ArrayList();
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference();
        final AgriExpertChatInterface listener = (AgriExpertChatInterface) ctx;
        String chatKey = "";
        ArrayList TwoChattingUsersID = new ArrayList<>();
        TwoChattingUsersID.add(personalEmail);
        TwoChattingUsersID.add(chatUserEmail);
        Collections.sort(TwoChattingUsersID);
        chatKey = TwoChattingUsersID.get(0) + "_" + TwoChattingUsersID.get(1);
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
                }
                seenStatus(AppName, personalEmail, chatUserEmail, finalChatKey);
                listener.getMessagesResponse(msgData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public static String changeStatus(Context ctx, String AppName, String UserId, String status) {
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

    public static void checkStatus(Context ctx, String AppName, String UserId) {
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

    private static void seenStatus(String AppName, final String personalEmail, final String chatUserEmail, String UsersChatKey) {
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

    private static void sendNotification(final String senderAppName, final String receiverAppName, final String sender, final String receiver, final String msg) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens").child(receiverAppName);
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                APIService apiService;
                apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(sender, sender + ": " + msg, senderAppName, "Sent");
                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200) {
                                        if (response.body().success != 1) {
                                            //Toast.makeText(MessagesActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable throwable) {
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void updateToken(String AppName, String Userid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(FirebaseInstanceId.getInstance().getToken());
        reference.child(AppName).child(Userid).setValue(token1);
    }

    private static String messageSend(final String senderAppName, final String receiverAppName, final String msg, final String sender, final String receiver, String msgType) {
        final boolean[] notify = {false};
        notify[0] = true;
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
        chatReference.child("Chat").child(receiver).child(senderAppName).child(sender).updateChildren(chat_hashMap);
        ArrayList TwoChattingUsersID = new ArrayList<>();
        TwoChattingUsersID.add(sender);
        TwoChattingUsersID.add(receiver);
        Collections.sort(TwoChattingUsersID);
        UsersChatKey = TwoChattingUsersID.get(0) + "_" + TwoChattingUsersID.get(1);
        chatReference.child("Messages").child(senderAppName).child(UsersChatKey).child(messageID).updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    response[0] = "success";
                    if (notify[0]) {
                        sendNotification(senderAppName, receiverAppName, sender, receiver, msg);
                    }
                    notify[0] = false;
                } else {
                    response[0] = task.getException().getMessage();
                }
            }
        });

        return response[0];
    }

    public static void registerUserForChat(Context context, final String appName, final String name, final String email, final String password) {
        final FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        final RegisterUserForChatInterface registerUserForChatInterface = (RegisterUserForChatInterface) context;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            DatabaseReference firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(appName).child(email.replace(".", ""));
                            HashMap<String, String> hashMap = new HashMap<String, String>();
                            hashMap.put("name", name);
                            hashMap.put("email", email);
                            hashMap.put("status", "online");

                            firebaseDatabaseReference.setValue(hashMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {

                                                registerUserForChatInterface.RegisterUserResponse("success");
                                            } else {
                                                registerUserForChatInterface.RegisterUserResponse(task.getException().toString());
                                            }
                                        }
                                    });
                        } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            mAuth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                registerUserForChatInterface.RegisterUserResponse("successfully login");
                                            } else {
                                                registerUserForChatInterface.RegisterUserResponse(task.getException().getMessage());
                                            }
                                        }
                                    });
                        } else {
                            registerUserForChatInterface.RegisterUserResponse(task.getException().getMessage());
                        }
                    }
                });
    }

    public static void getChatUsersList(Context context, final String email, String appName){
        final AgriExpertChatUsersInterface vetDocChatUsersInterface = (AgriExpertChatUsersInterface) context;
        if(!(appName.equalsIgnoreCase(String.valueOf(R.string.app_name))))
        {
            DatabaseReference chatUsersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(String.valueOf(R.string.app_name));
            chatUsersReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    Global.ChatUsersList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ChatUserModel user = new ChatUserModel();

                        user.setFirebaseEmail(snapshot.getKey());
                        // Toast.makeText(UserDashboardActivity.this, snapshot.getKey(), Toast.LENGTH_SHORT).show();
                        user.setName(snapshot.child("name").getValue().toString());
                        user.setEmail(snapshot.child("email").getValue().toString());
                        user.setStatus(snapshot.child("status").getValue().toString());
                        user.setAppName(String.valueOf(R.string.app_name));
                        Global.ChatUsersList.add(user);
                    }
                    //DoctorsListFrag.doctorsListAdapter.notifyDataSetChanged();
                    vetDocChatUsersInterface.ChatUsers(Global.ChatUsersList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else
        {
            final DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference();
            dbReference.child("Chat").child(email.replace(".", "")).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        dbReference.child("Chat").child(email.replace(".", "")).child(snapshot.getKey()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                for (final DataSnapshot snapshot1 : dataSnapshot1.getChildren()) {
                                    dbReference.child("Users").child(snapshot.getKey()).child(snapshot1.getKey()).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            ChatUserModel tempUser = new ChatUserModel();
                                            tempUser.setStatus(dataSnapshot.child("status").getValue().toString());
                                            tempUser.setName(dataSnapshot.child("username").getValue().toString());
                                            tempUser.setAppName(snapshot.getKey());
                                            tempUser.setFirebaseEmail(snapshot1.getKey());
                                            Global.ChatUsersList.add(tempUser);
                                       /*Toast.makeText(getActivity(), Global.chatUsersList.toString(), Toast.LENGTH_LONG).show();*/
                                           /* if (ChatUsersListFrag.adapter != null) {
                                                ChatUsersListFrag.adapter.notifyDataSetChanged();
                                            }*/
                                            vetDocChatUsersInterface.ChatUsers(Global.ChatUsersList);

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


    }
}
