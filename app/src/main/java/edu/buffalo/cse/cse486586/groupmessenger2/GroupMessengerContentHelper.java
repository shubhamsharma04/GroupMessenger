package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by opensam on 3/2/17.
 */

public class GroupMessengerContentHelper extends Application{
    // Credit : How to get context in non-activity class in a good way http://stackoverflow.com/questions/22371124/getting-activity-context-into-a-non-activity-class-android
    private static final String TAG = OnPTestClickListener.class.getName();
    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public boolean writeContent(String key, String value) {
    boolean result = true;
// How to write a file in Android Credit : https://developer.android.com/guide/topics/data/data-storage.html#filesInternal
        String fileName = key;
        String content = value;
        Context context = GroupMessengerContentHelper.getContext();
        Log.i(TAG,context.toString());
        if(null!=key && !("").equals(key)){
            try {
                FileOutputStream fos = context.openFileOutput(key, Context.MODE_PRIVATE);
                fos.write(value.getBytes());
                fos.close();
            } catch (IOException e) {
                Log.e(TAG,"",e);
            }
        } else {
            result = false;
        }
        return result;
    }

    public String getContentForKey(String key) {
        StringBuilder str = new StringBuilder();
        Context context = GroupMessengerContentHelper.getContext();
        try {
            FileInputStream fileInputStream = context.openFileInput(key);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String input = "";
            while((input=bufferedReader.readLine())!=null){
                str.append(input);
            }
            fileInputStream.close();
            bufferedReader.close();
            Log.i(TAG,"Returning : "+str.toString());
        } catch (FileNotFoundException e) {
            Log.e(TAG,"",e);
        } catch (IOException e) {
            Log.e(TAG,"",e);
        }
        return str.toString();
    }
}
