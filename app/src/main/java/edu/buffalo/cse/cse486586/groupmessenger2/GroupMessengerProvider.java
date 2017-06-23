package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String sKey = "key";
    private static final String sValue = "value";
    private static final String [] columns = new String[] {sKey,sValue};
    private GroupMessengerContentHelper groupMessengerContentHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v("insert", values.toString());
        String key = values.getAsString(sKey);
        Log.v("insert","To be inserted key : "+key);
        String value = values.getAsString(sValue);
        Log.v("insert","To be inserted value : "+value);
        if(!groupMessengerContentHelper.writeContent(key,value)){
            uri = null;
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        groupMessengerContentHelper = new GroupMessengerContentHelper();
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.v("query", selection);
        MatrixCursor cursor = new MatrixCursor(columns);
        String key = selection;
        String value = groupMessengerContentHelper.getContentForKey(key);
        Object[] row = new Object[]{key,value};
        cursor.addRow(row);
        return cursor;
    }
}
