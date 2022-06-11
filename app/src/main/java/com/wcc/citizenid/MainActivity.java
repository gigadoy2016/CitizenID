package com.wcc.citizenid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.acs.smartcard.Features;
import com.acs.smartcard.PinModify;
import com.acs.smartcard.PinVerify;
import com.acs.smartcard.ReadKeyOption;
import com.acs.smartcard.Reader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String[] powerActionStrings = { "Power Down","Cold Reset", "Warm Reset" };
    private static final String[] stateStrings = { "Unknown", "Absent","Present", "Swallowed", "Powered", "Negotiable", "Specific" };

    private static final String[] featureStrings = { "FEATURE_UNKNOWN",
            "FEATURE_VERIFY_PIN_START", "FEATURE_VERIFY_PIN_FINISH",
            "FEATURE_MODIFY_PIN_START", "FEATURE_MODIFY_PIN_FINISH",
            "FEATURE_GET_KEY_PRESSED", "FEATURE_VERIFY_PIN_DIRECT",
            "FEATURE_MODIFY_PIN_DIRECT", "FEATURE_MCT_READER_DIRECT",
            "FEATURE_MCT_UNIVERSAL", "FEATURE_IFD_PIN_PROPERTIES",
            "FEATURE_ABORT", "FEATURE_SET_SPE_MESSAGE",
            "FEATURE_VERIFY_PIN_DIRECT_APP_ID",
            "FEATURE_MODIFY_PIN_DIRECT_APP_ID", "FEATURE_WRITE_DISPLAY",
            "FEATURE_GET_KEY", "FEATURE_IFD_DISPLAY_PROPERTIES",
            "FEATURE_GET_TLV_PROPERTIES", "FEATURE_CCID_ESC_COMMAND" };

    private static final String[] propertyStrings = { "Unknown", "wLcdLayout",
            "bEntryValidationCondition", "bTimeOut2", "wLcdMaxCharacters",
            "wLcdMaxLines", "bMinPINSize", "bMaxPINSize", "sFirmwareID",
            "bPPDUSupport", "dwMaxAPDUDataSize", "wIdVendor", "wIdProduct" };

    private static final int DIALOG_VERIFY_PIN_ID = 0;
    private static final int DIALOG_MODIFY_PIN_ID = 1;
    private static final int DIALOG_READ_KEY_ID = 2;
    private static final int DIALOG_DISPLAY_LCD_MESSAGE_ID = 3;

    private static final int MAX_LINES = 25;
    private UsbManager mManager;
    private Reader mReader;
    private PendingIntent mPermissionIntent;

    /*---------------------------------------------------*/
    private TextView mResponseTextView;
    private Spinner mReaderSpinner;
    private ArrayAdapter<String> mReaderAdapter;
    private Spinner mSlotSpinner;
    private ArrayAdapter<String> mSlotAdapter;
    private Spinner mPowerSpinner;
    private Button mListButton;
    private Button mOpenButton;
    private Button mCloseButton;
    private Button mGetStateButton;
    private Button mPowerButton;
    private Button mGetAtrButton;
    private CheckBox mT0CheckBox;
    private CheckBox mT1CheckBox;
    private Button mSetProtocolButton;
    private Button mGetProtocolButton;
    private EditText mCommandEditText;
    private Button mTransmitButton;
    private EditText mControlEditText;
    private Button mControlButton;
    private Button mGetFeaturesButton;
    private Button mVerifyPinButton;
    private Button mModifyPinButton;
    private Button mReadKeyButton;
    private Button mDisplayLcdMessageButton;

    private Features mFeatures = new Features();
    private PinVerify mPinVerify = new PinVerify();
    private PinModify mPinModify = new PinModify();
    private ReadKeyOption mReadKeyOption = new ReadKeyOption();
    private String mLcdMessage;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Open reader
                            logMsg("Opening reader: " + device.getDeviceName() + "...");
                            new OpenTask().execute(device);
                        }
                    } else {
                        logMsg("Permission denied for device "+ device.getDeviceName());
                        // Enable open button
                        mOpenButton.setEnabled(true);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    // Update reader list
                    mReaderAdapter.clear();
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        if (mReader.isSupported(device)) {
                            mReaderAdapter.add(device.getDeviceName());
                        }
                    }
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && device.equals(mReader.getDevice())) {
                        // Disable buttons
                        mCloseButton.setEnabled(false);
                        mSlotSpinner.setEnabled(false);
                        mGetStateButton.setEnabled(false);
                        mPowerSpinner.setEnabled(false);
                        mPowerButton.setEnabled(false);
//                        mGetAtrButton.setEnabled(false);
//                        mT0CheckBox.setEnabled(false);
//                        mT1CheckBox.setEnabled(false);
                        mSetProtocolButton.setEnabled(false);
                        mGetProtocolButton.setEnabled(false);
//                        mTransmitButton.setEnabled(false);
//                        mControlButton.setEnabled(false);
//                        mGetFeaturesButton.setEnabled(false);
//                        mVerifyPinButton.setEnabled(false);
//                        mModifyPinButton.setEnabled(false);
//                        mReadKeyButton.setEnabled(false);
//                        mDisplayLcdMessageButton.setEnabled(false);

                        // Clear slot items
                        //mSlotAdapter.clear();

                        // Close reader
                        logMsg("Closing reader...");
                        new CloseTask().execute();
                    }
                }
            }
        }
    };

    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {
        @Override
        protected Exception doInBackground(UsbDevice... params) {
            Exception result = null;
            try {
                mReader.open(params[0]);
            } catch (Exception e) {
                result = e;
            }
            return result;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                logMsg(result.toString());
            } else {
                logMsg("Reader name: " + mReader.getReaderName());
                int numSlots = mReader.getNumSlots();
                logMsg("Number of slots: " + numSlots);
                // Add slot items
                mSlotAdapter.clear();
                for (int i = 0; i < numSlots; i++) {
                    mSlotAdapter.add(Integer.toString(i));
                }
                // Remove all control codes
                mFeatures.clear();
                // Enable buttons
                mCloseButton.setEnabled(true);
                mSlotSpinner.setEnabled(true);
                mGetStateButton.setEnabled(true);
                mPowerSpinner.setEnabled(true);
                mPowerButton.setEnabled(true);
//                mGetAtrButton.setEnabled(true);
//                mT0CheckBox.setEnabled(true);
//                mT1CheckBox.setEnabled(true);
                mSetProtocolButton.setEnabled(true);
                mGetProtocolButton.setEnabled(true);
//                mTransmitButton.setEnabled(true);
//                mControlButton.setEnabled(true);
//                mGetFeaturesButton.setEnabled(true);
//                mReadKeyButton.setEnabled(true);
//                mDisplayLcdMessageButton.setEnabled(true);
            }
        }
    }
    private class CloseTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mReader.close();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            mOpenButton.setEnabled(true);
        }
    }

    private class PowerParams {
        public int slotNum;
        public int action;
    }

    private class PowerResult {
        public byte[] atr;
        public Exception e;
    }

    private class PowerTask extends AsyncTask<PowerParams, Void, PowerResult> {
        @Override
        protected PowerResult doInBackground(PowerParams... params) {
            PowerResult result = new PowerResult();
            try {
                result.atr = mReader.power(params[0].slotNum, params[0].action);
            } catch (Exception e) {
                result.e = e;
            }
            return result;
        }
        @Override
        protected void onPostExecute(PowerResult result) {
            if (result.e != null) {
                logMsg(result.e.toString());
            } else {
                // Show ATR
                if (result.atr != null) {
                    logMsg("ATR:");
                    logBuffer(result.atr, result.atr.length);
                } else {
                    logMsg("ATR: None");
                }
            }
        }

    }
    private class SetProtocolParams {
        public int slotNum;
        public int preferredProtocols;
    }

    private class SetProtocolResult {
        public int activeProtocol;
        public Exception e;
    }
    private class SetProtocolTask extends AsyncTask<SetProtocolParams, Void, SetProtocolResult> {
        @Override
        protected SetProtocolResult doInBackground(SetProtocolParams... params) {
            SetProtocolResult result = new SetProtocolResult();
            try {
                result.activeProtocol = mReader.setProtocol(params[0].slotNum,
                        params[0].preferredProtocols);
            } catch (Exception e) {
                result.e = e;
            }
            return result;
        }

        @Override
        protected void onPostExecute(SetProtocolResult result) {
            if (result.e != null) {
                logMsg(result.e.toString());
            } else {
                String activeProtocolString = "Active Protocol: ";
                switch (result.activeProtocol) {
                    case Reader.PROTOCOL_T0:
                        activeProtocolString += "T=0";
                        break;
                    case Reader.PROTOCOL_T1:
                        activeProtocolString += "T=1";
                        break;
                    default:
                        activeProtocolString += "Unknown";
                        break;
                }
                // Show active protocol
                logMsg(activeProtocolString);
            }
        }
    }

    //--- init this Program
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get USB manager
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Initialize reader
        mReader = new Reader(mManager);

        mReader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {
                if (prevState < Reader.CARD_UNKNOWN || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }
                if (currState < Reader.CARD_UNKNOWN || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }
                // Create output string
                final String outputString = "Slot " + slotNum + ": " + stateStrings[prevState] + " -> "  + stateStrings[currState];
                // Show output
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logMsg(outputString);
                    }
                });
            }
        });

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);

        // Initialize response text view
        mResponseTextView = findViewById(R.id.main_text_view_response);
        mResponseTextView.setMovementMethod(new ScrollingMovementMethod());
        mResponseTextView.setMaxLines(MAX_LINES);
        mResponseTextView.setText("");

        // Initialize reader spinner
        mReaderAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
                mReaderAdapter.add(device.getDeviceName());
            }
        }
        mReaderSpinner = (Spinner) findViewById(R.id.main_spinner_reader);
        mReaderSpinner.setAdapter(mReaderAdapter);

        // Initialize slot spinner
        mSlotAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        mSlotSpinner = (Spinner) findViewById(R.id.main_spinner_slot);
        mSlotSpinner.setAdapter(mSlotAdapter);

        // Initialize power spinner
        ArrayAdapter<String> powerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, powerActionStrings);
        mPowerSpinner = (Spinner) findViewById(R.id.main_spinner_power);
        mPowerSpinner.setAdapter(powerAdapter);
        mPowerSpinner.setSelection(Reader.CARD_WARM_RESET);

        //====================================================================
        mPowerButton = findViewById(R.id.button_1);
        // Initialize list button
        mListButton = (Button) findViewById(R.id.main_button_list);
        mListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReaderAdapter.clear();
                for (UsbDevice device : mManager.getDeviceList().values()) {
                    if (mReader.isSupported(device)) {
                        mReaderAdapter.add(device.getDeviceName());
                    }
                }
            }
        });

        // Initialize open button
        mOpenButton = (Button) findViewById(R.id.main_button_open);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean requested = false;
                // Disable open button
                mOpenButton.setEnabled(false);
                String deviceName = (String) mReaderSpinner.getSelectedItem();
                if (deviceName != null) {
                    // For each device
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        // If device name is found
                        if (deviceName.equals(device.getDeviceName())) {
                            // Request permission
                            try{
                                mManager.requestPermission(device,mPermissionIntent);
                                //mResponseTextView.setText("Good");
                            }catch (Exception e){
                                mResponseTextView.setText("Err");
                            }
                            requested = true;
                            break;
                        }
                    }
                }
                if (!requested) {
                    // Enable open button
                    mOpenButton.setEnabled(true);
                }
            }
        });

        // Initialize close button
        mCloseButton = (Button) findViewById(R.id.main_button_close);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable buttons
                mCloseButton.setEnabled(false);
                mSlotSpinner.setEnabled(false);
                mGetStateButton.setEnabled(false);
                mPowerSpinner.setEnabled(false);
                mPowerButton.setEnabled(false);
//                mGetAtrButton.setEnabled(false);
//                mT0CheckBox.setEnabled(false);
//                mT1CheckBox.setEnabled(false);
                mSetProtocolButton.setEnabled(false);
                mGetProtocolButton.setEnabled(false);
//                mTransmitButton.setEnabled(false);
//                mControlButton.setEnabled(false);
//                mGetFeaturesButton.setEnabled(false);
//                mVerifyPinButton.setEnabled(false);
//                mModifyPinButton.setEnabled(false);
//                mReadKeyButton.setEnabled(false);
//                mDisplayLcdMessageButton.setEnabled(false);
//                // Clear slot items
//                mSlotAdapter.clear();
                // Close reader
                logMsg("Closing reader...");
                new CloseTask().execute();
                mOpenButton.setEnabled(true);
            }
        });

        // Initialize get state button
        mGetStateButton = (Button) findViewById(R.id.main_button_get_state);
        mGetStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get slot number
                int slotNum = mSlotSpinner.getSelectedItemPosition();
                // If slot is selected
                if (slotNum != Spinner.INVALID_POSITION) {
                    try {
                        // Get state
                        logMsg("Slot " + slotNum + ": Getting state...");
                        int state = mReader.getState(slotNum);
                        if (state < Reader.CARD_UNKNOWN
                                || state > Reader.CARD_SPECIFIC) {
                            state = Reader.CARD_UNKNOWN;
                        }
                        logMsg("State: " + stateStrings[state]);
                    } catch (IllegalArgumentException e) {
                        logMsg(e.toString());
                    }
                }
            }
        });

        // Initialize power button
        mPowerButton = (Button) findViewById(R.id.main_button_power);
        mPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get slot number
                int slotNum = mSlotSpinner.getSelectedItemPosition();
                // Get action number
                int actionNum = mPowerSpinner.getSelectedItemPosition();
                // If slot and action are selected
                if (slotNum != Spinner.INVALID_POSITION
                        && actionNum != Spinner.INVALID_POSITION) {
                    if (actionNum < Reader.CARD_POWER_DOWN
                            || actionNum > Reader.CARD_WARM_RESET) {
                        actionNum = Reader.CARD_WARM_RESET;
                    }
                    // Set parameters
                    PowerParams params = new PowerParams();
                    params.slotNum = slotNum;
                    params.action = actionNum;
                    // Perform power action
                    logMsg("Slot " + slotNum + ": "  + powerActionStrings[actionNum] + "...");
                    new PowerTask().execute(params);
                }
            }
        });

        mPowerButton = (Button) findViewById(R.id.main_button_set_protocol);
        mPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get slot number
                int slotNum = mSlotSpinner.getSelectedItemPosition();
                // Get action number
                int actionNum = mPowerSpinner.getSelectedItemPosition();
                // If slot and action are selected
                if (slotNum != Spinner.INVALID_POSITION
                        && actionNum != Spinner.INVALID_POSITION) {
                    if (actionNum < Reader.CARD_POWER_DOWN
                            || actionNum > Reader.CARD_WARM_RESET) {
                        actionNum = Reader.CARD_WARM_RESET;
                    }
                    // Set parameters
                    PowerParams params = new PowerParams();
                    params.slotNum = slotNum;
                    params.action = actionNum;

                    // Perform power action
                    logMsg("Slot " + slotNum + ": "
                            + powerActionStrings[actionNum] + "...");
                    new PowerTask().execute(params);
                }
            }
        });

        // Initialize set protocol button
        mSetProtocolButton = (Button) findViewById(R.id.main_button_set_protocol);
        mSetProtocolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get slot number
                int slotNum = mSlotSpinner.getSelectedItemPosition();


                // If slot is selected
                if (slotNum != Spinner.INVALID_POSITION) {
                    int preferredProtocols = Reader.PROTOCOL_UNDEFINED;
                    String preferredProtocolsString = "";
//                    if (mT0CheckBox.isChecked()) {
//                        preferredProtocols |= Reader.PROTOCOL_T0;
                        preferredProtocolsString = "T=0";
//                    }
//                    if (mT1CheckBox.isChecked()) {
//                        preferredProtocols |= Reader.PROTOCOL_T1;
//                        if (preferredProtocolsString != "") {
                            preferredProtocolsString += "/";
//                        }
                        preferredProtocolsString += "T=1";
//                    }
//                    if (preferredProtocolsString == "") {
//                        preferredProtocolsString = "None";
//                    }
                    // Set Parameters
                    SetProtocolParams params = new SetProtocolParams();
                    params.slotNum = slotNum;
                    params.preferredProtocols = preferredProtocols;
                    // Set protocol
                    logMsg("Slot " + slotNum + ": Setting protocol to "
                            + preferredProtocolsString + "...");
                    new SetProtocolTask().execute(params);
                }
            }
        });

        // Initialize get active protocol button
        mGetProtocolButton = (Button) findViewById(R.id.main_button_get_protocol);
        mGetProtocolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get slot number
                int slotNum = mSlotSpinner.getSelectedItemPosition();
                // If slot is selected
                if (slotNum != Spinner.INVALID_POSITION) {
                    try {
                        // Get active protocol
                        logMsg("Slot " + slotNum + ": Getting active protocol...");
                        int activeProtocol = mReader.getProtocol(slotNum);
                        // Show active protocol
                        String activeProtocolString = "Active Protocol: ";
                        switch (activeProtocol) {
                            case Reader.PROTOCOL_T0:
                                activeProtocolString += "T=0";
                                break;
                            case Reader.PROTOCOL_T1:
                                activeProtocolString += "T=1";
                                break;
                            default:
                                activeProtocolString += "Unknown";
                                break;
                        }
                        logMsg(activeProtocolString);
                    } catch (IllegalArgumentException e) {
                        logMsg(e.toString());
                    }
                }
            }
        });
    }


    public void btn1Click(View v){
        mResponseTextView.setText("Hello 000");
    }

    /**
     * Logs the message.
     *
     * @param msg
     *            the message.
     */
    private void logMsg(String msg) {

        DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
        Date date = new Date();
        String oldMsg = mResponseTextView.getText().toString();

        mResponseTextView
                .setText(oldMsg + "\n" + dateFormat.format(date) + msg);

        if (mResponseTextView.getLineCount() > MAX_LINES) {
            mResponseTextView.scrollTo(0,
                    (mResponseTextView.getLineCount() - MAX_LINES)
                            * mResponseTextView.getLineHeight());
        }
    }

    /**
     * Logs the contents of buffer.
     *
     * @param buffer
     *            the buffer.
     * @param bufferLength
     *            the buffer length.
     */
    private void logBuffer(byte[] buffer, int bufferLength) {
        String bufferString = "";
        for (int i = 0; i < bufferLength; i++) {
            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }
            if (i % 16 == 0) {
                if (bufferString != "") {
                    logMsg(bufferString);
                    bufferString = "";
                }
            }
            bufferString += hexChar.toUpperCase() + " ";
        }
        if (bufferString != "") {
            logMsg(bufferString);
        }
    }

}