package pl.pwr.citrus.strack;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import pl.pwr.citrus.strack.db.DatabaseAdapter;

public class StoresActivity extends AppCompatActivity {

    private DatabaseAdapter mDbHelper;
    private MAdapter mMAdapter;
    private ListView mDslv;

    private long storeId;

    private CoordinatorLayout coordinatorLayout;

    CharSequence options[] = new CharSequence[] {"Edytuj", "Usun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stores);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coord);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(StoresActivity.this, StoreDetailsActivity.class);
                i.putExtra("INTENT", 0);
                startActivity(i);
            }
        });
    }

    private void displayItemList() {
        mDbHelper = DatabaseAdapter.getInstance(getBaseContext());
        mDbHelper.openConnection();
        // The desired columns to be bound
        String[] columns = new String[] { DatabaseAdapter.STORE_NAME,
                DatabaseAdapter.STORE_GROCERY,
                DatabaseAdapter.STORE_HOUSEHOLD,
                DatabaseAdapter.STORE_COSMETICS
        };

        // the XML defined views which the data will be bound to
        int[] ids = new int[] { R.id.item_name
                , R.id.textView1,
                R.id.textView2, R.id.textView3
        };

        // pull all items from database
        Cursor cursor = mDbHelper.getAllMealRecords();

        mMAdapter = new MAdapter(this, R.layout.item_store, null, columns, ids,
                0);

        mDslv = (ListView) findViewById(R.id.listView);

        mDslv.setAdapter(mMAdapter);
        mMAdapter.changeCursor(cursor);
        Log.i("LOL", cursor.getCount() + "");

        mDslv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                                    int position, long id) {
                // Get the cursor, positioned to the corresponding row in the
                // result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // Get the item name and details from this row in the database.
                int listId = cursor.getInt(cursor
                        .getColumnIndex("_id"));
                String name = cursor.getString(cursor
                        .getColumnIndex(DatabaseAdapter.STORE_NAME));
                Intent i = new Intent(StoresActivity.this, StoreDetailsActivity.class);
                i.putExtra("INTENT", listId);
                startActivity(i);

            }
        });

        mDslv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> listView, View view,
                                           int position, long id) {
                displayItemList();
                // Get the cursor, positioned to the corresponding row in the
                // result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                // Get the item name and details from this row in the database.
                storeId = cursor.getLong(cursor
                        .getColumnIndex("_id"));

                AlertDialog.Builder builder = new AlertDialog.Builder(StoresActivity.this);
                builder.setTitle("Wybierz");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:

                                Intent i = new Intent(StoresActivity.this, StoreDetailsActivity.class);
                                i.putExtra("INTENT", storeId);
                                startActivity(i);

                                break;
                            case 1:
                                mDbHelper
                                        .deleteStore(storeId);
                                displayItemList();
                                Snackbar.make(coordinatorLayout, "Sklep usunięty", Snackbar.LENGTH_LONG).
                                        setAction("Anuluj", snackbarUndoClickListener).show();
                                break;
                        }
                    }
                });
                builder.show();

//                showDialogView(listId, listName, true);
                return true;
            }
        });
    }

    View.OnClickListener snackbarUndoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mDbHelper.undeleteStore(storeId);
            displayItemList();
        }
    };

    private class MAdapter extends SimpleCursorAdapter{

        public MAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        displayItemList();
    }


}
