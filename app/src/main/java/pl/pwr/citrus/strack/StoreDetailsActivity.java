package pl.pwr.citrus.strack;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import pl.pwr.citrus.strack.db.DatabaseAdapter;

public class StoreDetailsActivity extends AppCompatActivity {

    private Bundle mExtras;
    private int intentId;
    private DatabaseAdapter mDbHelper;
    private CoordinatorLayout coordinatorLayout;

    private TextView grocText;
    private TextView houseText;
    private TextView cosmText;
    private EditText et;
    private SeekBar seekGroc;
    private SeekBar seekHouse;
    private SeekBar seekCosm;

    private String STORE_NAME;
    private int STORE_GROCERY;
    private int STORE_HOUSEOLD;
    private int STORE_COSMETICS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mExtras = getIntent().getExtras();
        intentId = mExtras.getInt("INTENT");
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coordinatorLayout);

        mDbHelper = DatabaseAdapter.getInstance(getBaseContext());
        mDbHelper.openConnection();

        et = (EditText) findViewById(R.id.storeName);
        grocText = (TextView) findViewById(R.id.groceryText);
        houseText = (TextView) findViewById(R.id.houseText);
        cosmText = (TextView) findViewById(R.id.cosmText);

        seekGroc = (SeekBar) findViewById(R.id.seekBar);
        seekHouse = (SeekBar) findViewById(R.id.seekBar2);
        seekCosm = (SeekBar) findViewById(R.id.seekBar3);

        seekGroc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                grocText.setText(String.valueOf(progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        seekHouse.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                houseText.setText(String.valueOf(progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        seekCosm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                cosmText.setText(String.valueOf(progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

    if(intentId>0) {
            getSupportActionBar().setTitle("Edytuj sklep");
            Cursor cursor = mDbHelper.getStoreRecord(intentId);
            cursor.moveToFirst();
            STORE_NAME = cursor.getString(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_NAME));
            STORE_GROCERY = cursor.getInt(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_GROCERY));
            STORE_HOUSEOLD = cursor.getInt(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_HOUSEHOLD));
            STORE_COSMETICS = cursor.getInt(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_COSMETICS));

            et.setText(STORE_NAME);
            seekGroc.setProgress(STORE_GROCERY-1);
            seekHouse.setProgress(STORE_HOUSEOLD-1);
            seekCosm.setProgress(STORE_COSMETICS-1);
        }
        else {
            getSupportActionBar().setTitle("Nowy sklep");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.store_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveProduct();
            return true;
        }else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveProduct(){


            if(et.getText().toString()==null||et.getText().toString()==""){
                Snackbar.make(coordinatorLayout, "Nazwa sklepu jest pusta", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }else {
                if(intentId>0) {
                    mDbHelper.updateStore(intentId, et.getText().toString(), seekGroc.getProgress()+1, seekHouse.getProgress()+1, seekCosm.getProgress()+1);
                    onBackPressed();
                }else {
                    mDbHelper.insertStore(et.getText().toString(), seekGroc.getProgress()+1, seekHouse.getProgress()+1, seekCosm.getProgress()+1);
                    onBackPressed();
                }
            }

    }
}
