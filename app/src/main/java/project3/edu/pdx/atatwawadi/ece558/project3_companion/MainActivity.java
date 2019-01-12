package project3.edu.pdx.atatwawadi.ece558.project3_companion;
//Import all the required libraries
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**This is the main class for this app. It implements the UI and updation to the Firebase DB.
 */
public class MainActivity extends AppCompatActivity {
    //Define required private variables
    private TextView mTempView;
    private TextView mAdc3View;
    private TextView mAdc4View;
    private TextView mAdc5View;
    private TextView mDac1View;
    private TextView mProgresstxtView;

    private Button mPlusButton;
    private Button mMinusButton;
    private SeekBar mPwm4;
    private SeekBar mPwm5;
    private SeekBar mPwm6;
    private ProgressBar mProgressbar;
    private int pwm4progress = 0;
    private int pwm5progress = 0;
    private int pwm6progress = 0;
    private int dacVal = 0;

    private static final String TAG = "MainActivity";
    private static final String KEY_TEMP = "temp";
    private static final String KEY_ADC3 = "adc3";
    private static final String KEY_ADC4 = "adc4";
    private static final String KEY_ADC5 = "adc5";
    private static final String KEY_DAC = "dac";
    private static final String KEY_progress = "progressBar";
    private static final String KEY_pwm4 = "pwm4";
    private static final String KEY_pwm5 = "pwm5";
    private static final String KEY_pwm6 = "pwm6";

    //Instantiate and get the reference for each child of data in the Firebase DB
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    DatabaseReference PWM3 = myRef.child("PWM3");
    DatabaseReference PWM4 = myRef.child("PWM4");
    DatabaseReference PWM5 = myRef.child("PWM5");
    DatabaseReference PWM6 = myRef.child("PWM6");
    DatabaseReference ADA5 = myRef.child("ADA5IN");
    DatabaseReference ADC3 = myRef.child("ADC3IN");
    DatabaseReference ADC4 = myRef.child("ADC4IN");
    DatabaseReference ADC5 = myRef.child("ADC5IN");
    DatabaseReference DAC1OUT = myRef.child("DAC1OUT");
    DatabaseReference Timestamp = myRef.child("TIMESTAMP");

    /** This initializes all the text views and buttons and seek bars in the app.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(Bundle) called of MainActivity");
        setContentView(R.layout.activity_main);
        //Define the text views and the buttons,seekbars
        mTempView = (TextView)findViewById(R.id.tempview);
        mAdc3View= (TextView)findViewById(R.id.adc3view);
        mAdc4View = (TextView)findViewById(R.id.adc4view);
        mAdc5View = (TextView)findViewById(R.id.adc5view);
        mDac1View = (TextView)findViewById(R.id.dac1view);
        mProgresstxtView = (TextView)findViewById(R.id.progressBar_txt);
        mProgressbar = (ProgressBar) findViewById(R.id.progressBar);
        mPlusButton = (Button)findViewById(R.id.plus_button);
        mMinusButton = (Button)findViewById(R.id.minus_button);
        mPwm4 = (SeekBar)findViewById(R.id.pwm4seekBar);
        mPwm5 = (SeekBar)findViewById(R.id.pwm5seekBar);
        mPwm6 = (SeekBar)findViewById(R.id.pwm6seekBar);

        mDac1View.setText("0");
        //Load the saved instance
        if(savedInstanceState != null) {
            Log.d(TAG, "restoring previous state");
            mTempView.setText(String.valueOf(savedInstanceState.getDouble(KEY_TEMP, 0)));
            Log.d(TAG, "current temp restored");
            mAdc3View.setText(String.valueOf(savedInstanceState.getDouble(KEY_ADC3, 0)));
            Log.d(TAG, "current adc3 restored");
            mAdc4View.setText(String.valueOf(savedInstanceState.getDouble(KEY_ADC4, 0)));
            Log.d(TAG, "current adc4 restored");
            mAdc5View.setText(String.valueOf(savedInstanceState.getDouble(KEY_ADC5, 0)));
            Log.d(TAG, "current adc5 restored");
            mDac1View.setText(String.valueOf(savedInstanceState.getInt(KEY_DAC, 0)));
            Log.d(TAG, "current dac restored");
            mProgresstxtView.setText(String.valueOf(savedInstanceState.getInt(KEY_progress, 0)));
            mProgressbar.setProgress(savedInstanceState.getInt(KEY_progress, 0));
            Log.d(TAG, "current progress restored");
            mPwm4.setProgress(savedInstanceState.getInt(KEY_pwm4, 0));
            Log.d(TAG, "current pwm4 restored");
            mPwm5.setProgress(savedInstanceState.getInt(KEY_pwm5, 0));
            Log.d(TAG, "current pwm5 restored");
            mPwm6.setProgress(savedInstanceState.getInt(KEY_pwm6, 0));
            Log.d(TAG, "current pwm6 restored");
        }

        //Define onClick Listener for the Plus button. Set limits for Dac Value to 0-32
        mPlusButton.setOnClickListener(new View.OnClickListener(){
            /**This Listener reads the values from the DAC and increases it by 1.It updates the DAC value in the firebase
             * along with the timestamp.
             */
            public void onClick(View v){
                dacVal = Integer.valueOf(mDac1View.getText().toString());
                Date currentTime = Calendar.getInstance().getTime();
                if(dacVal>=0&&dacVal<32){
                    dacVal += 1;
                    DAC1OUT.setValue(dacVal); //Update the dac value in the Firebase DB
                    Timestamp.setValue(currentTime.toString()); //Update the Timestamp value in the Firebase DB
                }
            }
        });


        //Define onClick Listener for the Minus button. Set limits for Dac Value to 0-32.
        mMinusButton.setOnClickListener(new View.OnClickListener(){
            /**This Listener reads the values from the DAC and decreases it by 1.It updates the DAC value in the firebase
             * along with the timestamp.
             */
            public void onClick(View v){
                dacVal = Integer.valueOf(mDac1View.getText().toString());
                Date currentTime = Calendar.getInstance().getTime();
                if(dacVal>0&&dacVal<=32){
                    dacVal -= 1;
                    DAC1OUT.setValue(dacVal); //Update the dac value in the Firebase DB
                    Timestamp.setValue(currentTime.toString()); //Update the Timestamp value in the Firebase DB
                }
            }
        });

        //Set the Seekbar listener for Pwm 4
        mPwm4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // called when the user first touches the SeekBar
            }

            //When the user stops moving the seekbar it toasts the value of the seekbar
            /**This Listener reads the values from the PWM 4 seekbar and updates them in the firebase
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // called after the user finishes moving the SeekBar
                Date currentTime = Calendar.getInstance().getTime();
                pwm4progress = mPwm4.getProgress();
                PWM4.setValue(pwm4progress); //Update the PWM4 value in the Firebase DB
                Timestamp.setValue(currentTime.toString());
                Toast.makeText(MainActivity.this, "PWM 4 Seek bar progress is :" + pwm4progress,
                        Toast.LENGTH_SHORT).show();
            }
        });

        //Set the Seekbar listener for Pwm 4
        mPwm5.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // called when the user first touches the SeekBar
            }
            //When the user stops moving the seekbar it toasts the value of the seekbar
            /**This Listener reads the values from the PWM 5 seekbar and updates them in the firebase
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Date currentTime = Calendar.getInstance().getTime();
                pwm5progress = mPwm5.getProgress();
                PWM5.setValue(pwm5progress); //Update the PWM5 value in the Firebase DB
                Timestamp.setValue(currentTime.toString());
                Toast.makeText(MainActivity.this, "PWM 5 Seek bar progress is :" + pwm5progress,
                        Toast.LENGTH_SHORT).show();
            }
        });


        //Set the Seekbar listener for Pwm 4
        mPwm6.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //When the user stops moving the seekbar it toasts the value of the seekbar
            /**This Listener reads the values from the PWM 6 seekbar and updates them in the firebase
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Date currentTime = Calendar.getInstance().getTime();
                pwm6progress = mPwm6.getProgress();
                PWM6.setValue(pwm6progress);  //Update the PWM6 value in the Firebase DB
                Timestamp.setValue(currentTime.toString());
                Toast.makeText(MainActivity.this, "PWM 6 Seek bar progress is :" + pwm6progress,
                        Toast.LENGTH_SHORT).show();
            }
        });

        myRef.addValueEventListener(new ValueEventListener() {
            /**This function reads the values from the realtime database implemented in Firebase
             * If there is any change in any value of the database. It reflects them in the app.
             * @param dataSnapshot -  snapshot of the firebase DB
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated. It fetches different values from firebase and updates the text views.
                mTempView.setText(dataSnapshot.child("ADA5IN").getValue(Double.class).toString());
                mAdc3View.setText(dataSnapshot.child("ADC3IN").getValue(Double.class).toString());
                mAdc4View.setText(dataSnapshot.child("ADC4IN").getValue(Double.class).toString());
                mAdc5View.setText(dataSnapshot.child("ADC5IN").getValue(Double.class).toString());
                mDac1View.setText(dataSnapshot.child("DAC1OUT").getValue(Integer.class).toString());
                mProgresstxtView.setText(dataSnapshot.child("PWM3").getValue(Integer.class).toString());
                mProgressbar.setProgress(dataSnapshot.child("PWM3").getValue(Integer.class));
                mPwm4.setProgress(dataSnapshot.child("PWM4").getValue(Integer.class));
                mPwm5.setProgress(dataSnapshot.child("PWM5").getValue(Integer.class));
                mPwm6.setProgress(dataSnapshot.child("PWM6").getValue(Integer.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    //log callbacks
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    public void onPause(){
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    /** This function is called before destroying the activity to store the current state.
     * @param - savedInstanceState- The current instance.
     */
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        //savedInstanceState - variable of type bundle
        Log.i(TAG, "onSaveInstanceState called");
        //pairing the instance var with key and storing all the text view values.
        savedInstanceState.putDouble(KEY_TEMP,Double.valueOf(mTempView.getText().toString()));
        savedInstanceState.putDouble(KEY_ADC3,Double.valueOf(mAdc3View.getText().toString()));
        savedInstanceState.putDouble(KEY_ADC4,Double.valueOf(mAdc4View.getText().toString()));
        savedInstanceState.putDouble(KEY_ADC5,Double.valueOf(mAdc5View.getText().toString()));
        savedInstanceState.putInt(KEY_DAC, Integer.valueOf(mDac1View.getText().toString()));
        savedInstanceState.putInt(KEY_progress, Integer.valueOf(mProgresstxtView.getText().toString()));
        savedInstanceState.putInt(KEY_pwm4, Integer.valueOf(mPwm4.getProgress()));
        savedInstanceState.putInt(KEY_pwm5, Integer.valueOf(mPwm5.getProgress()));
        savedInstanceState.putInt(KEY_pwm6, Integer.valueOf(mPwm6.getProgress()));
    }

    public void onStop(){
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }

}