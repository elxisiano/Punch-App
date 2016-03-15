package com.hungarians.punch_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Chronometer;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.app.Activity;
import android.content.Intent;



public class MainActivity extends AppCompatActivity {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    public TextView vistaContador;
    public TextView vistaMensaje;
    public TextView vistaGForce;
    public TextView vistaHPS;
    public TextView vistaTBH;
    public TextView vistaKcal;
    public Chronometer vistaChrono;

    public int numeroGolpes = 0;

    /**
     * Name of the connected device
     */
    public String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    public ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the communication services
     */
    private BluetoothService mCommunicationService = null;

    /**
     * Objeto miembro de la clase StringBuilder para trabajar con los mensajes recibidos
     */
    private StringBuilder strBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Otro tonto que le da al botoncico", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //TODO: Aquí se generan los componentes de la interfaz de usuario

        // Se crean las vistas del contantador y los mensajes
        vistaContador = (TextView)findViewById(R.id.contadorView);
        vistaMensaje = (TextView)findViewById(R.id.messageView);
        vistaGForce = (TextView)findViewById(R.id.gForceView);
        vistaHPS = (TextView)findViewById(R.id.hpsView);
        vistaTBH = (TextView)findViewById(R.id.tbhView);
        vistaKcal = (TextView)findViewById(R.id.kcalView);
        vistaChrono = (Chronometer)findViewById(R.id.chronometer);

        //TODO: PASO 1: Obtención del BluetoothAdapter (Se incluye en la memoria)
        //Obtener el adapter local para Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Si el adapter es null, el dispositivo no soporta Bluetooth
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "No existe adapatador de Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //TODO: PASO 2: Habilitar el Bluetooth (Se incluye en la memoria)
        // Si el Bluetooth no esta encendido, solicita que sea activado.
        // setupCommunication() sera llamado en onActivityResult.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // En caso de que ya esté activado, se configura la comunicación.
        } else if (mCommunicationService == null) {
            setupCommunication();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCommunicationService != null) {
            mCommunicationService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Utilizamos esta condicion en onResume() para cubrir el caso en el que
        // el Bluetooth no fue activado durante el onStart(), se detiene para
        // activarlo...
        // onResume() será llamado cuando la actividad ACTION_REQUEST_ENABLE vuelva.
        if (mCommunicationService != null) {
            // Solo si el estado es STATE_NONE sabemos que no hemos comenzado ya.
            if (mCommunicationService.getState() == BluetoothService.STATE_NONE) {
                // Arrancar el servicio de comunicacion
                mCommunicationService.start();
            }
        }
    }

    /**
     * Configura la interfaz de usuario y las operaciones en segundo plano necesarias para la
     * comunicación.
     */
    private void setupCommunication() {

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<>(this, R.layout.message);

        // Initialize the BluetoothChatService to perform bluetooth connections
        mCommunicationService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mCommunicationService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mCommunicationService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            //FragmentActivity activity = getActivity();
            switch (msg.what) {

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case Constants.MESSAGE_READ:
                    /**
                     * Guarda en un String los mensajes recibidos por Bluetooth, para, por ejemplo
                     * poder mostrarlos por pantalla.
                     * byte[] readBuf = (byte[]) msg.obj;
                     * // construct a string from the valid bytes in the buffer
                     * String readMessage = new String(readBuf, 0, msg.arg1);
                     * mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                     */
                    // TODO: Aqui ponemos las acciones a realizar cuando reciba info del Arduino

                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    strBuilder.append(readMessage);                                                // append string
                    int endOfLineIndex = strBuilder.indexOf("--");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,

                        sumarGolpes();

                        //String checkedMessage = strBuilder.substring(0, endOfLineIndex);
                        //int dataLength = checkedMessage.length();// extract string


                        if (strBuilder.charAt(0) == '#') {
                            String gF = strBuilder.substring(1, 4);
                            String hps = strBuilder.substring(5, 9);
                            String tbh = strBuilder.substring(10, 13);
                            String kcal = strBuilder.substring(14, 17);

                            vistaGForce.setText(gF);    //update the textviews with sensor values
                            vistaHPS.setText(hps);
                            vistaTBH.setText(tbh);
                            vistaKcal.setText(kcal);

                            float gF_value = Float.parseFloat(gF);
                            float tbh_value = Float.parseFloat(tbh);

                            if (numeroGolpes == 1) {
                                vistaChrono.setBase(SystemClock.elapsedRealtime());
                                vistaChrono.start();
                            } else {
                                mostrarMensaje(" ");
                            }

                            if (gF_value < 1.6) {
                                mostrarMensaje("¡Más fuerte!");
                            }

                            if (gF_value > 5.5) {
                                mostrarMensaje("¡Gran golpe!");
                            }

                            if (tbh_value > 4) {
                                mostrarMensaje("¡Más rápido!");
                            }

                            if (tbh_value < 0.25) {
                                mostrarMensaje("¡Buen ritmo!");
                            }


                        }

                            strBuilder.delete(0, strBuilder.length()); // and clear

                        //checkedMessage = " ";
                    }

                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    //if (null != activity) {
                    //Toast.makeText(getApplicationContext(), "Connected to "
                    //        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    Snackbar.make(findViewById(R.id.fab), "Conectado a "+ mConnectedDeviceName,
                            Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    mostrarMensaje("¡Listo para empezar!");
                    // }
                    break;

                case Constants.MESSAGE_TOAST:
                    // if (null != activity) {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    // }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupCommunication();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    //Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mCommunicationService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // TODO: Arreglar la chapucilla esta :)
            case R.id.bluetooth_connect: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.exit: {
                // Sale de la aplicación
                finish();
            }
            case R.id.arduino_test: {
                // Envía el carácter "L" al Arduino
                sendMessage("L");
                return true;
            }
            case R.id.counter_preview: {
                //Aumenta progresivamente el número del contador
                Handler cpHandler = new Handler();
                cpHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sumarGolpes();
                    }
                }, 500);
            }
        }
        return false;
    }

    public void sumarGolpes() {
        numeroGolpes = numeroGolpes + 1;
        String formatoNumeroGolpes = String.format("%03d", numeroGolpes);
        vistaContador.setText(formatoNumeroGolpes);
        //vistaMensaje.setText("");
    }

    public void mostrarMensaje(String mensaje) {
        vistaMensaje.setText(mensaje);
    }
}
