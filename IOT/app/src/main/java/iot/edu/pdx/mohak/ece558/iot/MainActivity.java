/**
 * @Author:
 * Mohak Patel
 * Archit Tatwawadi
 *
 * Description:
 *The project is to control an RGB LED and monitor a temperature sensor remotely using Google’s
 * Firebase realtime database as the communication bridge between a user application running on a
 * mobile Android device and the Raspberry Pi 3 (RPI3) running a control app. The apps will also
 * control the speed of a low cost DC motor based on the reading of the temperature sensor. The
 * RPI3 (master) communicates with a PIC16F (slave) series microcontroller via I2C. The PIC
 * contains the PWM channels (Pulse width modulation generator), DACs (digital to analog converter),
 * and ADC’s(analog to digital converter) actually connected to the devices.
 */


//unique package name
package iot.edu.pdx.mohak.ece558.iot;

//built in library
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

//library to handle log events
import android.util.Log;

//library for android thing
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

//library to handle firebase and databse
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//library for exceptions
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static android.content.ContentValues.TAG;

/**
 * Main activity for the android thing
 */
public class MainActivity extends Activity {
    // I2C Device Name
    private static final String I2C_DEVICE_NAME = "I2C1";
    //Gpio on raspberry pi for led
    private static final String LED = "BCM4";
    private Gpio mLedGpio;
    // I2C Slave Address
    private static final int I2C_ADDRESS = 0x08;
    //PWM Address
    private static final int PWM3_ADDRESS = 0x00;
    private static final int PWM4_ADDRESS = 0x01;
    private static final int PWM5_ADDRESS= 0x02;
    private static final int PWM6_ADDRESS = 0x03;
    //DAC Address
    private static final int DAC1_ADDRESS = 0x04;
    //ADC Address
    private static final int ADA5_ADDRESS_LSB =0x05 ;
    private static final int ADC4_ADDRESS_LSB =0x07 ;
    private static final int ADC3_ADDRESS_LSB =0x09 ;
    private static final int ADC5_ADDRESS_LSB =0x0b ;

    // 1.98 volt for reference volatage in mili volt
    private static final int Ref_volatage = 1980;

    //Duty cycle
    private static final byte DUTYCYCLE_30 =0x1e ;
    private static final byte DUTYCYCLE_40 =0x28 ;
    private static final byte DUTYCYCLE_50 =0x32 ;
    private static final byte DUTYCYCLE_70 =0x46 ;
    private static final byte DUTYCYCLE_80 =0x50 ;

    private float Temparature;
    private float lasttemp;

    //I2c object and handler
    private I2cDevice mDevice;
    private Handler mHandler = new Handler();

    //Firebase obeject and database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    DatabaseReference PWM3 = myRef.child("PWM3");
    DatabaseReference ADA5 = myRef.child("ADA5IN");
    DatabaseReference ADC3 = myRef.child("ADC3IN");
    DatabaseReference ADC4 = myRef.child("ADC4IN");
    DatabaseReference ADC5 = myRef.child("ADC5IN");
    DatabaseReference Timestamp = myRef.child("Timestamp");

    //Local variable
    public int pwm3_val;
    public int pwm4_val;
    public int pwm5_val;
    public int DAC_val;



    /**
     *nCreate is called when the quiz_activity
     * is started.
     *  super.xxx method whenever we override a method
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // calls the super.onCreate() method and inflates
        super.onCreate(savedInstanceState);
        /**
         * create the object for peripherals
         *  and get all inctances
         */
        PeripheralManager manager = PeripheralManager.getInstance();

        /**
         * addvalueEventListner method to read the updated values from firebase
         */
        myRef.addValueEventListener(new ValueEventListener() {
            /**
             * OnDataChange method to update the activity based on the updated values from firebase
             *  This method is called once with the initial value and again
             *  whenever data at this location is updated.
             * @param dataSnapshot
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //read the updated values from firebase
                DAC_val=dataSnapshot.child("DAC1OUT").getValue(Integer.class);
                pwm3_val= dataSnapshot.child("PWM6").getValue(Integer.class);
                pwm4_val=dataSnapshot.child("PWM4").getValue(Integer.class);
                pwm5_val=dataSnapshot.child("PWM5").getValue(Integer.class);

                try {
                    //Set the leds based on the PWM values
                    //Set the ADC valuses based on the DAC
                    set_LEDS();
                    set_ADC_DAC();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Oncancelled method to give a error when it is unable to read the data from firebase
             * @param error
             */
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        // Attempt to access the  GPIO port
        try {
            mLedGpio = manager.openGpio(LED);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Attempt to access the I2C device
        // and set the direction and intial value of the GPIO
        try {
            mDevice = manager.openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            Log.i(TAG, "Start blinking LED GPIO pin");
        } catch (IOException e) {
            Log.w(TAG, "Unable to access I2C device", e);
        }
        /**
         * Start the handler
         */
        mHandler.post(mDrawRunnable);

    }


    /***
     * Runnable method for drawRunnale fuction
     */
    private Runnable mDrawRunnable = new Runnable() {
        /**
         * Run method to toggle the led  and read the temparature value and
         * convert it into celsius.
         * Update the temperature value to firebase
         * and if exception then set led to on for indication
         *
         */
        @Override
        public void run() {
            try {
                mLedGpio.setValue(!mLedGpio.getValue());
                float d= readReg1(mDevice,ADA5_ADDRESS_LSB);
                float voltage= (d*Ref_volatage/1024);
                Temparature= (voltage-500)/10;
                Log.w(TAG, "Temparature in Celcius:"+ Temparature);


                try {
                    //set the motor speed according to temperature
                    //Check the temperature if its fluctuating (Noise) too much then
                    // don't change the speed of motor
                    if(lasttemp-Temparature<5){
                        ADA5.setValue(Temparature);
                        Date currentTime=Calendar.getInstance().getTime();
                        Timestamp.setValue(currentTime.toString());
                        set_motor();
                        lasttemp=Temparature;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mLedGpio.setValue(true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Log.w(TAG, "Unable to read data", e);
            }
            //call the handler
            mHandler.postDelayed(mDrawRunnable,1000);

        }

    };

    /**
     * Set the motor speed according to the temparature
     * update the timestamp on firebase
     * @throws IOException
     * @throws InterruptedException
     */
    public void set_motor() throws IOException, InterruptedException {
        Log.w(TAG, "Set dutycycle for motor");

        if(Temparature >15 && Temparature <=18)
        {
            Log.w(TAG, "Set dutycycle for motor : 30");
            setReg(mDevice,PWM6_ADDRESS,DUTYCYCLE_30);
            PWM3.setValue((int) DUTYCYCLE_30);
        }
        else if(Temparature >18 && Temparature <=22)
        {
            Log.w(TAG, "Set dutycycle for motor : 50");
            setReg(mDevice,PWM6_ADDRESS,DUTYCYCLE_50);
            PWM3.setValue((int) DUTYCYCLE_50);
        }
        else if(Temparature >22 && Temparature <=25)
        {
            Log.w(TAG, "Set dutycycle for motor : 70");
            setReg(mDevice,PWM6_ADDRESS,DUTYCYCLE_70);
            PWM3.setValue((int) DUTYCYCLE_70);
        }
        else  if(Temparature <=15)
        {
            Log.w(TAG, "Set dutycycle for motor : 80");
            setReg(mDevice,PWM6_ADDRESS,DUTYCYCLE_80);

            PWM3.setValue((int) DUTYCYCLE_80);
        }
        else{
            Log.w(TAG, "Set dutycycle for motor :40");
            setReg(mDevice,PWM6_ADDRESS,DUTYCYCLE_40);
            PWM3.setValue((int) DUTYCYCLE_40);

        }
        Date currentTime=Calendar.getInstance().getTime();
        Timestamp.setValue(currentTime.toString());

    }

    /**
     * Set the Led according to PWM valuse set by user
     * update the timestamp on firebase
     * @throws IOException
     */

    public void set_LEDS()throws IOException {
        String hex =Integer.toHexString(pwm3_val);
        byte value= (byte) Integer.parseInt( hex,16);
        setReg(mDevice,PWM3_ADDRESS,value);
        String hex_1 =Integer.toHexString(pwm4_val);
        byte value_1= (byte) Integer.parseInt( hex_1,16);
        setReg(mDevice,PWM4_ADDRESS,value_1);
        String hex_2 =Integer.toHexString(pwm5_val);
        byte value_2= (byte) Integer.parseInt( hex_2,16);
        setReg(mDevice,PWM5_ADDRESS,value_2);
        Date currentTime=Calendar.getInstance().getTime();
        Timestamp.setValue(currentTime.toString());
    }

    /**
     * Update the value of ADCs based on the DAC to firebase
     * if DAC value is not between the 0-32 then don't update the ADC
     * @throws IOException
     */
    public void set_ADC_DAC() throws IOException
    {
        String hex =Integer.toHexString(DAC_val);
        byte value= (byte) Integer.parseInt( hex,16);
        Log.w(TAG, "value :"+value);
        if(value >= 0x00 && value <= 0x20 )
        {
            setReg(mDevice,DAC1_ADDRESS,value);
        }
        int ADC3_val =readReg1(mDevice,ADC3_ADDRESS_LSB);
        ADC3.setValue(ADC3_val);
        Log.w(TAG, "ADC3 value :"+ADC3_val);
        int ADC4_val =readReg1(mDevice,ADC4_ADDRESS_LSB);
        ADC4.setValue(ADC4_val);
        Log.w(TAG, "ADC4 value :"+ADC4_val);
        int ADC5_val =readReg1(mDevice,ADC5_ADDRESS_LSB);
        ADC5.setValue(ADC5_val);
        Log.w(TAG, "ADC5 value :"+ADC5_val);
    }

    /**
     *  Modify the contents of a single register
     * @param device
     * @param address
     * @param value1
     * @throws IOException
     */
    public void setReg(I2cDevice device, int address,byte value1) throws IOException {
        //first clear the register value and then write the new value
        byte value=0x00;
        value |= value1;
        Log.w(TAG, "new value :"+value);
        device.writeRegByte(address,  value);
    }

    /**
     * Read a register block-two blocks
     * LSB and MSB both
     * @param device
     * @param startAddress
     * @return data
     * @throws IOException
     */
    public int readReg1(I2cDevice device, int startAddress) throws IOException {
        int data= device.readRegWord(startAddress);
        return data;
    }

    /**
     * Read a register block-single block
     * @param device
     * @param startAddress
     * @return
     * @throws IOException
     */
    public int readReg(I2cDevice device, int startAddress) throws IOException {
        // Read three consecutive register values
        int data= device.readRegByte(startAddress);
        return data;
    }

    /**
     * onDestroy method to close the
     * opened peripheral object
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C device", e);
            }
        }
    }

}
