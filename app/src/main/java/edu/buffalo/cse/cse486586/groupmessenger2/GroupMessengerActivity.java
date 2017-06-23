package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static Integer count = -1;
    private Uri mUri;
    private static int maxIdSoFar = 0;
    private PriorityQueue<MsgObject> backQ = null;
    private static int emulatorId = 0;
    private static Integer semphoreLockId = 0;
    private static Integer proposeSemaphoreID = 0;
    private static int numOfAvds = 5;
    private boolean isAvdAlive [] = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backQ = new PriorityQueue<MsgObject>();
        setContentView(R.layout.activity_group_messenger);
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        isAvdAlive = new boolean[numOfAvds];
        for(int i=0;i<numOfAvds;i++){
            isAvdAlive[i] = true;
        }
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

       // final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        // Hack to get Emulator number. Credit : Steve Ko
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        emulatorId = Integer.parseInt(portStr);
        Log.i(TAG,"Emulator id : "+emulatorId);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(GeneralConstants.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }



        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                //TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                //remoteTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     * Credit : Steve Ko
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author and Credit stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.i(TAG, "Inside receive");
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(GeneralConstants.SOCKET_TIMEOUT);
                    // Credit : Socket programming Based on the input from TA Sharath during recitatio
                    DataInputStream inputStream = new DataInputStream(client.getInputStream());
                    StringBuilder str = new StringBuilder(inputStream.readUTF());
                    Log.i(TAG,"Msg Received : "+str.toString());
                    JSONObject jsonObject = new JSONObject(str.toString());
                    int action = jsonObject.getInt(GeneralConstants.KEY_ACTION);
                    int emulatorId = jsonObject.getInt(GeneralConstants.KEY_PID);
                    String messageToSend = GeneralConstants.ACK_MSG;
                    MsgObject msgObject = new MsgObject();
                    msgObject.setId(jsonObject.getInt(GeneralConstants.KEY_ID));
                    msgObject.setMsg(jsonObject.getString(GeneralConstants.KEY_MSG));
                    msgObject.setpId(jsonObject.getInt(GeneralConstants.KEY_PID));
                    msgObject.setDeliverable(false);
                    msgObject.setAction(action);

                    switch (action){
                        case GeneralConstants.ACTION_SEND_SEQ:
                            int proposedSeq = getProposedSeq();
                            msgObject.setAgreedId(proposedSeq);
                            Log.i(TAG,"Proposing seq num : "+proposedSeq+" for msg : "+msgObject.getMsg());
                            Log.i(TAG,"Putting in PQ : "+msgObject.toString());
                            messageToSend = messageToSend + ","+proposedSeq;
                            putInPriorityQ(msgObject);
                            Log.i(TAG,"Size of PQ : "+backQ.size());
                            break;
                        case GeneralConstants.ACTION_ACCEPT_SEQ:
                            msgObject.setAgreedId(jsonObject.getInt(GeneralConstants.KEY_AGREEDID));
                            Log.i(TAG,"Updating in PQ : "+msgObject.toString());
                            boolean noTerribleOcc = updatePQ(msgObject);
                            if(!noTerribleOcc){
                                Log.i(TAG,"Something terrible has happened. Flee Flee for your lives. JK :)");
                            } /*else {
                                noTerribleOcc =  writeMsgs();
                            }*/
                            break;
                        case GeneralConstants.ACTION_DELETE_MSG:
                             Log.i(TAG,"About to delete msg : "+msgObject.toString());
                             boolean isSuccessFul = deleteMsgFromPQ(msgObject);
                             if(!isSuccessFul){
                                 Log.e(TAG,"Somehow failed to remove object : "+msgObject.toString()+" .This will most likely cause problems");
                             }
                            break;
                        case GeneralConstants.ACTION_WRITE_MSG:
                            msgObject.setAgreedId(jsonObject.getInt(GeneralConstants.KEY_AGREEDID));
                            msgObject.setDeliverable(true);
                            boolean noTerribleOccFin = updatePQ(msgObject);
                            if(noTerribleOccFin) {
                                Log.i(TAG, "About to write msg : " + msgObject.toString()+" Size of PQ : "+backQ.size());
                            } else {
                                Log.e(TAG,"Final step glitch for msg : "+msgObject.toString()+" Size of PQ : "+backQ.size());
                            }
                            writeMsgs();
                            break;


                    }

                    DataOutputStream dataOutputStream = new DataOutputStream(client.getOutputStream());
                    dataOutputStream.writeUTF(messageToSend);
                    dataOutputStream.close();
                    inputStream.close();
                    client.close();
                } catch (IOException e) {
                    Log.e(TAG, "Can't connect4 ",e);
                    Log.e(TAG,"Emulator index4 : "+emulatorId+" has failed");
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException Parse Exception",e);
                }
            }
        }

        private boolean deleteMsgFromPQ(MsgObject msgObject) {
            boolean isSuccessFul = false;
            synchronized (proposeSemaphoreID) {
                isSuccessFul=  backQ.remove(msgObject);
            }
            return isSuccessFul;
        }

        private boolean writeMsgs() {
            boolean result = true;
            synchronized (proposeSemaphoreID) {
                MsgObject msg = backQ.peek();
                if(msg!=null){
                    int emulatorId = msg.getpId();
                    Log.i(TAG,"Head is from : "+emulatorId);
                    int index = (emulatorId-GeneralConstants.BASE_EMULATOR_ID)/2;
                    if(!isAvdAlive[index]){
                        Log.i(TAG,"Since msg is from : "+emulatorId+" we'll remove it");
                        while(msg!=null && msg.getpId()==emulatorId){
                            backQ.remove();
                            msg = backQ.peek();
                        }
                    }

                }
                while (msg!=null && msg.isDeliverable()) {
                    count++;
                    ContentValues keyValueToInsert = new ContentValues();
                    keyValueToInsert.put("key", String.valueOf(count));
                    keyValueToInsert.put("value", msg.getMsg());
                    Log.i("Inserting Inserting", msg.getMsg() + "    :   " + count);
                    Uri newUri = getContentResolver().insert(
                            mUri,
                            keyValueToInsert
                    );
                    backQ.remove();
                    msg = backQ.peek();
                }
            }
            return result;
        }

        private boolean updatePQ(MsgObject msgObject) {
            boolean result = true;
            synchronized (proposeSemaphoreID) {
                 while(backQ.remove(msgObject)){
                     Log.i(TAG,"Deleting object : "+msgObject.toString());
                 }
                backQ.add(msgObject);
                if(msgObject.getAgreedId()>proposeSemaphoreID){
                    proposeSemaphoreID = msgObject.getAgreedId();
                }
            }
            return result;
        }

        private boolean putInPriorityQ(MsgObject msgObject) {
            synchronized (proposeSemaphoreID) {
                backQ.add(msgObject);
            }
            return true;
        }

        private int getProposedSeq() {
                synchronized (proposeSemaphoreID){
                    ++proposeSemaphoreID;
                }
            return proposeSemaphoreID;
        }

        // Credit Steve Ko
        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            Log.i(TAG, "Inside post : "+strReceived);
            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived;
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author and Credit stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            int agreedId = -1;
            int msgUid = 0;
            synchronized (semphoreLockId) {
                ++semphoreLockId;
                Log.i(TAG, " Unique id : " + semphoreLockId);
                msgUid = semphoreLockId;
            }

            MsgObject msg = new MsgObject();
            //msg.setAction(GeneralConstants.ACTION_SEND_SEQ);
            String msgToSend = msgs[0];
            msg.setpId(emulatorId);
            msg.setMsg(msgToSend);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(GeneralConstants.KEY_MSG, msgToSend);
                jsonObject.put(GeneralConstants.KEY_ID, semphoreLockId);
                jsonObject.put(GeneralConstants.KEY_PID, emulatorId);
                while (true) {
                    List<Integer> allPropsedIds = new ArrayList<Integer>();
                    jsonObject.put(GeneralConstants.KEY_ACTION, GeneralConstants.ACTION_SEND_SEQ);
                    boolean canDeliver = true;
                    for (int i = 0; i < numOfAvds; i++) {
                        if (isAvdAlive[i]) {
                            try {
                                String remotePort = GeneralConstants.ALL_PORTS[i];
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));
// Credit : Socket programming Based on the input from TA Sharath during recitation
                                socket.setSoTimeout(GeneralConstants.SOCKET_TIMEOUT);
                                Log.i(TAG, "Inside Send1 : " + jsonObject.toString());
                                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                                outputStream.writeUTF(jsonObject.toString());
                                StringBuilder str = null;
                                InputStream inputStream = socket.getInputStream();
                                DataInputStream dataInputStream = new DataInputStream(inputStream);
                                do {
                                    str = new StringBuilder(dataInputStream.readUTF());
                                } while (!str.toString().startsWith(GeneralConstants.ACK_MSG));
                                Log.i(TAG,"Received msg : "+str.toString());
                                int proposedId = Integer.parseInt(str.toString().split(",")[1]);
                                allPropsedIds.add(proposedId);
                                outputStream.close();
                                inputStream.close();
                                socket.close();
                            } catch (UnknownHostException e) {
                                Log.e(TAG, "ClientTask UnknownHostException1 : ", e);
                                Log.e(TAG, "Emulator index1 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                //canDeliver = false;
                            } catch (IOException e) {
                                Log.e(TAG, "ClientTask socket IOException1 : ", e);
                                Log.e(TAG, "Emulator index1 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                //canDeliver = false;
                            }
                        }
                    }
                    if(!canDeliver){
                        continue;
                    }
                    agreedId = getMaxId(allPropsedIds);
                    Log.i(TAG, "Number of proposals got : " + allPropsedIds.size());
                    Log.i(TAG, "for message : " + msgToSend + " Agreed id : " + agreedId);
                    // TODO : Change this
                    jsonObject.put(GeneralConstants.KEY_ACTION, GeneralConstants.ACTION_ACCEPT_SEQ);
                    Log.i(TAG, "for message : " + msgToSend + " Agreed id : " + agreedId+" action : "+jsonObject.getInt(GeneralConstants.KEY_ACTION));
                    jsonObject.put(GeneralConstants.KEY_AGREEDID, agreedId);

                    for (int i = 0; i < numOfAvds; i++) {
                        if (isAvdAlive[i]) {
                            try {
                                String remotePort = GeneralConstants.ALL_PORTS[i];
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));
                                socket.setSoTimeout(GeneralConstants.SOCKET_TIMEOUT);
// Credit : Socket programming Based on the input from TA Sharath during recitation

                                Log.i(TAG, "Inside Send2 : " + jsonObject.toString());
                                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                                outputStream.writeUTF(jsonObject.toString());
                                StringBuilder str = null;
                                InputStream inputStream = socket.getInputStream();
                                DataInputStream dataInputStream = new DataInputStream(inputStream);
                                do {
                                    str = new StringBuilder(dataInputStream.readUTF());
                                } while (!str.toString().startsWith(GeneralConstants.ACK_MSG));
                                outputStream.close();
                                inputStream.close();
                                socket.close();
                            } catch (UnknownHostException e) {
                                Log.e(TAG, "ClientTask UnknownHostException2 : ", e);
                                Log.e(TAG, "Emulator index2 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                canDeliver = false;
                            } catch (IOException e) {
                                Log.e(TAG, "ClientTask socket IOException2 : ", e);
                                Log.e(TAG, "Emulator index2 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                canDeliver = false;
                            }
                        }
                    }
                    if(canDeliver){
                        Log.i(TAG,"Seems like nothing bad happened for msg : "+msgToSend+" for agreed id : "+agreedId);
                        jsonObject.put(GeneralConstants.KEY_ACTION, GeneralConstants.ACTION_WRITE_MSG);
                    } else {
                        Log.i(TAG,"Seems like something bad happened for msg : "+msgToSend+" for agreed id : "+agreedId);
                        continue;
                       // jsonObject.put(GeneralConstants.KEY_ACTION, GeneralConstants.ACTION_DELETE_MSG);
                    }

                    for (int i = 0; i < numOfAvds; i++) {
                        if (isAvdAlive[i]) {
                            try {
                                String remotePort = GeneralConstants.ALL_PORTS[i];
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(remotePort));
                                socket.setSoTimeout(GeneralConstants.SOCKET_TIMEOUT);
// Credit : Socket programming Based on the input from TA Sharath during recitation

                                Log.i(TAG, "Inside Send3 : " + jsonObject.toString());
                                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                                outputStream.writeUTF(jsonObject.toString());
                                StringBuilder str = null;
                                InputStream inputStream = socket.getInputStream();
                                DataInputStream dataInputStream = new DataInputStream(inputStream);
                                do {
                                    str = new StringBuilder(dataInputStream.readUTF());
                                } while (!str.toString().startsWith(GeneralConstants.ACK_MSG));
                                outputStream.close();
                                inputStream.close();
                                socket.close();
                            } catch (UnknownHostException e) {
                                Log.e(TAG, "ClientTask UnknownHostException3 : ", e);
                                Log.e(TAG, "Emulator index3 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                canDeliver = false;
                            } catch (IOException e) {
                                Log.e(TAG, "ClientTask socket IOException3 : ", e);
                                Log.e(TAG, "Emulator index3 : " + i + " has failed");
                                isAvdAlive[i] = false;
                                canDeliver = false;
                            }
                        }
                    }
                    if(canDeliver){
                        break;
                    }

                }

                }catch(JSONException e){
                    Log.e(TAG, "JSONException : ", e);
                }

            return null;
        }

        private int getMaxId(List<Integer> allPropsedIds) {
            int result = 0;
            if(allPropsedIds!=null){
                int size = allPropsedIds.size();
                for(int i=0;i<size;i++){
                    if(allPropsedIds.get(i)>result){
                        result = allPropsedIds.get(i);
                    }
                }
            }
            return result;
        }
    }
}
