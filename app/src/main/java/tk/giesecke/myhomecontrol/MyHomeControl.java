package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.co.senab.photoview.PhotoViewAttacher;

import static tk.giesecke.myhomecontrol.MessageListener.bytesLoadRcvd;
import static tk.giesecke.myhomecontrol.MessageListener.bytesLoadSend;
import static tk.giesecke.myhomecontrol.MessageListener.bytesMsgsRcvd;
import static tk.giesecke.myhomecontrol.MessageListener.bytesMsgsSend;
import static tk.giesecke.myhomecontrol.MessageListener.clientsConn;
import static tk.giesecke.myhomecontrol.MessageListener.mqttClients;
import static tk.giesecke.myhomecontrol.MessageListener.unSubscribeBrokerStatus;
import static tk.giesecke.myhomecontrol.NsdHelper.mServicesHosts;

public class MyHomeControl extends AppCompatActivity implements View.OnClickListener
		, AdapterView.OnItemClickListener , LoadImageTask.Listener , SeekBar.OnSeekBarChangeListener {

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-MAIN";

	/** Access to activities shared preferences */
	private static SharedPreferences mPrefs;
	/* Name of shared preferences */
	public static final String sharedPrefName = "MyHomeControl";
	/** Context of this application */
	@SuppressLint("StaticFieldLeak")
	private Context appContext;
	/** Id of menu, needed to set user selected icons and device names */
	private Menu abMenu;
	/** Id's of menu items */
//	int action_lightControl_id = 0;
//	int action_security_id = 1;
//	int action_solar_id = 2;
//	int action_aircon_id = 3;
//	int action_close_id = 4;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_selAlarm_id = 5;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_selWarning_id = 6;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_locations_id = 7;
//	int action_refresh_id = 8;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_debug_id = 9;
	@SuppressWarnings("FieldCanBeLocal")
	private final
	int action_devDebug_id = 10;
	/** Id's of views */
	private final int view_security_id = 0;
	final static int view_solar_id = 1;
	final static int view_aircon_id = 2;
	@SuppressWarnings("FieldCanBeLocal")
	private final int view_devDebug_id = 3;
	@SuppressWarnings("FieldCanBeLocal")
	private final int view_seccam_id = 4;
	private final int view_lights_id = 5;
	/** Visible view 0 = security, 1 = solar panel, 2 = aircon */
	private int visibleView = 1;
	/** Flag for debug output */
	private boolean showDebug = false;
	/** The view of the main UI */
	private View appView;
	/** Text view for the date */
	private TextView chartTitle;

	/** Shared preferences value for last shown view */
	private final String prefsLastView = "lastView";
	/** Shared preferences value for show debug messages flag */
	public static final String prefsShowDebug = "showDebug";

	/** Shared preferences value security alarm sound */
	public static final String prefsSecurityAlarm = "secAlarm";

	/** Shared preferences value for solar alarm sound */
	public static final String prefsSolarWarning = "solarAlarm";
	/** Shared preferences value for last synced month */
	public static final String prefsSolarSynced = "solarLastSynced";
	/** Shared preferences value for number solar widgets placed */
	public static final String prefsSolarWidgetNum = "solarWidgetNum";
	/** Shared preferences value for large widget */
	public static final String prefsSolarWidgetSize = "solarWidgetSizeLarge";

	/** Shared preferences value for last selected device */
	private final String prefsSelDevice = "airconSelDevice";
	/** Shared preferences value for show debug messages flag */
	private final String prefsLocationName = "airconLocation";
	/** Shared preferences value for show debug messages flag */
	private final String prefsDeviceIcon = "airconIcon";

	/** Shared preferences value for dimmed brightness values */
	static final String prefsLightBedDim = "lightBedDimVal";

	/** View for selecting device to change icon and device name */
	private View locationSettingsView;
	/** View of aircon device name and icon change dialog */
	private View airconDialogView;
	/** Button ids from location selection dialog */
	private final int[] buttonIds = {
			R.id.dia_sel_device0,
			R.id.dia_sel_device1,
			R.id.dia_sel_device2};
	/** Resource ids of drawables for the icons */
	private final int[] iconIDs = {R.drawable.ic_bathroom,
			R.drawable.ic_bedroom,
			R.drawable.ic_dining,
			R.drawable.ic_entertainment,
			R.drawable.ic_kids,
			R.drawable.ic_kitchen,
			R.drawable.ic_livingroom,
			R.drawable.ic_office};
	/* Resource ids of the icon buttons */
	private final int[] iconButtons = {R.id.im_bath,
			R.id.im_bed,
			R.id.im_dining,
			R.id.im_entertain,
			R.id.im_kids,
			R.id.im_kitchen,
			R.id.im_living,
			R.id.im_office};
	/** Index of device handled in dialog box */
	private int dlgDeviceIndex;
	/** R.id of selected icon for a device */
	private int dlgIconIndex;

	/** Flag for sound selector (true = security alarm, false = solar panel warning) */
	private boolean isSelAlarm = true;

	// SPmonitor, Security front, Security back, Aircon 1, Aircon 2, Aircon 3, Monitor, Camera front
	/** List of device names */
	static final String[] deviceNames = {"spMonitor", "sf1", "sb1", "fd1", "ca1", "mhc", "moni", "cm1", "lb1"};
	/** List of potential control device availability */
	private static final boolean[] deviceIsOn = {false, false, false, false, false, false, false, false, false};
	/** List of IP addresses of found devices */
	private static final String[] deviceIPs = {"", "", "", "", "", "", "", "", ""};
	/** deviceIsOn index for SPmonitor */
	static final int spMonitorIndex = 0;
	/** deviceIsOn index for Security front */
	static final int secFrontIndex = 1;
	/** deviceIsOn index for Security back */
	static final int secBackIndex = 2;
	/** deviceIsOn index for Aircon 1 */
	static final int aircon1Index = 3;
	/** deviceIsOn index for Aircon 2 */
	private static final int aircon2Index = 4;
	/** deviceIsOn index for Aircon 3 */
	private static final int aircon3Index = 5;
	/** deviceIsOn index for Monitoring display */
	private static final int moniIndex = 6;
	/** deviceIsOn index for front yard camera */
	private static final int cam1Index = 7;
	/** deviceIsOn index for bedroom lights */
	private static final int lb1Index = 8;

	// Security view related
	/** Security view */
	private RelativeLayout secView = null;
	/** TextView for status message display in security view */
	private TextView secStatus;
	/** ImageView to show status of alarm enabled front sensor */
	private ImageView ivAlarmStatus;
	/** ImageView to show status of light enabled front sensor */
	private ImageView ivLightStatus;
	/** TableLayout to show status of back alarm system */
	private TableLayout secBackView;
	/** ImageView to show status of alarm enabled back sensor */
	private ImageView ivAlarmStatusBack;
	/** ImageView to show status of light enabled back sensor */
	private ImageView ivLightStatusBack;
	/** Check box for auto activation of alarm */
	private CheckBox secAutoAlarmFront;
	/** Check box for auto activation of alarm */
	private CheckBox secAutoAlarmBack;
	/** Clickable text view to change activation times */
	private TextView secChangeAlarm;

	/** Status flag for alarm front sensor */
	static boolean hasAlarmOnFront = true;
	/** Status flag for alarm back sensor */
	static boolean hasAlarmOnBack = true;
	/** Status flag for auto alarm front sensor */
	static boolean hasAutoOnFront = true;
	/** Status flag for auto alarm back sensor */
	static boolean hasAutoOnBack = true;

	/** Auto activation on time as string */
	static String secAutoOn;
	/** Auto activation off time as string */
	static String secAutoOff;
	/** Auto activation on time as integer */
	static int secAutoOnStored;
	/** Auto activation off time as integer */
	static int secAutoOffStored;

	/** Array list with available alarm names */
	private ArrayList<String> notifNames = new ArrayList<>();
	/** Array list with available alarm uri's */
	private ArrayList<String> notifUri = new ArrayList<>();
	/** Selected alarm name */
	private String notifNameSel = "";
	/** Selected alarm uri */
	private String notifUriSel = "";

	// Solar monitor view related
	/** Solar panel view */
	private RelativeLayout solView = null;
	/** TextView for status message display in solar panel view */
	private TextView solStatus;
	/** Array with existing log dates on the Arduino */
	private static final List<String> logDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int logDatesIndex = 0;
	/** Array with existing log dates on the Arduino */
	private static final List<String> lastLogDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int lastLogDatesIndex = 0;
	/** Flag for showing last month */
	private static boolean showingLast = false;
	/** Flag for showing a log */
	static boolean showingLog = false;
	// Flag for database empty */
	static boolean dataBaseIsEmpty = true;

	/** Instance of DataBaseHelper for this month*/
	private DataBaseHelper dbHelperNow;
	/** Instance of DataBaseHelper for last month*/
	private DataBaseHelper dbHelperLast;

	/** Today's year-month database name */
	private static String[] dbNamesList = new String[2];
	/** Flag for last month update request */
	private static boolean needLastMonth = false;

	/** AsyncTask for updating current month database */
	private static AsyncTask atNow;
	/** AsyncTask for updating last month database */
	private static AsyncTask atLast;

	// Aircon view related
	/** Aircon control view */
	private RelativeLayout airView = null;
	/** FujiDenzo control view */
	private RelativeLayout airFDView = null;
	/** Carrier control view */
	private RelativeLayout airCAView = null;
	/** TextView for status message display in aircon view */
	private TextView airStatus;
	/** Light of button to switch consumption control for FujiDenzo layout */
	private View btAutoLightFD;
	/** Light of button to switch consumption control for Carrier layout */
	private View btAutoLightCA;
	/** Light of button to switch on/off for FujiDenzo layout */
	private View btOnOffLightFD;
	/** Light of button to switch on/off for Carrier layout */
	private View btOnOffLightCA;
	/** Light of button to switch fan to high speed for FujiDenzo layout */
	private View btFanHighLightFD;
	/** Light of button to switch fan to medium speed for FujiDenzo layout */
	private View btFanMedLightFD;
	/** Light of button to switch fan to low speed for FujiDenzo layout */
	private View btFanLowLightFD;
	/** Light of button to switch to cool mode for FujiDenzo layout */
	private View btCoolLightFD;
	/** Light of button to switch to cool mode for Carrier layout */
	private View btCoolLightCA;
	/** Light of button to switch to dry mode for FujiDenzo layout */
	private View btDryLightFD;
	/** Light of button to switch to dry mode for Carrier layout */
	private View btDryLightCA;
	/** Light of button to switch to fan mode for FujiDenzo layout */
	private View btFanLightFD;
	/** Light of button to switch to fan mode for Carrier layout */
	private View btFanLightCA;
	/** Light of button to switch on sweep for Carrier layout */
	private View btSweepLightCA;
	/** Light of button to switch on turbo mode for Carrier layout */
	private View btTurboLightCA;
	/** Light of button to switch on ion mode for Carrier layout */
	private View btIonLightCA;
	/** Light of button to switch on auto temp function for Carrier layout */
	private View btAutomLightCA;
	/** Button to switch fan speed for Carrier layout */
	private Button btFanCA;
	/** Consumption value display for FujiDenzo layout */
	private TextView txtConsValFD;
	/** Temperature value display for FujiDenzo layout */
	private TextView txtTempValFD;
	/** Status value display for FujiDenzo layout */
	private TextView txtAutoStatusValFD;
	/** Consumption value display for Carrier layout */
	private TextView txtConsValCA;
	/** Temperature value display for Carrier layout */
	private TextView txtTempValCA;
	/** Status value display for Carrier layout */
	private TextView txtAutoStatusValCA;

	/** Timer button for FujiDenzo layout */
	private Button btTimerFD;
	/** Timer button for Carrier layout */
	private Button btTimerCA;

	/** Color for activated button */
	private static int colorRed;
	/** Color for deactivated button */
	private static int colorGrey;
	/** Color for deactivated timer button */
	private static int colorGreen;
	/** Color for activated timer button */
	private static int colorOrange;

	/** ID of the selected device */
	private static int selDevice = 0;
	/** IP address of the selected device */
	private static final String[] espIP = {"192.168.0.142",
			"192.168.0.143",
			"192.168.0.144",
			"192.168.0.145",
			"192.168.0.146",
			"192.168.0.147",
			"192.168.0.148",
			"192.168.0.149"};
	/** Name of the device */
	private static final String[] deviceName = {"", "", "", "", "", "", "", ""};
	/** Layout version for the device */
	private static final int[] deviceType = {99, 99, 99, 99, 99, 99, 99, 99};
	/** Valid device type ids */
	private static final int FUJIDENZO = 0;
	private static final int CARRIER = 1;
	private static final int OTHER_AIRCON = 2;
	/** Location of the device */
	private final String[] locationName = {"Office", "Living", "Bedroom", "", "", "", "", "Test"};
	/** Icon for the device */
	private final int[] deviceIcon = {7, 6, 1, 0, 0, 0, 0, 3};
	/** Fan speed of device */
	private static final int[] fanStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Mode status of device */
	private static final int[] modeStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Power status of device */
	private static final int[] powerStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Cooling temperature of device */
	private static final int[] coolStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Timer setting of device */
	private static final int[] deviceTimer = {1, 1, 1, 1, 1, 1, 1, 1};
	/** Off time of timer of device */
	private static final String[] deviceOffTime = {"", "", "", "", "", "", "", ""};
	/** Consumption status of device (only from master device */
	private static double consStatus = 0;
	/** Auto power status of device (only from master device */
	private static int autoStatus = 0;
	/** Auto power enabled status of device */
	private static final int[] autoOnStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Sweep enabled status of device */
	private static final int[] sweepStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Turbo enabled status of device */
	private static final int[] turboStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Ion enabled status of device */
	private static final int[] ionStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Timer on status of device */
	private static final int[] timerStatus = {0, 0, 0, 0, 0, 0, 0, 0};

	private static final String CMD_ON_OFF = "00";

	private static final String CMD_MODE_AUTO = "10";
	private static final String CMD_MODE_COOL = "11";
	private static final String CMD_MODE_DRY = "12";
	private static final String CMD_MODE_FAN = "13";

	private static final String CMD_FAN_HIGH = "20";
	private static final String CMD_FAN_MED = "21";
	private static final String CMD_FAN_LOW = "22";
	private static final String CMD_FAN_SPEED = "23";

	private static final String CMD_TEMP_PLUS = "30";
	private static final String CMD_TEMP_MINUS = "31";

	public static final String CMD_OTHER_TIMER = "40";
	private static final String CMD_OTHER_SWEEP = "41";
	private static final String CMD_OTHER_TURBO = "42";
	private static final String CMD_OTHER_ION = "43";

	private static final String CMD_AUTO_ON = "98";
	private static final String CMD_AUTO_OFF = "99";

	/** Debug device view */
	private LinearLayout debugView = null;
	/** String with received debug messages */
	private String debugMsgs = "";
	/** String of highlighted text */
	private String highlightText = "";

	/** Text of snackbar view */
	private String snackBarText = "";

	/** Debug device view */
	private RelativeLayout seccamView = null;
	/** Zoomable image view attacher */
	private PhotoViewAttacher snapShotAttacher = null;
	/** List with available images on image server */
	private List<String> availImages;

	/** Debug device view */
	private RelativeLayout lightsView = null;
	/** Current value of bed room lights */
	private int lightsBedRoomVal = 0;
	/** Light control seek bar */
	private SeekBar bedRoomValSB;
	/** Textview for current brightness level */
	private TextView bedRoomVal;
	/** URL for bed room light control */
	static final String urlLB1 = "192.168.0.151";

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		intent.putExtra("view", getSharedPreferences(sharedPrefName,0).getInt(prefsLastView,0));
		// set the intent passed from the service to the original intent
		setIntent(intent);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onCreate started");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_home_control);
		/** Instance of the tool bar */
		Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
		if (toolBar != null) {
			setSupportActionBar(toolBar);
		}

		// Enable access to internet
		if (Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

//		mPrefs.edit().remove("MQTT_User").apply();

		// Get context of the application to be reused in Async Tasks
		appContext = this;

		// Get list of last known devices and IP addresses from the preferences
		for (int i=0; i<deviceNames.length; i++) {
			String deviceIP = mPrefs.getString(deviceNames[i],"NA");
			if (!deviceIP.equalsIgnoreCase("NA")) { // device saved?
				deviceIPs[i] = deviceIP;
				deviceIsOn[i] = true;
			} else {
				deviceIsOn[i] = false;
			}
		}

		// Initialize variables for buttons, layouts, views, ...
		setGlobalVar();

		// In case the database is not yet existing, open it once
		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		// Initiate databases (in case they are not existing yet)
		/** Instance of database */
		SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();
		dataBase.beginTransaction();
		dataBase.endTransaction();
		dataBase.close();
		/** Instance of data base */
		dataBase = dbHelperLast.getReadableDatabase();
		dataBase.beginTransaction();
		dataBase.endTransaction();
		dataBase.close();

		String locationStatus;
		if (Utilities.isHomeWiFi(this)) {
			locationStatus = getResources().getString(R.string.at_home);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found home WiFi");
		} else {
			locationStatus = getResources().getString(R.string.not_home);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Didn't find home WiFi");
		}
		secStatus.setText(locationStatus);
		airStatus.setText(locationStatus);
		solStatus.setText(locationStatus);

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onCreate finished");
		String layout = getString(R.string.layout);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected layout = " + layout);
	}

	/**
	 * Called when activity is getting visible
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onResume started");
		super.onResume();

		if (myServiceIsStopped(MessageListener.class)) {
			// Start service to listen to UDP & MQTT broadcast messages
			startService(new Intent(this, MessageListener.class));
		}

		// Register the receiver for messages from UDP & GCM listener
		// Create an intent filter to listen to the broadcast sent with the action "BROADCAST_RECEIVED"
		/** Intent filter for app internal broadcast receiver */
		IntentFilter intentFilter = new IntentFilter(MessageListener.BROADCAST_RECEIVED);
		//Map the intent filter to the receiver
		registerReceiver(activityReceiver, intentFilter);

		// Register screen on/off broadcast receiver if no longer registered
		if (!EventReceiver.eventReceiverRegistered) {
			/** IntentFilter to receive screen on/off & connectivity broadcast msgs */
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
			/** Receiver for screen on/off broadcast msgs */
			BroadcastReceiver mReceiver = new EventReceiver();
			registerReceiver(mReceiver, filter);
		}

		// Initialize last known devices
		for (String deviceName1 : deviceNames) {
			String deviceIP = mPrefs.getString(deviceName1, "NA");
			if (!deviceIP.equalsIgnoreCase("NA")) { // device saved?
				new Initialize().execute(deviceName1);
			}
		}

		// Tell MQTT listener that we just started and need last status
		MessageListener.uiStarted = true;

		Intent thisIntent = getIntent();
		Bundle thisBundle = thisIntent.getExtras();
		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

		if ((thisBundle != null) && (thisBundle.getInt("view", 9) != 9)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart with extra: " + thisBundle.getInt("view", 0));
			// Get the requested view of call
			visibleView = thisBundle.getInt("view", 0);
		} else {
			// Get the last view the user had selected
			visibleView = mPrefs.getInt(prefsLastView,0);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart with view: " + visibleView);
		}

		// Get the layouts of all possible views
		secView = (RelativeLayout) findViewById(R.id.view_security);
		solView = (RelativeLayout) findViewById(R.id.view_solar);
		airView = (RelativeLayout) findViewById(R.id.view_aircon);
		debugView = (LinearLayout) findViewById(R.id.view_devdebug);
		seccamView = (RelativeLayout) findViewById(R.id.view_seccam);
		lightsView = (RelativeLayout) findViewById(R.id.view_lights);

		// Setup views
		switch (visibleView) {
			case 0: // Security
				switchUI(view_security_id);
				break;
			case 1: // Solar panel
				switchUI(view_solar_id);
				break;
			case 2: // Aircon
				switchUI(view_aircon_id);
				break;
//			case 3: // Debug screen
//				switchUI(view_devDebug_id);
//				break;
			case 4: // Security camera view
				switchUI(view_security_id);
				break;
			case 5: // Lights control
				switchUI(view_lights_id);
				break;
			default:
				switchUI(view_solar_id);
				break;
		}

		// Setup aircon views
		switch (selDevice) {
			case 0:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				break;
			case 1:
				airFDView.setVisibility(View.INVISIBLE);
				airCAView.setVisibility(View.VISIBLE);
				break;
			case 2:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				break;
		}

		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		ImageButton ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_on);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));
		ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_off);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));
		ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_dim);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onResume finished");
	}

	/**
	 * Called when activity is getting invisible
	 */
	@Override
	protected void onPause() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onPause started");
		super.onPause();

		// Check if async tasks with database access are still running
		if(atNow != null && atNow.getStatus() == AsyncTask.Status.RUNNING)
			atNow.cancel(false);
		if(atLast != null && atLast.getStatus() == AsyncTask.Status.RUNNING)
			atLast.cancel(false);

		// Close databases
		dbHelperNow.close();
		dbHelperLast.close();

		// Unsubscribe from MQTT broker status messages
		unSubscribeBrokerStatus();

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onPause finished");
}

	/**
	 * Called when activity is getting destroyed
	 * Handles security fragment specific tasks
	 */
	@Override
	protected void onDestroy() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onDestroy started");
		super.onDestroy();
		// Unregister the receiver for messages from MQTT/TCP/UDP listener
		unregisterReceiver(activityReceiver);
		activityReceiver = null;
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onDestroy finished");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onCreateOptionsMenu started");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_my_home_control, menu);
		abMenu = menu;

		// Enable/Disable device debug view selection in menu
		// Check if we have the IP address 192.168.0.121
		WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		@SuppressWarnings("deprecation") String ipAddress = Formatter.formatIpAddress(ip);
		MenuItem menuShowDebugItem = abMenu.getItem(action_debug_id); // Switch to device debug view menu entry
		MenuItem menuDebugViewItem = abMenu.getItem(action_devDebug_id); // Switch to device debug view menu entry
		if (ipAddress.equalsIgnoreCase("192.168.0.121")) {
			menuShowDebugItem.setVisible(true);
			menuDebugViewItem.setVisible(true);
			if (showDebug) {
				menuShowDebugItem.setTitle(R.string.action_debug_off);
			} else {
				menuShowDebugItem.setTitle(R.string.action_debug);
			}

		} else {
			menuShowDebugItem.setVisible(false);
			menuDebugViewItem.setVisible(false);
		}

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sequence - onCreateOptionsMenu finished");
		return true;
	}

	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/** Menu item pointer */
		MenuItem menuItem;

		switch (item.getItemId()) {
			case R.id.action_close:
				finish();
				break;
			case R.id.action_selWarning:
			case R.id.action_selAlarm:
				isSelAlarm = item.getItemId() != R.id.action_selWarning;
				notifNames = new ArrayList<>();
				notifUri = new ArrayList<>();
				notifNames.add(getString(R.string.no_alarm_sel));
				notifUri.add("");
				notifNames.add(getString(R.string.snd_alarm));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alarm);
				notifNames.add(getString(R.string.snd_alert));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alert);
				notifNames.add(getString(R.string.snd_dog));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.dog);
				/** Index of last user selected alarm tone */
				int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri, isSelAlarm) + 2;

				// get sound_selector.xml view
				/** Layout inflater for sound selection dialog */
				LayoutInflater alarmDialogInflater = LayoutInflater.from(this);
				/** View for sound selection dialog */
				@SuppressLint("InflateParams")
				View alarmSettingsView = alarmDialogInflater.inflate(R.layout.sound_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder alarmDialogBuilder = new AlertDialog.Builder(this);

				// set sound_selector.xml to alert dialog builder
				alarmDialogBuilder.setView(alarmSettingsView);

				// set dialog message
				alarmDialogBuilder
						.setTitle(getResources().getString(R.string.action_selAlarm))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										if (!notifNameSel.equalsIgnoreCase("")) {
											if (isSelAlarm) {
												mPrefs.edit().putString(prefsSecurityAlarm, notifUriSel).apply();
											} else {
												mPrefs.edit().putString(prefsSolarWarning, notifUriSel).apply();
											}
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alarmDialog = alarmDialogBuilder.create();

				// show it
				alarmDialog.show();

				/** Pointer to list view with the alarms */
				ListView lvAlarmList = (ListView) alarmSettingsView.findViewById(R.id.lv_AlarmList);
				/** Array adapter for the ListView */
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						appContext,
						R.layout.my_list_item,
						notifNames );
				lvAlarmList.setAdapter(arrayAdapter);
				// Use long click listener to play the alarm sound
				lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					                               int pos, long id) {
						/** Instance of media player */
						MediaPlayer mMediaPlayer = new MediaPlayer();
						try {
							mMediaPlayer.setDataSource(appContext, Uri.parse(notifUri.get(pos)));
							/** Audio manager to play the sound */
							final AudioManager audioManager = (AudioManager) appContext
									.getSystemService(Context.AUDIO_SERVICE);
							if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
								mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
							}
						} catch (IOException e) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot play alarm");
						}
						return true;
					}
				});
				lvAlarmList.setOnItemClickListener(this);
				lvAlarmList.setItemChecked(uriIndex, true);
				lvAlarmList.setSelection(uriIndex);
				break;
			case R.id.action_security:
				// Show security UI
				switchUI(view_security_id);
				break;
			case R.id.action_solar:
				// Show solar panel UI
				switchUI(view_solar_id);
				break;
			case R.id.action_aircon:
				// Show aircon UI
				switchUI(view_aircon_id);
				break;
			case R.id.action_refresh:
				if (Utilities.isHomeWiFi(this)) {
					// Start discovery of mDNS/NSD services available if not running already
					if (myServiceIsStopped(CheckAvailDevices.class)) {
						startService(new Intent(this, CheckAvailDevices.class));
					}
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.scan_impossible), Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.action_debug:
				showDebug = !showDebug;
				mPrefs.edit().putBoolean(prefsShowDebug,showDebug).apply();
				menuItem = abMenu.getItem(action_debug_id); // Debug menu entry
				if (showDebug) {
					menuItem.setTitle(R.string.action_debug_off);
				} else {
					menuItem.setTitle(R.string.action_debug);
				}
				break;
			case R.id.action_locations:
				// get location_selector.xml view
				/** Layout inflater for device selection dialog */
				LayoutInflater locationDialogInflater = LayoutInflater.from(this);
				locationSettingsView = locationDialogInflater.inflate(R.layout.location_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder locationDialogBuilder = new AlertDialog.Builder(this);

				// set location_selector.xml to alert dialog builder
				locationDialogBuilder.setView(locationSettingsView);

				/** Pointer to button, used to set OnClickListener for buttons in the dialog */
				Button btToSetOnClickListener;
				/** Pointer to button text, used to give each button in the dialog a specific name */
				String buttonTxt;

				for (int i = aircon1Index; i < aircon1Index+3; i++) {
					if (deviceIsOn[i]) {
						btToSetOnClickListener = (Button) locationSettingsView.findViewById(buttonIds[i]);
						btToSetOnClickListener.setVisibility(View.VISIBLE);
						if (locationName[i-aircon1Index].equalsIgnoreCase("")) {
							btToSetOnClickListener.setText(deviceName[i-aircon1Index]);
						} else {
							buttonTxt = locationName[i-aircon1Index];
							btToSetOnClickListener.setText(buttonTxt);
						}
						btToSetOnClickListener.setOnClickListener(this);
					}
				}

				// set dialog message
				locationDialogBuilder
						.setTitle(getResources().getString(R.string.dialog_selector_title))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										for (int i = -aircon1Index; i < -aircon1Index+3; i++) {
											mPrefs.edit().putString(
													prefsLocationName + Integer.toString(i),
													locationName[i-aircon1Index]).apply();
											mPrefs.edit().putInt(
													prefsDeviceIcon + Integer.toString(i),
													deviceIcon[i-aircon1Index]).apply();
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alertDialog = locationDialogBuilder.create();

				// show it
				alertDialog.show();

				break;
			case R.id.action_devDebug:
				// Show security UI
				switchUI(view_devDebug_id);
				break;
//			case R.id.action_selMqtt:
//				// get mqtt_user_select.xml view
//				/** Layout inflater for mqtt user selection dialog */
//				LayoutInflater mqttDialogInflater = LayoutInflater.from(this);
//				/** View for mqtt user selection dialog */
//				final View mqttSettingsView = mqttDialogInflater.inflate(R.layout.mqtt_user_select, null);
//				/** Alert dialog builder for mqtt user selection dialog */
//				AlertDialog.Builder mqttDialogBuilder = new AlertDialog.Builder(this);
//
//				// set mqtt_user_select.xml to alert dialog builder
//				mqttDialogBuilder.setView(mqttSettingsView);
//
//				// set dialog message
//				mqttDialogBuilder
//						.setTitle(getResources().getString(R.string.action_mqttUser))
//						.setCancelable(false)
//						.setNegativeButton("OK",
//								new DialogInterface.OnClickListener() {
//									public void onClick(DialogInterface dialog, int id) {
//										RadioGroup rgMqttUserList = (RadioGroup) mqttSettingsView.findViewById(R.id.rb_mqtt_users);
//										int selectedMqttUser = rgMqttUserList.getCheckedRadioButtonId();
//										View radioButton = rgMqttUserList.findViewById(selectedMqttUser);
//										selectedMqttUser = rgMqttUserList.indexOfChild(radioButton);
//										mPrefs.edit().putInt("MQTT_User",selectedMqttUser).apply();
//										String mqttUserSelectEnd = getResources().getString(R.string.action_mqttUser_End);
//										Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
//												mqttUserSelectEnd,
//												Snackbar.LENGTH_INDEFINITE);
//										mySnackbar.setAction("OK", mOnClickListener);
//										mySnackbar.show();
//										dialog.cancel();
//									}
//								});
//
//				// create alert dialog
//				/** Alert dialog for mqtt user selection */
//				AlertDialog mqttUserDialog = mqttDialogBuilder.create();
//
//				// show it
//				mqttUserDialog.show();
//
//				/** Pointer to radio group view with the mqtt users */
//				RadioGroup rgMqttUserList = (RadioGroup) mqttSettingsView.findViewById(R.id.rb_mqtt_users);
//				int currMqttUser = mPrefs.getInt("MQTT_User",0);
//				rgMqttUserList.check(currMqttUser);
//				break;
			case R.id.action_lightControl:
				// Show light control view
				switchUI(view_lights_id);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Listener for clicks in ListView for alarm sound selection
	 *
	 * @param parent
	 *              AdapterView of alert dialog
	 * @param view
	 *              View of ListView
	 * @param position
	 *              Position in ListView that has been clicked
	 * @param id
	 *              ID of item in ListView that has been clicked
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		notifNameSel = notifNames.get(position);
		notifUriSel = notifUri.get(position);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v
	 * 		The view that was clicked.
	 */
	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {

		if (!handleSPMbuttons(v)) { // Check if it was a solar panel view button and handle it
			if (!handleSecurityButtons(v)) { // Check if it was a security view button and handle it
				if (!handleAirconButtons(v)) { // Check if it was a aircon view button and handle it
					if (!handleDebugButtons(v)) { // Check if it was a debug view button and handle it
						if (!handleLightButtons(v)) { // Check if it was a light control view button and handle it
							switch (v.getId()) { // Handle other buttons right here
								case R.id.dia_sel_device0:
								case R.id.dia_sel_device1:
								case R.id.dia_sel_device2:
									switch (v.getId()) {
										case R.id.dia_sel_device0:
											dlgDeviceIndex = FUJIDENZO;
											break;
										case R.id.dia_sel_device1:
											dlgDeviceIndex = CARRIER;
											break;
										default:
											dlgDeviceIndex = OTHER_AIRCON;
											break;
									}
									// get location_selector.xml view
									/** Layout inflater for dialog to change device name and icon */
									LayoutInflater airconDialogInflater = LayoutInflater.from(this);
									/** View of aircon device name and icon change dialog */
									airconDialogView = airconDialogInflater.inflate(R.layout.locations, null);
									/** Alert dialog builder for dialog to change device name and icon */
									AlertDialog.Builder airconDialogBuilder = new AlertDialog.Builder(this);
									// set location_selector.xml to alert dialog builder
									airconDialogBuilder.setView(airconDialogView);

									/** Button to set onClickListener for icon buttons in the dialog */
									ImageButton btOnlyClickListener;

									for (int i = 0; i < 8; i++) {
										btOnlyClickListener = (ImageButton) airconDialogView.findViewById(iconButtons[i]);
										btOnlyClickListener.setOnClickListener(this);
									}

									dlgIconIndex = deviceIcon[dlgDeviceIndex];
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);

									/** Edit text field for the user selected device name */
									final EditText userInput = (EditText) airconDialogView.findViewById(R.id.dia_et_location);
									userInput.setText(locationName[dlgDeviceIndex]);

									// set dialog message
									airconDialogBuilder
											.setTitle(getResources().getString(R.string.dialog_change_title))
											.setCancelable(false)
											.setPositiveButton("OK",
													new DialogInterface.OnClickListener() {
														@SuppressWarnings({"deprecation", "ConstantConditions"})
														public void onClick(DialogInterface dialog, int id) {
															locationName[dlgDeviceIndex] = userInput.getText().toString();
															deviceIcon[dlgDeviceIndex] = dlgIconIndex;
															// Update underlying dialog box with new device name
															/** Button of selection dialog that we are processing */
															Button btToChangeName = (Button) locationSettingsView.findViewById(buttonIds[dlgDeviceIndex]);
															btToChangeName.setText(locationName[dlgDeviceIndex]);
															locationSettingsView.invalidate();
															// Update UI
															/** Text view to show location name */
															TextView locationText;
															/** Image view to show location icon */
															ImageView locationIcon;
															if (dlgDeviceIndex == FUJIDENZO) {
																locationText = (TextView) findViewById(R.id.txt_device_fd);
																locationText.setText(locationName[dlgDeviceIndex]);
																locationIcon = (ImageView) findViewById(R.id.im_icon_fd);
																locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
															} else if (dlgDeviceIndex == CARRIER) {
																locationText = (TextView) findViewById(R.id.txt_device_ca);
																locationText.setText(locationName[dlgDeviceIndex]);
																locationIcon = (ImageView) findViewById(R.id.im_icon_ca);
																locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
															} else if (dlgDeviceIndex == OTHER_AIRCON) {
																// TODO add another aircon layout
																if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected another aircon");
															}
															// TODO add other aircon control layouts here
														}
													})
											.setNegativeButton("Cancel",
													new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int id) {
															dialog.cancel();
														}
													});

									// create alert dialog
									AlertDialog alertDialog = airconDialogBuilder.create();

									// show it
									alertDialog.show();
									break;
								case R.id.im_bath:
									dlgIconIndex = 0;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_bed:
									dlgIconIndex = 1;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_dining:
									dlgIconIndex = 2;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_entertain:
									dlgIconIndex = 3;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_kids:
									dlgIconIndex = 4;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_kitchen:
									dlgIconIndex = 5;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_living:
									dlgIconIndex = 6;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_office:
									dlgIconIndex = 7;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Called when an image has been loaded from the gallery server.
	 *
	 * @param bitmap
	 * 		The bitmap that has been downloaded.
	 */
	@Override
	public void onImageLoaded(Bitmap bitmap) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received image");
		ImageView mImageView = (ImageView) findViewById(R.id.iv_snapshot);
		mImageView.setImageBitmap(bitmap);
		if (snapShotAttacher == null) {
			snapShotAttacher = new PhotoViewAttacher(mImageView);
		} else {
			snapShotAttacher.update();
		}
		TextView imageName = (TextView) findViewById(R.id.tv_image_name);
		imageName.setVisibility(View.VISIBLE);
		imageName.setText(availImages.get(0));
		switchUI(view_seccam_id); // Switch to security camera view
	}

	/**
	 * Called when an error occurred while an image was loaded from the gallery server.
	 */
	@Override
	public void onError() {
		Toast.makeText(this, "Error Loading Image !", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when brightness value seekbar changed.
	 */
	@Override
	public void onProgressChanged(SeekBar v, int progress, boolean isUser) {
		String lightValue;
		lightsBedRoomVal = 222 - progress;
		if (lightsBedRoomVal == 140) {
			lightValue = getString(R.string.lights_val_on); // Bulbs are full on
		} else if (lightsBedRoomVal > 222) {
			lightValue = getString(R.string.lights_val_off); // Bulbs are full off
		} else {
			lightValue = getString(R.string.lights_val_dim); // Bulbs are dimmed on
		}
		bedRoomVal.setText(lightValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int progress = bedRoomValSB.getProgress();
		String lightValue;
		lightsBedRoomVal = 222 - progress;
		if (lightsBedRoomVal == 140) {
			lightValue = getString(R.string.lights_val_on); // Bulbs are full on
		} else if (lightsBedRoomVal > 222) {
			lightValue = getString(R.string.lights_val_off); // Bulbs are full off
		} else {
			lightValue = getString(R.string.lights_val_dim); // Bulbs are dimmed on
		}
		bedRoomVal.setText(lightValue);
		lightValue = "b="+lightValue;
		new MyHomeControl.ESPbyTCP(urlLB1,lightValue,"0");
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send new brightness to lights: "+lightValue);
	}

	/**
	 * Broadcast receiver for notifications received over UDP or MQTT or GCM
	 */
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/** Message received over UDP or GCM or from */
			String sender = intent.getStringExtra("from");
			String message;

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received broadcast from " + sender);

			if (sender.equalsIgnoreCase("NSD")) {
				boolean nsdScanResult = intent.getBooleanExtra("resolved",false);
				if (nsdScanResult) { // Scan finished successful
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Device scan finished");
					message = "Device scan successful finished";
					// Clean up and refresh preferences from found devices
					for (int i=0; i<deviceNames.length; i++) {
						boolean foundInList = false;
						int listIndex = 0;
						for (int j=0; j<NsdHelper.mServicesNames.length; j++) {
							if (deviceNames[i].equalsIgnoreCase(NsdHelper.mServicesNames[j])) {
								// found the device, break the loop
								foundInList = true;
								listIndex = j;
								break;
							}
						}
						if (!foundInList) {
							mPrefs.edit().remove(deviceNames[i]).apply();
							deviceIsOn[i] = false;
						} else {
							deviceIPs[i] = mServicesHosts[listIndex].toString().substring(1);
							deviceIsOn[i] = true;
							new Initialize().execute(deviceNames[i]);
						}
					}
					debugViewUpdate();
					String nsdResults = "Found devices & IP's:\n\n";
					for (int i=0; i<deviceNames.length; i++) {
						if (deviceIsOn[i]) {
							nsdResults += deviceIPs[i] + "\t\t" + deviceNames[i] + "\n";
						}
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							nsdResults,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(NsdHelper.mServicesNames.length);

				} else { // Scan error
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Device scan stopped with error");
					message = "Device scan stopped with error";
				}
				if (BuildConfig.DEBUG) {
					if (showDebug) {
						if (!snackBarText.isEmpty()) {
							snackBarText += "\n" + message;
						} else {
							snackBarText = message;
						}
						Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
								snackBarText,
								Snackbar.LENGTH_INDEFINITE);
						mySnackbar.setAction("OK", mOnClickListener);
						mySnackbar.show();
						View snackbarView = mySnackbar.getView();
						TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
						tv.setMaxLines(5);
					}
				}
				// Ask for status from light control unit
				new ESPbyTCP(urlLB1, "s", "0");
				return;
			} else {
				message = intent.getStringExtra("message");
			}

			// Is broadcast a debug message from the devices???
			if (sender.equalsIgnoreCase("DEBUG")) {
				String deviceName = "";
				if(message.contains(" ")){
					deviceName= message.substring(0, message.indexOf(" "));
				}
				if (!deviceName.isEmpty()) {
					Button buttonToChange = null;
					switch (deviceName) {
						case "moni":
							buttonToChange = (Button) findViewById(R.id.bt_debug_moni);
							break;
						case "sf1":
							buttonToChange = (Button) findViewById(R.id.bt_debug_secf);
							break;
						case "sb1":
							buttonToChange = (Button) findViewById(R.id.bt_debug_secb);
							break;
						case "fd1":
							buttonToChange = (Button) findViewById(R.id.bt_debug_ac1);
							break;
						case "ca1":
							buttonToChange = (Button) findViewById(R.id.bt_debug_ac2);
							break;
						case "cm1":
							buttonToChange = (Button) findViewById(R.id.bt_debug_cam1);
							break;
					}
					if (buttonToChange != null) {
						if (message.contains("TCP is on")) {
							buttonToChange.setBackgroundColor(colorGreen);
						}
						if (message.contains("TCP is off")) {
							buttonToChange.setBackgroundColor(colorOrange);
						}
					}
				} else {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received unknown broadcast: " + message);
				}
				TextView debugTxtView = (TextView) findViewById(R.id.tv_sv_debug);
				debugMsgs += message + "\n";

				if (highlightText == null || highlightText.isEmpty()) {
					debugTxtView.append(message + "\n");
				} else {
					if (message.toUpperCase().contains(highlightText.toUpperCase())) {
						debugTxtView.append(message + "\n");
					}
				}
				ScrollView debugTxtScroll = (ScrollView) findViewById(R.id.sv_debugview);
				debugTxtScroll.fullScroll(View.FOCUS_DOWN);
				return;
			}

			// Is broadcast a mqtt status message???
			if (sender.equalsIgnoreCase("STATUS")) {
				if (BuildConfig.DEBUG) {
					if (showDebug) {
						if (!snackBarText.isEmpty()) {
							snackBarText += "\n" + message;
						} else {
							snackBarText = message;
						}
						Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
								snackBarText,
								Snackbar.LENGTH_INDEFINITE);
						mySnackbar.setAction("OK", mOnClickListener);
						mySnackbar.show();
						View snackbarView = mySnackbar.getView();
						TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
						tv.setMaxLines(5);
					}
				}
				return;
			}

			// Is broadcast a mqtt broker status message???
			if (sender.equalsIgnoreCase("BROKER")) {
				debugViewUpdate();
				return;
			}

			/** Return values for onPostExecute */
			CommResultWrapper result = new CommResultWrapper();

			// Check if response is a JSON array
			if (Utilities.isJSONValid(message)) {
				result.comResult = message;
				JSONObject jsonResult;
				try {
					jsonResult = new JSONObject(message);
					String broadCastDevice = "";

					// Get device name
					if (jsonResult.has("de")) {
						broadCastDevice = jsonResult.getString("de");
					}

					Integer dotPos;

					// Check if device is already known
					switch (broadCastDevice) {
						case "spm":
							if (!deviceIsOn[spMonitorIndex]) {
								deviceIsOn[spMonitorIndex] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							result.comCmd = "/?s";
							solarViewUpdate(message, true);
							break;
						case "fd1":
							if (!deviceIsOn[aircon1Index]) {
								deviceIsOn[aircon1Index] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							result.comCmd = "/?s";
							result.deviceIndex = 0;
							airconViewUpdate(result);
							break;
						case "ca1":
							if (!deviceIsOn[aircon2Index]) {
								deviceIsOn[aircon2Index] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							result.comCmd = "/?s";
							result.deviceIndex = 1;
							airconViewUpdate(result);
							break;
						case "xy1":
							if (!deviceIsOn[aircon3Index]) {
								deviceIsOn[aircon3Index] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							break;
						case "sf1":
							if (!deviceIsOn[secFrontIndex]) {
								deviceIsOn[secFrontIndex] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "sb1":
							if (!deviceIsOn[secBackIndex]) {
								deviceIsOn[secBackIndex] = true;
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							TextView outsideWeatherTV = (TextView) findViewById(R.id.tv_weather_out);
							String outsideWeather = "Outside\n";
							String leadDigits;
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "%";
							}
							outsideWeatherTV.setText(outsideWeather);
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "wei":
							TextView insideWeatherTV = (TextView) findViewById(R.id.tv_weather_in);
							String insideWeather = "Inside\n";
//							String leadDigits;
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								insideWeather = insideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								insideWeather = insideWeather + leadDigits + "%";
							}
							insideWeatherTV.setText(insideWeather);
							break;
						case "weo":
							outsideWeatherTV = (TextView) findViewById(R.id.tv_weather_out);
							outsideWeather = "Outside\n";
//							String leadDigits;
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0,dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "%";
							}
							outsideWeatherTV.setText(outsideWeather);
							break;
						case "cm1":
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "lb1":
							String lightValue = getResources().getString(R.string.lights_val_unknown);
							if (jsonResult.has("br")) {
								lightsBedRoomVal = jsonResult.getInt("br");
								if (lightsBedRoomVal == 140) {
									lightValue = getString(R.string.lights_val_on); // Bulbs are full on
								} else if (lightsBedRoomVal > 222) {
									lightValue = getString(R.string.lights_val_off); // Bulbs are full off
								} else {
									lightValue = getString(R.string.lights_val_dim); // Bulbs are dimmed on
								}
							} else {
								lightsBedRoomVal = 0;
							}
							bedRoomVal.setText(lightValue);
							bedRoomValSB.setProgress(222-lightsBedRoomVal);

							if (jsonResult.has("di")) {
								int newDimLightLevel = jsonResult.getInt("di");
								if (newDimLightLevel != mPrefs.getInt(prefsLightBedDim,200)) {
									mPrefs.edit().putInt(prefsLightBedDim,newDimLightLevel).apply();
								}
							}
							ImageButton ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_on);
							//noinspection deprecation
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_on));
							ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_off);
							//noinspection deprecation
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_off));
							ibButtonToChange = (ImageButton) findViewById(R.id.ib_light_bed_dim);
							//noinspection deprecation
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_dim));
							break;
					}
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				}
			}
		}
	};

	/**
	 * Communication in Async Task between Android and Arduino Yun
	 */
	private class SPMPcommunication extends AsyncTask<String, String, CommResultWrapper> {

		/**
		 * Background process of communication
		 *
		 * @param params
		 * 		params[0] = URL
		 * 		params[1] = command to be sent to ESP or Arduino
		 * 		params[2] = result of communication
		 * 		params[3] = ID of requester
		 * 			spm = solar panel monitor view
		 * 			air = aircon control view
		 * 			sec = security control view
		 * 	@return <code>CommResultWrapper</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CommResultWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			CommResultWrapper result = new CommResultWrapper();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.comResult = params[2];
			result.deviceIndex = Integer.parseInt(params[4]);
			result.comFailed = false;

			Context thisAppContext = getApplicationContext();

			/** A HTTP client to access the YUN device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			if (!Utilities.isHomeWiFi(getApplicationContext())) {
				// For solar panel monitor get data from web site if we are not home
				result.httpURL = "www.spmonitor.giesecke.tk";
				result.comCmd = "/l.php";
			}
			/** URL to be called */
			String urlString = "http://" + result.httpURL + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callSPM = " + urlString);

			/** Request to ESP device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from SPM device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					} else {
						result.comFailed = true;
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_esp);
						if (result.comResult.contains("EHOSTUNREACH") || result.comResult.equalsIgnoreCase("")) {
							result.comResult = errorMsg;
							if (Utilities.isHomeWiFi(getApplicationContext())) {
								// Set spMonitor device as not available
								deviceIsOn[spMonitorIndex] = false;
								deviceIPs[spMonitorIndex] = "";
								result.comFailed = true;
							}
						}
						return result;
					} catch (NullPointerException en) {
						result.comResult = thisAppContext.getString(R.string.err_no_esp);
						if (Utilities.isHomeWiFi(getApplicationContext())) {
							// Set spMonitor device as not available
							deviceIsOn[spMonitorIndex] = false;
							deviceIPs[spMonitorIndex] = "";
							result.comFailed = true;
						}
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = thisAppContext.getString(R.string.err_esp);
				result.comFailed = true;
			}
			return result;
		}

		/**
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResultWrapper with requester ID and result of communication
		 */
		protected void onPostExecute(CommResultWrapper result) {
			if (BuildConfig.DEBUG && result.comFailed) {
				if (showDebug) {
					if (!snackBarText.isEmpty()) {
						snackBarText += "\n" + result.comResult;
					} else {
						snackBarText = result.comResult;
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							snackBarText,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(5);
				}
			}
			if (!dataBaseIsEmpty) {
				solarViewUpdate(result.comResult, false);
			}
		}
	}

	/**
	 * Send topic to MQTT broker in AsyncTask
	 */
	private class doPublishAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String payload = params[0];
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic publish: " + payload);
			if (MessageListener.mqttClient == null) { // If service is not (yet) active, don't publish
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic publish failed - not connected");
				return null;
			}
			IMqttToken token;
			try {
				byte[] encodedPayload;
				encodedPayload = payload.getBytes("UTF-8");
				MqttMessage message = new MqttMessage(encodedPayload);
				message.setQos(0);
				message.setRetained(true);
				token = MessageListener.mqttClient.publish("/CMD", message);
				token.waitForCompletion(5000);
			} catch (MqttSecurityException | UnsupportedEncodingException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT publish exception " +e.getMessage());
			} catch (MqttException e) {
				switch (e.getReasonCode()) {
					case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " +e.getMessage());
						break;
					case MqttException.REASON_CODE_CLIENT_TIMEOUT:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " +e.getMessage());
						break;
					case MqttException.REASON_CODE_CONNECTION_LOST:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " +e.getMessage());
						break;
					case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " +e.getMessage());
						break;
					case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "FAILED_AUTHENTICATION "+ e.getMessage());
						break;
					default:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT unknown error " + e.getMessage());
						break;
				}
			}
			return null;
		}
	}

	/**
	 * Communication in Async Task between Android and ESP8266 over TCP
	 */
	private class ESPbyTCPAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String targetAddress = params[0];
			String targetMessage = params[1];

			if (targetAddress.equalsIgnoreCase("")) { // target address is empty, don't try to connect!
				return null;
			}
			try {
				InetAddress tcpServer = InetAddress.getByName(targetAddress);
				Socket tcpSocket = new Socket(tcpServer, 6000);

				tcpSocket.setSoTimeout(10000);
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sending " + targetMessage
						+ " to " + targetAddress);
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(tcpSocket.getOutputStream())), true);
				out.println(targetMessage);
				tcpSocket.close();
			} catch (Exception e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
						+ " " + targetAddress);
			}
			return null;
		}
	}

	/**
	 * Communication in Async Task between Android and ESP8266 over TCP
	 */
	class ESPbyTCP implements Runnable {

		final String targetAddress;
		final String targetMessage;
		final String targetDevice;

		ESPbyTCP(String ipAddress, String tcpMessage, String deviceID) {
			this.targetAddress = ipAddress;
			this.targetMessage = tcpMessage;
			this.targetDevice = deviceID;
			run();
		}

		public void run() {
			// If we are not on home WiFi, send command to MQTT broker
			if (!Utilities.isHomeWiFi(getApplicationContext()) && !targetDevice.equalsIgnoreCase("chk")) {
				String mqttTopic = "{\"ip\":\"" + targetDevice + "\","; // Device IP address
//				mqttTopic += "\"cm\":\"" + targetMessage.substring(1) + "\"}"; // The command
				mqttTopic += "\"cm\":\"" + targetMessage + "\"}"; // The command

				new doPublishAsync().execute(mqttTopic);
			} else {
				new ESPbyTCPAsync().execute(targetAddress, targetMessage);
			}
		}
	}

	/**
	 * Communication in Async Task between Android and Picture Gallery Server
	 */
	private class galleryCommunication extends AsyncTask<String, String, CommResultWrapper> {

		/**
		 * Background process of communication
		 *
		 * @param params
		 * 		params[0] = URL
		 * 		params[1] = command to be sent to ESP or Arduino
		 * 		params[2] = result of communication
		 * 		params[3] = ID of requester
		 * 			spm = solar panel monitor view
		 * 			air = aircon control view
		 * 			sec = security control view
		 * 	@return <code>CommResultWrapper</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CommResultWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			CommResultWrapper result = new CommResultWrapper();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.deviceIndex = Integer.parseInt(params[2]);
			result.comFailed = false;

			Context thisAppContext = getApplicationContext();

			/** A HTTP client to access the gallery server */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			/** URL to be called */
			String urlString = "http://" + result.httpURL + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callGallery = " + urlString);

			/** Request to ESP device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from gallery server */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					} else {
						result.comFailed = true;
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_gallery);
						if (result.comResult.contains("EHOSTUNREACH") || result.comResult.equalsIgnoreCase("")) {
							result.comResult = errorMsg;
						}
						result.comFailed = true;
						return result;
					} catch (NullPointerException en) {
						result.comResult = thisAppContext.getString(R.string.err_gallery);
						result.comFailed = true;
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = thisAppContext.getString(R.string.err_gallery);
				result.comFailed = true;
			}
			return result;
		}

		/**
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResultWrapper with requester ID and result of communication
		 */
		protected void onPostExecute(CommResultWrapper result) {
			if (BuildConfig.DEBUG && result.comFailed) {
				if (showDebug) {
					if (!snackBarText.isEmpty()) {
						snackBarText += "\n" + result.comResult;
					} else {
						snackBarText = result.comResult;
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							snackBarText,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(5);
				}
			}
			availImages = Arrays.asList(result.comResult.split("\\s*,\\s*"));
//			if (result.deviceIndex == 0) { // Load latest image and display
				if (!availImages.isEmpty()) {
					String lastImage = "http://giesecke.tk/1s/" + availImages.get(0) + ".jpg";
					new LoadImageTask((LoadImageTask.Listener)appContext).execute(lastImage);
				}
				// TODO save list of available images
				// TODO ask user which image to show
				if (showDebug) {
					if (!snackBarText.isEmpty()) {
						snackBarText += "\n" + availImages.get(0);
					} else {
						snackBarText = availImages.get(0);
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							snackBarText,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(5);
				}
//			} else { // Present a list of available images and let user select which to show
//				// TODO show downloaded image
//			}
			if (!dataBaseIsEmpty) {
				solarViewUpdate(result.comResult, false);
			}
		}
	}

	/**
	 * Update UI with values received from ESP device
	 *
	 * @param result
	 * 		result sent by onPostExecute
	 */
	private void securityViewUpdate(final CommResultWrapper result) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				/** String used for temporary conversions */
				String tempString;
				if (Utilities.isJSONValid(result.comResult)) {
					/** JSON object to hold the result received from the ESP8266 */
					JSONObject jsonResult;
					try {
						jsonResult = new JSONObject(result.comResult);
						/** String to hold complete status in viewable form */
						String message;
						/** Device ID */
						String deviceIDString;
						try {
							deviceIDString = jsonResult.getString("de");
						} catch (JSONException e) {
							deviceIDString = "unknown";
						}

						// Get device status and light status and add it to viewable status
						if (deviceIDString.equalsIgnoreCase("sf1")) {
							message = Utilities.getDeviceStatus(jsonResult, appContext,
									ivAlarmStatus, ivLightStatus,
									secBackView, secAutoAlarmFront, secChangeAlarm);
							message += Utilities.getLightStatus(jsonResult);
						} else if (deviceIDString.equalsIgnoreCase("sb1")) {
							message = Utilities.getDeviceStatus(jsonResult, appContext,
									ivAlarmStatusBack, ivLightStatusBack,
									secBackView, secAutoAlarmBack, secChangeAlarm);
							message += Utilities.getLightStatus(jsonResult);
						} else {
							message = "Camera snapshot ";
							try {
								int snapShotResult = jsonResult.getInt("pi");
								if (snapShotResult == 1) {
									message += "successful\n";
									new galleryCommunication().execute("giesecke.tk/gallery", "/get.php","0");
								} else {
									message += "failed\n";
								}
							} catch (JSONException ignore) {
							}
						}
						try {
							tempString = jsonResult.getString("ssid");
							message += "SSID: " + tempString + "\n";
						} catch (JSONException ignore) {
						}
						try {
							tempString = jsonResult.getString("ip");
							message += "IP: " + tempString + "\n";
						} catch (JSONException ignore) {
						}
						try {
							tempString = jsonResult.getString("mac");
							message += "MAC: " + tempString + "\n";
						} catch (JSONException ignore) {
						}
						try {
							tempString = jsonResult.getString("sketch");
							message += "Sketch size: " + tempString + "\n";
						} catch (JSONException ignore) {
						}
						try {
							tempString = jsonResult.getString("freemem");
							message += "Free Memory: " + tempString + "\n";
						} catch (JSONException ignore) {
						}

						if (showDebug) {
							secStatus.setText(message);
						} else {
							secStatus.setText("");
						}
					} catch (JSONException e) {
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Parse JSON and show received status in UI
	 *
	 * @param result
	 *            CommResultWrapper
	 *               isSearchDevice = flag that device search is active
	 *               deviceIndex = index of device that is investigated
	 *               reqCmd = command to be sent to the ESP device
	 *               comResult = return string as JSON from the ESP device
	 */
	private void airconViewUpdate(CommResultWrapper result) {

		Context thisAppContext = getApplicationContext();
		try {
			/** JSON object with the result from the ESP device */
			JSONObject deviceResult = new JSONObject(result.comResult);
			if (deviceResult.has("de")) {
				deviceName[result.deviceIndex] = deviceResult.getString("de");
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fd")) {
					deviceType[result.deviceIndex] = FUJIDENZO;
				}
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fb")) {
					deviceType[result.deviceIndex] = FUJIDENZO;
				}
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("ca")) {
					deviceType[result.deviceIndex] = CARRIER;
				}
				// TODO here is the place to add more layout versions for air cons
			}
			if (deviceResult.has("po")) {
				powerStatus[result.deviceIndex] = deviceResult.getInt("po");
			}
			if (deviceResult.has("mo")) {
				modeStatus[result.deviceIndex] = deviceResult.getInt("mo");
			}
			if (deviceResult.has("sp")) {
				fanStatus[result.deviceIndex] = deviceResult.getInt("sp");
			}
			if (deviceResult.has("te")) {
				coolStatus[result.deviceIndex] = deviceResult.getInt("te");
			}
			if (deviceResult.has("co")) {
				consStatus = deviceResult.getDouble("co");
			}
			if (deviceResult.has("st")) {
				autoStatus = deviceResult.getInt("st");
			}
			if (deviceResult.has("au")) {
				autoOnStatus[result.deviceIndex] = deviceResult.getInt("au");
			}
			if (deviceResult.has("sw")) {
				sweepStatus[result.deviceIndex] = deviceResult.getInt("sw");
			}
			if (deviceResult.has("tu")) {
				turboStatus[result.deviceIndex] = deviceResult.getInt("tu");
			}
			if (deviceResult.has("io")) {
				ionStatus[result.deviceIndex] = deviceResult.getInt("io");
			}
			if (deviceResult.has("ti")) {
				timerStatus[result.deviceIndex] = deviceResult.getInt("ti");
			}
			if (deviceResult.has("ot")) {
				deviceTimer[result.deviceIndex] = deviceResult.getInt("ot");
			}
			if (deviceResult.has("ts")) {
				deviceOffTime[result.deviceIndex] = deviceResult.getString("ts");
			}

			// TODO here is the place to add more status for other air cons

			// Update UI
			updateAirStatus(result.deviceIndex, thisAppContext);
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received invalid JSON = " + result.comResult);
		}
		if (showDebug) {
			airStatus.setText(result.comResult);
		} else {
			airStatus.setText("");
		}
	}

	/**
	 * Update UI fields with the latest status of a device
	 *
	 * @param deviceIndex
	 *            Index of the device to be updated
	 */
	private void updateAirStatus(int deviceIndex, Context thisAppContext) {
		/** String for the average consumption value */
		@SuppressLint("DefaultLocale") String consText = String.format("%.0f", consStatus) + "W";
		/** String for the temperature setting value */
		String tempText = Integer.toString(coolStatus[deviceIndex]) + "C";
		/** String for the auto on/off status */
		String statusText = Integer.toString(autoStatus);
		/** String with timer duration */
		String timerTime;

		switch (deviceType[deviceIndex]) {
			case FUJIDENZO:
				btOnOffLightFD.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorRed);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btCoolLightFD.setBackgroundColor(colorRed);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Fan medium mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorRed);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Fan high mode
						btFanHighLightFD.setBackgroundColor(colorRed);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
				}
				if (timerStatus[deviceIndex] == 0) {
					btTimerFD.setBackgroundColor(colorGreen);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							thisAppContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerFD.setBackgroundColor(colorOrange);
//					timerTime = thisAppContext.getResources().getString(R.string.timer_on);
					timerTime = deviceOffTime[selDevice];
				}
				btTimerFD.setText(timerTime);

				txtConsValFD.setText(consText);
				txtTempValFD.setText(tempText);
				txtAutoStatusValFD.setText(statusText);
				btAutoLightFD.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				break;
			case CARRIER:
				btOnOffLightCA.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorRed);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorRed);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 3: // Auto mode
						btAutomLightCA.setBackgroundColor(colorRed);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanCA.setText(R.string.bt_txt_fan_low);
						break;
					case 1: // Fan medium mode
						btFanCA.setText(R.string.bt_txt_fan_med);
						break;
					case 2: // Fan high mode
						btFanCA.setText(R.string.bt_txt_fan_high);
						break;
				}
				if (timerStatus[deviceIndex] == 0) {
					btTimerCA.setBackgroundColor(colorGreen);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							thisAppContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerCA.setBackgroundColor(colorOrange);
//					timerTime = thisAppContext.getResources().getString(R.string.timer_on);
					timerTime = deviceOffTime[selDevice];
				}
				btTimerCA.setText(timerTime);
				btSweepLightCA.setBackgroundColor(
						(sweepStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btTurboLightCA.setBackgroundColor(
						(turboStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btIonLightCA.setBackgroundColor(
						(ionStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				txtConsValCA.setText(consText);
				txtTempValCA.setText(tempText);
				txtAutoStatusValCA.setText(statusText);
				btAutoLightCA.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				break;
			// TODO here is the place to add more layouts for other air cons
		}
	}

	/**
	 * Async task class to contact Linino part of the spMonitor device
	 * and sync spMonitor database with local Android database
	 */
	private class syncSolarDB extends AsyncTask<String, String, SolarCommResultWrapper> {

		@Override
		protected SolarCommResultWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			SolarCommResultWrapper result = new SolarCommResultWrapper();

			/** A HTTP client to access the ESP device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			/** Which month to sync */
			result.syncMonth = params[0];

			/** Response from the spMonitor device or error message */
			result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);

			/** URL to be called */
			String urlString = "http://" + deviceIPs[spMonitorIndex] + "/sd/spMonitor/query2.php"; // URL to call

			// Check for last entry in the local database
			/** Instance of data base */
			SQLiteDatabase dataBase;
			if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
				dataBase = dbHelperNow.getReadableDatabase();
			} else {
				dataBase = dbHelperLast.getReadableDatabase();
			}
			// Is database in use?
			while (dataBase.inTransaction()) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "syncSolarDB Database is in use");
			}

			dataBase.beginTransaction();
			/** Cursor with data from database */
			Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
			/** Flag for database access type */
			boolean splitAccess = false;
			if (dbCursor != null) {
				if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
					dbCursor.moveToFirst();

					int lastMinute =  dbCursor.getInt(4);
					int lastHour = dbCursor.getInt(3);
					int lastDay = dbCursor.getInt(2);

					urlString += "?date=" + dbCursor.getString(0); // add year
					urlString += "-" + ("00" +
							dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
					urlString += "-" + ("00" +
							String.valueOf(lastDay))
							.substring(String.valueOf(lastDay).length()); // add day
					urlString += "-" + ("00" +
							String.valueOf(lastHour))
							.substring(String.valueOf(lastHour).length()); // add hour
					urlString += ":" + ("00" +
							String.valueOf(lastMinute))
							.substring(String.valueOf(lastMinute).length()); // add minute
					urlString += "&get=all";
				} else { // local database is empty, need to sync all data
					splitAccess = true;
					urlString += "?date=" + result.syncMonth;
				}
			} else { // something went wrong with the database access
				result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
				try {
					dataBase.endTransaction();
					dataBase.close();
				} catch (IllegalStateException ignore) {
				}
				try {
					dataBase.close();
				} catch (IllegalStateException ignore) {
				}
				return result;
			}
			dbCursor.close();
			dataBase.endTransaction();
			dataBase.close();

			/** Repeat counter used when full database needs to be synced */
			int loopCnt = 0;
			/** URL used for access */
			String thisURL = urlString;
			if (splitAccess) {
				loopCnt = 3;
			}

			for (int loop = 0; loop <= loopCnt; loop++) {
				if (splitAccess) {
					urlString = thisURL + "-" + String.valueOf(loop);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"URL = " + urlString);
				}
				/** Request to spMonitor device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from spMonitor device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							result.taskResult = response.body().string();
						}
					} catch (IOException e) {
						result.taskResult = e.getMessage();
						try {
							if (result.taskResult.contains("EHOSTUNREACH")) {
								result.taskResult = getApplicationContext().getString(R.string.err_arduino);
							}
							if (result.taskResult.equalsIgnoreCase("")) {
								result.taskResult = getApplicationContext().getString(R.string.err_arduino);
							}
							return result;
						} catch (NullPointerException en) {
							result.taskResult = getResources().getString(R.string.err_no_device);
							return result;
						}
					}

					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"JSON size = " + result.taskResult.length());

					try {
						/** JSON array with the data received from spMonitor device */
						JSONArray jsonFromDevice = new JSONArray(result.taskResult);
						if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
							dataBase = dbHelperNow.getWritableDatabase();
						} else {
							dataBase = dbHelperLast.getWritableDatabase();
						}
						// Get received data into local database
						/** Data string for insert into database */
						String record = "";
						try {
							dataBase.beginTransaction();
							for (int i=0; i<jsonFromDevice.length(); i++) {
								// skip first data record from device if we are just updating the database
								if (i == 0 && !splitAccess) i++;
								/** JSONObject with a single record */
								JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
								record = jsonRecord.getString("d");
								record = record.replace("-",",");
								record += ","+jsonRecord.getString("l");
								record += ","+jsonRecord.getString("s");
								record += ","+jsonRecord.getString("c");
								if (BuildConfig.DEBUG && i <= 1) Log.d(DEBUG_LOG_TAG,"DB insert: " + record);
								DataBaseHelper.addDay(dataBase, record);
							}
							dataBase.setTransactionSuccessful();
							dataBase.endTransaction();
							dataBase.close();
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"DB insert: " + record);
							result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSynced);
						} catch (SQLiteDatabaseLockedException e) {
							result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail1);
							dataBase.endTransaction();
							dataBase.close();
						}
					} catch (JSONException e) {
						result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
						dataBase.endTransaction();
						dataBase.close();
					}
				}
			}
			dataBaseIsEmpty = false;
			return result;
		}

		protected void onPostExecute(SolarCommResultWrapper result) {
			updateSynced(result.taskResult, result.syncMonth);
			if (needLastMonth) {
				atLast = new syncSolarDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dbNamesList[1]);
				needLastMonth = false;
			}
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void solarViewUpdate(final String value, final boolean isBroadCast) {
		runOnUiThread(new Runnable() {
			@SuppressLint("DefaultLocale")
			@SuppressWarnings({"deprecation", "ConstantConditions"})
			@Override
			public void run() {
				/** Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					// decode JSON
					if (Utilities.isJSONValid(value)) {
						/** Flag for data from external server */
						boolean isFromLocal;
						/** JSON object containing the values */
						JSONObject jsonValues = null;
						try {
							jsonValues = new JSONObject(value.substring(1,value.length()-1));
							isFromLocal = false;
						} catch (JSONException ignore) {
							isFromLocal = true;
						}
						if (isBroadCast) {
							isFromLocal = false;
							try {
								jsonValues = new JSONObject(value);
							} catch (JSONException ignore) {
								return;
							}
						}
						try {
							if (isFromLocal) {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								if (jsonResult.has("value")) {
									/** JSON object containing the values */
									jsonValues = jsonResult.getJSONObject("value");
								} else {
									return;
								}
							}

							try {
								ChartHelper.solarPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("S")):
										Float.parseFloat(jsonValues.getString("s"));
								ChartHelper.lastSolarPowerMin = ChartHelper.solarPowerMin;
							} catch (Exception excError) {
								ChartHelper.solarPowerMin = ChartHelper.lastSolarPowerMin;
							}
							try {
								ChartHelper.consPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("C")):
										Float.parseFloat(jsonValues.getString("c"));
								ChartHelper.lastConsPowerMin = ChartHelper.consPowerMin;
							} catch (Exception excError) {
								ChartHelper.consPowerMin = ChartHelper.lastConsPowerMin;
							}

							result = "S=" + String.valueOf(ChartHelper.solarPowerMin) + "W ";
							result += "s=";
							try {
								result += jsonValues.getString("s");
							} catch (Exception ignore) {
								result += "---";
							}
							if (jsonValues.has("sv")) {
								result += "A sv=";
								try {
									result += jsonValues.getString("sv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sr")) {
								result += "V sr=";
								try {
									result += jsonValues.getString("sr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sa")) {
								result += "W sa=";
								try {
									result += jsonValues.getString("sa");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sp")) {
								result += "W sp=";
								try {
									result += jsonValues.getString("sp");
								} catch (Exception excError) {
									result += "---";
								}
							}
							result += "\nC=" + String.valueOf(ChartHelper.consPowerMin) + "W c=";
							try {
								result += jsonValues.getString("c");
							} catch (Exception excError) {
								result += "---";
							}
							if (jsonValues.has("cv")) {
								result += "A cv=";
								try {
									result += jsonValues.getString("cv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cr")) {
								result += "V cr=";
								try {
									result += jsonValues.getString("cr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("ca")) {
								result += "W ca=";
								try {
									result += jsonValues.getString("ca");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cp")) {
								result += "W cp=";
								try {
									result += jsonValues.getString("cp");
								} catch (Exception excError) {
									result += "---";
								}
								result += " ";
							} else {
								result += " ";
							}

							/** Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = ChartHelper.solarPowerMin + ChartHelper.consPowerMin;

							valueFields = (TextView) findViewById(R.id.tv_solar_val);
							/** String for display */
							String displayTxt;
							displayTxt = String.format("%.0f", ChartHelper.solarPowerMin) + "W";
							valueFields.setText(displayTxt);
							valueFields = (TextView) findViewById(R.id.tv_cons_val);
							displayTxt = String.format("%.0f", resultPowerMin) + "W";
							valueFields.setText(displayTxt);
							solStatus.setText(result);

							valueFields = (TextView) findViewById(R.id.tv_result_txt);
							if (ChartHelper.consPowerMin > 0.0d) {
								valueFields.setText(getString(R.string.tv_result_txt_im));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_red_light));
							} else {
								valueFields.setText(getString(R.string.tv_result_txt_ex));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_green_light));
							}
							displayTxt = String.format("%.0f", Math.abs(ChartHelper.consPowerMin)) + "W";
							valueFields.setText(displayTxt);

							if (ChartHelper.autoRefreshOn) {
								if (ChartHelper.plotData != null) {
									/** Current time as string */
									String nowTime = Utilities.getCurrentTime();
									ChartHelper.plotData.addXValue(nowTime);
									ChartHelper.timeStampsCont.add(nowTime);
									ChartHelper.solarSeries.add
											(new Entry(ChartHelper.solarPowerMin, ChartHelper.solarSeries.size()));
									ChartHelper.solarPowerCont.add(ChartHelper.solarPowerMin);
									if (ChartHelper.consPowerMin < 0.0) {
										ChartHelper.consPSeries.add
												(new Entry(ChartHelper.consPowerMin, ChartHelper.consPSeries.size()));
										ChartHelper.consumPPowerCont.add(ChartHelper.consPowerMin);
										ChartHelper.consMSeries.add(new Entry(0, ChartHelper.consMSeries.size()));
										ChartHelper.consumMPowerCont.add(0.0f);
									} else {
										ChartHelper.consMSeries.add
												(new Entry(ChartHelper.consPowerMin, ChartHelper.consMSeries.size()));
										ChartHelper.consumMPowerCont.add(ChartHelper.consPowerMin);
										ChartHelper.consPSeries.add(new Entry(0, ChartHelper.consPSeries.size()));
										ChartHelper.consumPPowerCont.add(0.0f);
									}
									/** Text view to show min and max poser values */
									TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(ChartHelper.consumMPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);
									maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(ChartHelper.solarPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);

									// let the chart know it's data has changed
									ChartHelper.lineChart.notifyDataSetChanged();
									ChartHelper.lineChart.invalidate();
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
							solStatus.setText(e.getMessage());
							return;
						}
						result += Utilities.getCurrentTime();
						solStatus.setText(result);
					}
				} else {
					solStatus.setText(value);
				}
			}
		});
	}

	/**
	 * Update Debug UI with status of MQTT broker
	 */
	private void debugViewUpdate() {
		Locale locale = Locale.getDefault();
		TextView viewToChange = (TextView) findViewById(R.id.tv_mqtt_bytes_avg);
		String tvText = NumberFormat.getNumberInstance(locale).format(bytesLoadRcvd)
				+ " / " + NumberFormat.getNumberInstance(locale).format(bytesLoadSend);
		viewToChange.setText(tvText);
		viewToChange = (TextView) findViewById(R.id.tv_mqtt_msg_avg);
		tvText = NumberFormat.getNumberInstance(locale).format(bytesMsgsRcvd)
				+ " / " + NumberFormat.getNumberInstance(locale).format(bytesMsgsSend);
		viewToChange.setText(tvText);

		viewToChange = (TextView) findViewById(R.id.tv_mqtt_client_conn);
		viewToChange.setText(NumberFormat.getNumberInstance(locale).format(clientsConn));

		String statusClients1 = "";
		String statusClients2 = "";
		int mqttClientsNum = mqttClients.size();
		if (mqttClientsNum != 0) {
			for (int i=0; i<mqttClientsNum; i++) {
				if (i%2 == 0) { // Get two clients into one line
					statusClients1 += mqttClients.get(i) + "\n";
				} else {
					statusClients2 += mqttClients.get(i) + "\n";
				}
			}
		}
		viewToChange = (TextView) findViewById(R.id.tv_mqtt_client_list1);
		viewToChange.setText(statusClients1);
		viewToChange = (TextView) findViewById(R.id.tv_mqtt_client_list2);
		viewToChange.setText(statusClients2);

		String devNamesFound = "";
		String devIPsFound = "";
		for (int i=0; i<deviceNames.length; i++) {
			if (deviceIsOn[i]) {
				devNamesFound = devNamesFound + deviceNames[i] + "\n";
				devIPsFound = devIPsFound + deviceIPs[i] + "\n";
			}
		}
		viewToChange = (TextView) findViewById(R.id.tv_device_name_list);
		viewToChange.setText(devNamesFound);
		viewToChange = (TextView) findViewById(R.id.tv_device_ip_list);
		viewToChange.setText(devIPsFound);
	}

	/**
	 * Subscribe/Unsubscribe from MQTT broker status topics in AsyncTask
	 */
	private class mqttDebugAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String task = params[0];
			if (task.equalsIgnoreCase("subscribe")) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to MQTT status");
				MessageListener.subscribeBrokerStatus();
			} else {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Unsubscribe from MQTT status");
				unSubscribeBrokerStatus();
			}
			return null;
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Linino part)
	 *
	 * @param result
	 *        result sent by spMonitor
	 * @param syncMonth
	 *        Month that got synced
	 */
	private void updateSynced(final String result, final String syncMonth) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				solStatus.setText(result);

				/** The application context */
				Context thisAppContext = getApplicationContext();
				if (!showingLog) {
					/** Today split into 3 integers for the database query */
					int[] todayDate = Utilities.getCurrentDate();
					/** Array with existing log dates on the Arduino */
					List<String> thisLogDates;

					/** Instance of data base */
					SQLiteDatabase dataBase;
					try {
						if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
							dataBase = dbHelperNow.getReadableDatabase();
							thisLogDates = logDates;
						} else {
							dataBase = dbHelperLast.getReadableDatabase();
							thisLogDates = lastLogDates;
						}

						dataBase.beginTransaction();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, todayDate[2],
								todayDate[1], todayDate[0] - 2000);
						ChartHelper.fillSeries(newDataSet, appView);
						if (newDataSet != null) {
							newDataSet.close();
						}
						thisLogDates.clear();
						/** List with years in the database */
						ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
						for (int year = 0; year < yearsAvail.size(); year++) {
							/** List with months of year in the database */
							ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
									0, yearsAvail.get(year));
							for (int month = 0; month < monthsAvail.size(); month++) {
								/** List with days of month of year in the database */
								ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
										monthsAvail.get(month),
										yearsAvail.get(year));
								for (int day = 0; day < daysAvail.size(); day++) {
									thisLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
											.substring(String.valueOf(yearsAvail.get(year)).length()) +
											"-" + ("00" + String.valueOf(monthsAvail.get(month)))
											.substring(String.valueOf(monthsAvail.get(month)).length()) +
											"-" + ("00" + String.valueOf(daysAvail.get(day)))
											.substring(String.valueOf(daysAvail.get(day)).length()));
								}
							}
						}

						dataBase.endTransaction();
						dataBase.close();
						if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
							logDatesIndex = thisLogDates.size() - 1;
							ChartHelper.initChart(true, thisAppContext, chartTitle);
						} else {
							lastLogDatesIndex = thisLogDates.size() - 1;
						}
					} catch (SQLiteDatabaseLockedException ignore) {
					}
				}
				// Get latest value and update UI
				new SPMPcommunication().execute(deviceIPs[spMonitorIndex], "/data/get", "", "spm", Integer.toString(selDevice));
			}
		});
	}

	/**
	 * Check if service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private boolean myServiceIsStopped(Class<?> serviceClass) {
		/** Activity manager for services */
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Set all global variables used
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private void setGlobalVar() {
		// For security view:
		secStatus = (TextView) findViewById(R.id.security_status);
		ivAlarmStatus = (ImageView) findViewById(R.id.dot_alarm_status);
		ivLightStatus = (ImageView) findViewById(R.id.dot_light);
		secAutoAlarmFront = (CheckBox) findViewById(R.id.cb_sec_auto_alarm);
		secAutoAlarmBack = (CheckBox) findViewById(R.id.cb_sec_auto_alarm_2);
		secChangeAlarm = (TextView) findViewById(R.id.tv_change_alarm);
		secBackView = (TableLayout) findViewById(R.id.tl_alarm_back);
		ivAlarmStatusBack = (ImageView) findViewById(R.id.dot_alarm_status_back);
		ivLightStatusBack = (ImageView) findViewById(R.id.dot_light_back);

		// For solar view:
		solStatus = (TextView) findViewById(R.id.solar_status);
		appView = getWindow().getDecorView().findViewById(android.R.id.content);
		ChartHelper.lineChart = (LineChart) findViewById(R.id.graph);
		chartTitle = (TextView) findViewById(R.id.tv_plotTitle);

		// For aircon view:
		airFDView = (RelativeLayout) findViewById(R.id.fuji_denzo);
		airCAView = (RelativeLayout) findViewById(R.id.carrier);

		airStatus = (TextView) findViewById(R.id.aircon_status);
		btAutoLightFD = findViewById(R.id.bt_auto_hl_fd);
		btAutoLightCA = findViewById(R.id.bt_auto_hl_ca);
		btOnOffLightFD = findViewById(R.id.bt_on_off_hl_fd);
		btOnOffLightCA = findViewById(R.id.bt_on_off_hl_ca);
		btFanHighLightFD = findViewById(R.id.bt_fan_high_hl_fd);
		btFanMedLightFD = findViewById(R.id.bt_fan_med_hl_fd);
		btFanLowLightFD = findViewById(R.id.bt_fan_low_hl_fd);
		btCoolLightFD = findViewById(R.id.bt_cool_hl_fd);
		btCoolLightCA = findViewById(R.id.bt_cool_hl_ca);
		btDryLightFD = findViewById(R.id.bt_dry_hl_fd);
		btDryLightCA = findViewById(R.id.bt_dry_hl_ca);
		btFanLightFD = findViewById(R.id.bt_fan_hl_fd);
		btFanLightCA = findViewById(R.id.bt_fan_hl_ca);
		btSweepLightCA = findViewById(R.id.bt_sweep_hl_ca);
		btTurboLightCA = findViewById(R.id.bt_turbo_hl_ca);
		btIonLightCA = findViewById(R.id.bt_ion_hl_ca);
		btAutomLightCA = findViewById(R.id.bt_autom_hl_ca);

		btTimerFD = (Button) findViewById(R.id.bt_timer_fd);
		btTimerCA = (Button) findViewById(R.id.bt_timer_ca);

		btFanCA = (Button) findViewById(R.id.bt_fanspeed_ca);

		txtConsValFD = (TextView) findViewById(R.id.txt_cons_val_fd);
		txtTempValFD = (TextView) findViewById(R.id.txt_temp_val_fd);
		txtAutoStatusValFD = (TextView) findViewById(R.id.txt_auto_status_val_fd);
		txtConsValCA = (TextView) findViewById(R.id.txt_cons_val_ca);
		txtTempValCA = (TextView) findViewById(R.id.txt_temp_val_ca);
		txtAutoStatusValCA = (TextView) findViewById(R.id.txt_auto_status_val_ca);

		// For debug view
		final EditText edittext = (EditText) findViewById(R.id.et_highlight);
		edittext.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					filterDbgMsg();
					return true;
				}
				return false;
			}
		});

		// For light control view
		bedRoomValSB = (SeekBar) findViewById(R.id.sb_bedroom);
		bedRoomValSB.setOnSeekBarChangeListener(this);
		bedRoomVal = (TextView) findViewById(R.id.tv_bedroom_value);

		ImageButton bedRoomDimIB = (ImageButton) findViewById(R.id.ib_light_bed_dim);
		bedRoomDimIB.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				int brightnessVal = 222 - bedRoomValSB.getProgress();
				mPrefs.edit().putInt(prefsLightBedDim, brightnessVal).apply();
				/** Command for ESP */
				String cmd = String.valueOf(brightnessVal);
				cmd = "d=" + cmd;
				new ESPbyTCP(urlLB1,cmd,"0");
				return true;
			}
		});

		//noinspection deprecation
		colorRed = getResources().getColor(android.R.color.holo_red_light);
		//noinspection deprecation
		colorGrey = getResources().getColor(android.R.color.darker_gray);
		//noinspection deprecation
		colorOrange = getResources().getColor(android.R.color.holo_orange_light);
		//noinspection deprecation
		colorGreen = getResources().getColor(android.R.color.holo_green_light);

		/** Pointer to text views showing the consumed / produced energy */
		TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
		energyText.setVisibility(View.INVISIBLE);
		energyText = (TextView) findViewById(R.id.tv_solar_energy);
		energyText.setVisibility(View.INVISIBLE);

		/** Button to stop/start continuous UI refresh */
		Button btStop = (Button) findViewById(R.id.bt_stop);
		if (showingLog) {
			//noinspection deprecation
			btStop.setTextColor(getResources().getColor(android.R.color.holo_green_light));
			btStop.setText(getResources().getString(R.string.start));
		}

		// Get index of last selected device */
		selDevice = mPrefs.getInt(prefsSelDevice, 0);

		// Set visible view flag to security
		visibleView = mPrefs.getInt(prefsLastView, 0);
		// Set flag for debug output
		showDebug = mPrefs.getBoolean(prefsShowDebug, false);
	}

	/**
	 * Initializing method
	 * - Find all available devices
	 * - Check if Google Cloud Messaging is registered
	 * - Call initializing methods for all devices
	 */
	private class Initialize extends AsyncTask<String, Void, Void> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Void doInBackground(String... params) {

			String foundDevice = params[0];

			if (foundDevice.equalsIgnoreCase("spm") || foundDevice.equalsIgnoreCase("spMonitor")) {
				initSPM();
				return null;
			}
			if (foundDevice.equalsIgnoreCase("sf1") || foundDevice.equalsIgnoreCase("sb1")) {
				initSecurity(foundDevice);
				return null;
			}

			if (foundDevice.equalsIgnoreCase("fd1") || foundDevice.equalsIgnoreCase("ca1")) {
				initAircons(foundDevice);
				return null;
			}

			if (foundDevice.equalsIgnoreCase("lb1")) {
				initLights(foundDevice);
				return null;
			}

			return null;
		}
	}

	/**
	 * Initializing method for aircon control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initAircons(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("fd1")) { // Aircon 1 - Office
			// Get initial status from Aircon 1
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 1");
			// Update aircon status
			initHandler(8, "s", deviceIPs[aircon1Index], "fd1", "0", null, null);
			if (mPrefs.contains(prefsLocationName + "0")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "0", "");
			}
			// Update aircon 1 location name
			initHandler(4, locationName[0], "1", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "0")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 1 icon
			//noinspection deprecation
			initHandler(7, "1", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[0]]));
		}
		if (foundDevice.equalsIgnoreCase("ca1")) { // Aircon 2 - Living room
			// Get initial status from Aircon 2
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 2");
			// Update aircon 2 status
			initHandler(8, "s", deviceIPs[aircon2Index], "ca1", "1", null, null);
			if (mPrefs.contains(prefsLocationName + "1")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "1", "");
			}
			// Update aircon 2 location name
			initHandler(5, locationName[1], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "1")) {
				deviceIcon[1] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 2 icon
			//noinspection deprecation
			initHandler(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_ca),
					getResources().getDrawable(iconIDs[deviceIcon[1]]));
		}
		// TODO add third aircon if ever available
		if (foundDevice.equalsIgnoreCase("xy1")) { // Aircon 2 - Living room
			// Get initial status from Aircon 3
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 3");
			initHandler(8, "s", deviceIPs[aircon3Index], "xy1", "2", null, null);
			if (mPrefs.contains(prefsLocationName + "2")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "2", "");
			}
			// Update aircon 3 location name
			initHandler(6, locationName[2], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "2")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "2", 99);
			}
			// Update aircon 3 icon
			//noinspection deprecation
			initHandler(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[2]]));
		}
		if (!deviceIsOn[aircon1Index] && !deviceIsOn[aircon2Index] && !deviceIsOn[aircon3Index]) {
			// Show message no aircons found
			initHandler(2, getResources().getString(R.string.err_aircon),"","","", null, null);
		}
	}

	/**
	 * Initializing method for security control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initSecurity(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("sf1")) { // Security front
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of front Security");
			// Update security status front sensor
			initHandler(8, "s", deviceIPs[secFrontIndex], "sf1", Integer.toString(selDevice), null, null);
		}
		if (foundDevice.equalsIgnoreCase("sb1")) { // Security back
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of back Security");
			// Update security status back sensor
			initHandler(8, "s", deviceIPs[secBackIndex], "sb1", Integer.toString(selDevice), null, null);
			initHandler(10, "", "", "", "", null, null);
		}
	}

	/**
	 * Initializing method for solar panel monitor
	 * Check local databases and request update if necessary
	 * Send status update request
	 */
	private void initSPM() {
		if (deviceIsOn[spMonitorIndex]) { // spMonitor
			// Get initial status from spMonitor device
			// Get today's day for the online database name
			dbNamesList = Utilities.getDateStrings();

			// In case the database is not yet existing, open it once
			/** Instance of data base */
			SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();
			/** Instance of data base */
			dataBase = dbHelperLast.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();

			// Check if database is empty. If yes, sync only the database for this month
			dataBase = dbHelperNow.getReadableDatabase();
			/** Cursor with data from database */
			Cursor chCursor = DataBaseHelper.getLastRow(dataBase);
			if (chCursor != null) {
				if (chCursor.getCount() != 0) { // local database is not empty, no need can sync all data
					dataBaseIsEmpty = false;
				} else { // local database is empty, need to sync all data including last month
					needLastMonth = true;
				}
			}
			if (chCursor != null) {
				chCursor.close();
			}
			dataBase.close();

			// Start background sync of the database
			initHandler(9, dbNamesList[0], "", "", "", null, null);

			if (!dataBaseIsEmpty) { // Sync second database only if first one is not empty
				// Check if we have already synced the last month
				/** Instance of data base */
				dataBase = dbHelperLast.getReadableDatabase();
				/** Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				if (dbCursor != null) {
					try {
						if (dbCursor.getCount() == 0) { // local database is empty, need to sync all data
							needLastMonth = true;
						} else { // fill last log file array
							lastLogDates.clear();
							/** List with years in the database */
							ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
							for (int year = 0; year < yearsAvail.size(); year++) {
								/** List with months of year in the database */
								ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
										0, yearsAvail.get(year));
								for (int month = 0; month < monthsAvail.size(); month++) {
									/** List with days of month of year in the database */
									ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
											monthsAvail.get(month),
											yearsAvail.get(year));
									for (int day = 0; day < daysAvail.size(); day++) {
										lastLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
												.substring(String.valueOf(yearsAvail.get(year)).length()) +
												"-" + ("00" + String.valueOf(monthsAvail.get(month)))
												.substring(String.valueOf(monthsAvail.get(month)).length()) +
												"-" + ("00" + String.valueOf(daysAvail.get(day)))
												.substring(String.valueOf(daysAvail.get(day)).length()));
									}
								}
							}
							lastLogDatesIndex = lastLogDates.size() - 1;
						}
					} catch (IllegalStateException ignore) {
					}
				}
				if (dbCursor != null) {
					dbCursor.close();
				}
				dataBase.close();
			}

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of spMonitor");
		} else {
			dataBaseIsEmpty = false;
			// Show message not on home WiFi
			initHandler(1, getResources().getString(R.string.err_spMonitor), "", "", "", null, null);
			// Update of solar panel values
			initHandler(3, "/data/get", deviceIPs[spMonitorIndex], "spm", Integer.toString(selDevice), null, null);
		}
	}

	/**
	 * Initializing method for light control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initLights(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("lb1")) { // Bedroom light
			// Get initial status from light control
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of bedroom lights");
			// Update bedroom light status
			initHandler(8, "s", deviceIPs[lb1Index], "lb1", "0", null, null);
		}
	}

	/**
	 * Handle UI tasks of async task initialize()
	 *
	 * @param task
	 * 		task to take care of
	 * 	        0: Security screen status update
	 * 	        1: spMonitor screen status update
	 * 	        2: Aircon screen status update
	 * 	        3: start communication with Arduino Yun
	 * 	        4: Aircon 1 location & timer button text
	 * 	        5: Aircon 2 location & timer button text
	 * 	        6: Aircon 3 location & timer button text
	 * 	        7: Aircon location icon
	 * 	        8: start communication with ESP8266
	 * 	        9: Start background sync of database
	 * 	        10: Show backyard alarm status
	 * @param message
	 *      Update string for task 0, 1 and 2
	 *      Location text for task 4, 5 and 6
	 *      Command used in task 3
	 *      Database name in task 9
	 * @param url
	 *      URL to be used in task 3
	 * @param deviceID
	 *      Device ID to be used in task 3 and 8
	 * @param airconID
	 *      Aircon ID to be used in task 3
	 * @param iconImage
	 *      Image view of icon for task 7
	 * @param iconDrawable
	 *      Drawable of icon for task 7
	 */
	private void initHandler(final int task,
	                         final String message,
	                         final String url,
	                         final String deviceID,
	                         final String airconID,
	                         final ImageView iconImage,
	                         final Drawable iconDrawable) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings({"deprecation", "ConstantConditions"})
			@Override
			public void run() {
				/** Text view to show location name */
				TextView locationText;
				/** Timer button */
				Button btTimer;
				/** Text for timer button */
				String timerTime;
				switch (task) {
					case 0: // Security screen status update
						if (showDebug) {
							secStatus.setText(message);
						} else {
							secStatus.setText("");
						}
						break;
					case 1: // spMonitor screen status update
						solStatus.setText(message);
						break;
					case 2: // Aircon screen status update
						if (showDebug) {
							airStatus.setText(message);
						} else {
							airStatus.setText("");
						}
						break;
					case 3: // start communication with Arduino Yun
						new SPMPcommunication().execute(url, message, "", deviceID, airconID);
						break;
					case 4: // Aircon 1 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_fd);
						timerTime = Integer.toString(deviceTimer[0]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 5: // Aircon 2 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_ca);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_ca);
						timerTime = Integer.toString(deviceTimer[1]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 6: // Aircon 3 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_fd);
						timerTime = Integer.toString(deviceTimer[2]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 7: // Aircon location icon
						iconImage.setImageDrawable(iconDrawable);
						break;
					case 8: // start communication with ESP8266
						if (url.equalsIgnoreCase("")) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Empty address request on device " + deviceID
									+ " msg= " + message);
						}
						new ESPbyTCP(url, message, deviceID);
						break;
					case 9: // Start background sync of database
						atNow = new syncSolarDB().execute(message);
						break;
					case 10:
						TableLayout backYardDots = (TableLayout) findViewById(R.id.tl_alarm_back);
						backYardDots.setVisibility(View.VISIBLE);
						break;
					case 11:
				}
			}
		});
	}

	/**
	 * Switch to requested UI
	 *
	 * @param uiSelected
	 *            0 = Security UI
	 *            1 = Solar panel UI
	 *            2 = Aircon control UI
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private void switchUI(int uiSelected) {

		/** Pointer to action bar */
		Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
		/** Color of toolBar background */
		Drawable toolBarDrawable;
		/** Menu item pointer */
		MenuItem menuItem;
		/** Color for status bar */
		int statusBarColor;
		/** Color for action bar */
		int actionBarColor;

		switch (uiSelected) {
			case 0: // Security UI
				statusBarColor = getResources().getColor(R.color.colorPrimaryDark);
				actionBarColor = getResources().getColor(R.color.colorPrimary);
				if (abMenu != null) {
					// Make security menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				// Make security view visible
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == 3) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = 0;
				break;
			case 2: // Aircon control UI
				statusBarColor = getResources().getColor(android.R.color.holo_blue_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_blue_light);
				if (abMenu != null) {
					// Make aircon menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(true);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == 3) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = 2;
				break;
			case 3: // Device Debug UI
				statusBarColor = getResources().getColor(android.R.color.holo_orange_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_orange_light);
				if (abMenu != null) {
					// Make aircon menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.VISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				visibleView = 3;
				new mqttDebugAsync().execute("subscribe");
				break;
			case 4: // Security camera view
				statusBarColor = getResources().getColor(R.color.colorPrimaryDark);
				actionBarColor = getResources().getColor(R.color.colorPrimary);
				if (abMenu != null) {
					// Make security menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.VISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == 3) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = 4;
				break;
			case 5: // Light control view
				statusBarColor = getResources().getColor(android.R.color.holo_green_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_green_light);
				if (abMenu != null) {
					// Make solar panel menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.VISIBLE);
				if (visibleView == 3) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = 5;
				new ESPbyTCP(urlLB1, "s", "0");
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send status request to lights");
				break;
			case 1: // Solar panel UI == default view on startup
			default:
				statusBarColor = getResources().getColor(android.R.color.holo_green_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_green_light);
				if (abMenu != null) {
					// Make solar panel menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == 3) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = 1;
//				if (ChartHelper.autoRefreshOn) {
//					ChartHelper.initChart(true, appContext, chartTitle);
//				} else {
//					ChartHelper.initChart(false, appContext, chartTitle);
//				}
				ChartHelper.initChart(ChartHelper.autoRefreshOn, appContext, chartTitle);
				break;
		}
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			getWindow().setStatusBarColor(statusBarColor);
		}
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			toolBarDrawable = new ColorDrawable(actionBarColor);
			actionBar.setBackground(toolBarDrawable);
		}
		mPrefs.edit().putInt(prefsLastView,visibleView).apply();
	}

	/**
	 * Handle Security view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from security view
	 */
	@SuppressWarnings("deprecation")
	private boolean handleSecurityButtons(View v) {
		/** Flag if button was handled */
		boolean wasSecButton = true;
		/** URL for communication with ESP */
		String url = "";
		/** Command for ESP */
		String cmd = "";
		/** DeviceID used for MQTT */
		String deviceID = "";
		switch (v.getId()) {
			case R.id.dot_alarm_status:
				url = deviceIPs[secFrontIndex];
				if (hasAlarmOnFront) {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					cmd = "a=0";
				} else {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_on));
					cmd = "a=1";
				}
				deviceID = "sf1";
				break;
			case R.id.dot_alarm_status_back:
				url = deviceIPs[secBackIndex];
				if (hasAlarmOnBack) {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					cmd = "a=0";
				} else {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_on));
					cmd = "a=1";
				}
				deviceID = "sb1";
				break;
			case R.id.dot_light:
				ivLightStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = deviceIPs[secFrontIndex];
				cmd = "b";
				deviceID = "sf1";
				break;
			case R.id.dot_light_back:
				ivLightStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = deviceIPs[secBackIndex];
				cmd = "b";
				deviceID = "sb1";
				break;
			case R.id.cb_sec_auto_alarm:
				url = deviceIPs[secFrontIndex];
				if (secAutoAlarmFront.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					cmd = "a=2," + onTime + "," + offTime;
					secAutoAlarmFront.setText(getResources().getString(R.string.sec_auto_alarm_on,secAutoOn,secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					cmd = "a=3";
					secAutoAlarmFront.setText(getResources().getString(R.string.sec_auto_alarm_off));
				}
				deviceID = "sf1";
				break;
			case R.id.cb_sec_auto_alarm_2:
				url = deviceIPs[secBackIndex];
				if (secAutoAlarmBack.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					cmd = "a=2," + onTime + "," + offTime;
					secAutoAlarmBack.setText(getResources().getString(R.string.sec_auto_alarm_on,secAutoOn,secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					cmd = "a=3";
					secAutoAlarmBack.setText(getResources().getString(R.string.sec_auto_alarm_off));
				}
				deviceID = "sb1";
				break;
			case R.id.tv_change_alarm:
				final Dialog alarmDlg = new Dialog(MyHomeControl.this);
				final int orgOnTime = secAutoOnStored;
				final int orgOffTime = secAutoOffStored;
				alarmDlg.setTitle("NumberPicker");
				alarmDlg.setContentView(R.layout.alarm_settings);
				Button cancelButton = (Button) alarmDlg.findViewById(R.id.bt_sec_cancel);
				Button okButton = (Button) alarmDlg.findViewById(R.id.bt_sec_ok);
				final NumberPicker npOnTime = (NumberPicker) alarmDlg.findViewById(R.id.np_Alarm_on);
				npOnTime.setMaxValue(23);
				npOnTime.setMinValue(0);
				npOnTime.setValue(secAutoOnStored);
				npOnTime.setWrapSelectorWheel(false);
				npOnTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOnStored = newVal;
					}
				});
				final NumberPicker npOffTime = (NumberPicker) alarmDlg.findViewById(R.id.np_Alarm_off);
				npOffTime.setMaxValue(23);
				npOffTime.setMinValue(0);
				npOffTime.setValue(secAutoOffStored);
				npOffTime.setWrapSelectorWheel(false);
				npOffTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOffStored = newVal;
					}
				});
				cancelButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Reset changed values
						secAutoOnStored = orgOnTime;
						secAutoOffStored = orgOffTime;
						alarmDlg.dismiss();
					}
				});
				okButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String onTime = Integer.toString(secAutoOnStored);
						if (secAutoOnStored < 10) {
							onTime = "0" + onTime;
						}
						String offTime = Integer.toString(secAutoOffStored);
						if (secAutoOffStored < 10) {
							offTime = "0" + offTime;
						}
						new ESPbyTCP(deviceIPs[secFrontIndex],
								"a=2," + onTime + "," + offTime, "sf1");
						new ESPbyTCP(deviceIPs[secBackIndex],
								"a=2," + onTime + "," + offTime, "sb1");

						alarmDlg.dismiss();
					}
				});
				alarmDlg.show();
				break;
			case R.id.ib_snapshot_front:
			case R.id.ib_snapshot_front2:
				url = deviceIPs[cam1Index];
				cmd = "t";
				deviceID = "cm1";
				break;
			case R.id.ib_snapshot_last:
			case R.id.ib_snapshot_last2:
				cmd = "";
				new galleryCommunication().execute("93.104.213.79/gallery", "/get.php", "1");
				break;
			case R.id.ib_snapshot_gallery:
			case R.id.ib_snapshot_gallery2:
				cmd = "";
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://giesecke.tk/gallery"));
				startActivity(browserIntent);
				break;
			default:
				wasSecButton = false;
				break;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url,cmd, deviceID);
		}
		return wasSecButton;
	}

	/**
	 * Handle Solar panel view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from solar panel view
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private boolean handleSPMbuttons(View v) {
		/** Flag if button was handled */
		boolean wasSPMbutton = true;
		/** Button to go to previous  log */
		Button prevButton  = (Button) findViewById(R.id.bt_prevLog);
		/** Button to go to next log */
		Button nextButton  = (Button) findViewById(R.id.bt_nextLog);
		/** The application context */
		Context thisAppContext = getApplicationContext();

		switch (v.getId()) {
			case R.id.bt_prevLog:
				if (logDatesIndex == 0) {
					if ((lastLogDatesIndex == lastLogDates.size() - 1) && !showingLast) {
						lastLogDatesIndex++;
					}
					showingLast = true;
				} else {
					showingLast = false;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex > 0) {
						logDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet, appView);
						ChartHelper.initChart(false, thisAppContext, chartTitle);
						newDataSet.close();
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				} else { // use last months database
					if (lastLogDatesIndex > 0) {
						lastLogDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet, appView);
						ChartHelper.initChart(false, thisAppContext, chartTitle);
						newDataSet.close();
						dataBase.close();

						if (lastLogDatesIndex == 0) {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				}
				break;
			case R.id.bt_nextLog:
				if (lastLogDatesIndex == lastLogDates.size() - 1) {
					if ((logDatesIndex == 0) && showingLast) {
						logDatesIndex--;
					}
					showingLast = false;
				} else {
					showingLast = true;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex < logDates.size() - 1) {
						logDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet, appView);
						ChartHelper.initChart(false, thisAppContext, chartTitle);
						newDataSet.close();
						dataBase.close();

						if (logDatesIndex == logDates.size() - 1) {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				} else { // use last months database
					if (lastLogDatesIndex < lastLogDates.size() - 1) {
						lastLogDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet, appView);
						ChartHelper.initChart(false, thisAppContext, chartTitle);
						newDataSet.close();
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				}
				break;
			case R.id.bt_stop:
				if (ChartHelper.autoRefreshOn) {
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					ChartHelper.autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						if (Utilities.isHomeWiFi(this)) {
							atNow = new syncSolarDB().execute(dbNamesList[0]);
						}

						/** Pointer to text views showing the consumed / produced energy */
						TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = (TextView) findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						logDatesIndex = logDates.size() - 1;
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					}
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					stopButton.setText(getResources().getString(R.string.stop));
					ChartHelper.autoRefreshOn = true;
				}
				break;
			default:
				wasSPMbutton = false;
				break;
		}
		return wasSPMbutton;
	}

	/**
	 * Handle Aircon view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from aircon view
	 */
	@SuppressWarnings("ConstantConditions")
	private boolean handleAirconButtons(View v) {
		/** Flag if button was handled */
		boolean wasAirconButton = true;
		/** URL for communication with ESP */
		String url = espIP[selDevice];
		/** DeviceID used for MQTT */
		String deviceID = deviceName[selDevice];
		/** Command for ESP */
		String cmd = "";
		/** Timer button */
		Button btTimer;
		/** Text for timer button */
		String timerTime;

		switch (v.getId()) {
			case R.id.bt_auto_fd:
			case R.id.bt_auto_ca:
				if (autoOnStatus[selDevice] == 1) {
					cmd = "c=" + CMD_AUTO_OFF;
				} else {
					cmd = "c=" + CMD_AUTO_ON;
				}
				break;
			case R.id.bt_on_off_fd:
			case R.id.bt_on_off_ca:
				cmd = "c=" + CMD_ON_OFF;
				break;
			case R.id.bt_fan_high_fd:
				if (fanStatus[selDevice] != 2) {
					cmd = "c=" + CMD_FAN_HIGH;
				}
				break;
			case R.id.bt_fan_med_fd:
				if (fanStatus[selDevice] != 1) {
					cmd = "c=" + CMD_FAN_MED;
				}
				break;
			case R.id.bt_fan_low_fd:
				if (fanStatus[selDevice] != 0) {
					cmd = "c=" + CMD_FAN_LOW;
				}
				break;
			case R.id.bt_autom_ca:
				if (modeStatus[selDevice] != 3) {
					cmd = "c=" + CMD_MODE_AUTO;
				}
				break;
			case R.id.bt_cool_fd:
			case R.id.bt_cool_ca:
				if (modeStatus[selDevice] != 2) {
					cmd = "c=" + CMD_MODE_COOL;
				}
				break;
			case R.id.bt_dry_fd:
			case R.id.bt_dry_ca:
				if (modeStatus[selDevice] != 1) {
					cmd = "c=" + CMD_MODE_DRY;
				}
				break;
			case R.id.bt_fan_fd:
			case R.id.bt_fan_ca:
				if (modeStatus[selDevice] != 0) {
					cmd = "c=" + CMD_MODE_FAN;
				}
				break;
			case R.id.bt_sweep_ca:
				cmd = "c=" + CMD_OTHER_SWEEP;
				break;
			case R.id.bt_turbo_ca:
				cmd = "c=" + CMD_OTHER_TURBO;
				break;
			case R.id.bt_ion_ca:
				cmd = "c=" + CMD_OTHER_ION;
				break;
			case R.id.bt_plus_fd:
			case R.id.bt_plus_ca:
				cmd = "c=" + CMD_TEMP_PLUS;
				break;
			case R.id.bt_minus_fd:
			case R.id.bt_minus_ca:
				cmd = "c=" + CMD_TEMP_MINUS;
				break;
			case R.id.bt_fanspeed_ca:
				cmd = "c=" + CMD_FAN_SPEED;
				break;
			case R.id.im_icon_fd:
				if (deviceIsOn[aircon2Index]) { // Is Carrier aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.VISIBLE);
					selDevice = 1;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.im_icon_ca:
				if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon1Index]) {
					airCAView.setVisibility(View.INVISIBLE);
					airFDView.setVisibility(View.VISIBLE);
					selDevice = 0;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.bt_timer_fd:
			case R.id.bt_timer_ca:
				if (selDevice < 2) { // Only Aircon 1 and 2 support timer for now
					// TODO
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Setting timer to " + deviceTimer[selDevice]);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "t=" + Integer.toString(deviceTimer[selDevice]));
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Second command = " + "c="  + CMD_OTHER_TIMER);
					new ESPbyTCP(url, "t=" + Integer.toString(deviceTimer[selDevice]), deviceID);
					cmd = "c=" + CMD_OTHER_TIMER;
				}
				break;
			case R.id.bt_timer_minus_fd:
			case R.id.bt_timer_minus_ca:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] > 1) {
						deviceTimer[selDevice]--;
						cmd = "t=" + Integer.toString(deviceTimer[selDevice]);
					}
					btTimer = (Button) findViewById(R.id.bt_timer_fd);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			case R.id.bt_timer_plus_fd:
			case R.id.bt_timer_plus_ca:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] < 9) {
						deviceTimer[selDevice]++;
						cmd = "t=" + Integer.toString(deviceTimer[selDevice]);
					}
					btTimer = (Button) findViewById(R.id.bt_timer_fd);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			default: // End here if it was not an aircon view button
				wasAirconButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url, cmd, deviceID);
		}
		return wasAirconButton;
	}

	/**
	 * Handle Debug view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from debug view
	 */
	@SuppressWarnings("ConstantConditions")
	private boolean handleDebugButtons(View v) {
		/** Flag if button was handled */
		boolean wasDebugButton = true;
		/** URL for communication with ESP */
		String url = espIP[selDevice];
		/** DeviceID used for MQTT */
		String deviceID = "chk";
		/** Command for ESP */
		String cmd = "";
		/** Textview to show the incoming debug messages */
		TextView debugTxtView = (TextView) findViewById(R.id.tv_sv_debug);

		switch (v.getId()) {
			case R.id.bt_clear:
				debugTxtView.setText("");
				debugMsgs = "";
				break;
			case R.id.bt_debug_moni:
				cmd = "d";
				url = deviceIPs[moniIndex];
				break;
			case R.id.bt_res_moni:
				cmd = "r";
				url = deviceIPs[moniIndex];
				break;
			case R.id.bt_debug_secf:
				cmd = "d";
				url = deviceIPs[secFrontIndex];
				break;
			case R.id.bt_res_secf:
				cmd = "r";
				url = deviceIPs[secFrontIndex];
				break;
			case R.id.bt_debug_secb:
				cmd = "d";
				url = deviceIPs[secBackIndex];
				break;
			case R.id.bt_res_secb:
				cmd = "r";
				url = deviceIPs[secBackIndex];
				break;
			case R.id.bt_debug_ac1:
				cmd = "d";
				url = espIP[0];
				break;
			case R.id.bt_res_ac1:
				cmd = "r";
				url = espIP[0];
				break;
			case R.id.bt_debug_ac2:
				cmd = "d";
				url = espIP[1];
				break;
			case R.id.bt_res_ac2:
				cmd = "r";
				url = espIP[1];
				break;
			case R.id.bt_debug_cam1:
				cmd = "d";
				url = deviceIPs[cam1Index];
				break;
			case R.id.bt_res_cam1:
				cmd = "r";
				url = deviceIPs[cam1Index];
				break;
			case R.id.bt_highlight:
				filterDbgMsg();
				break;
			default: // End here if it was not an aircon view button
				wasDebugButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url, cmd, deviceID);
		}
		return wasDebugButton;
	}

	/**
	 * Handle Light control view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from light control view
	 */
	@SuppressLint("SetTextI18n")
	@SuppressWarnings("ConstantConditions")
	private boolean handleLightButtons(View v) {
		/** Flag if button was handled */
		boolean wasLightButton = true;
		/** DeviceID used for MQTT */
		String deviceID = "chk";
		/** Command for ESP */
		String cmd = "";

		switch (v.getId()) {
			case R.id.ib_light_bed_dim:
				int dimLightLevel = mPrefs.getInt(prefsLightBedDim,200);
				String lightValue = String.valueOf(dimLightLevel);
				cmd = "b="+lightValue;
				bedRoomVal.setText(getString(R.string.lights_val_dim));
				bedRoomValSB.setProgress(222-dimLightLevel);
				break;
			case R.id.ib_light_bed_off:
				cmd = "b=255";
				bedRoomVal.setText(getString(R.string.lights_val_off));
				bedRoomValSB.setProgress(0);
				break;
			case R.id.ib_light_bed_on:
				cmd = "b=140";
				bedRoomVal.setText(getString(R.string.lights_val_on));
				bedRoomValSB.setProgress(82);
				break;
			default: // End here if it was not an light control view button
				wasLightButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(urlLB1, cmd, deviceID);
		}
		return wasLightButton;
	}

	/**
	 * Filter debug message with new search phrase
	 * or reset to full text if search phrase is empty
	 */
	private void filterDbgMsg() {
		/** Textview to show the incoming debug messages */
		TextView debugTxtView = (TextView) findViewById(R.id.tv_sv_debug);
		/** Edittext to highlight search word in the incoming debug messages */
		EditText debugTxtHighlight = (EditText) findViewById(R.id.et_highlight);

		highlightText = debugTxtHighlight.getText().toString();
		ScrollView debugTxtScroll = (ScrollView) findViewById(R.id.sv_debugview);

		if (highlightText == null || highlightText.isEmpty()) { //if no search text is entered
			debugTxtView.setText(debugMsgs);
			debugTxtScroll.fullScroll(View.FOCUS_DOWN);
		} else {
			StringBuilder output = new StringBuilder();
			String line;
			if (debugMsgs.startsWith("null")) {
				debugMsgs = debugMsgs.substring(4);
			}
			try {
				BufferedReader reader = new BufferedReader(
						new StringReader(debugMsgs));
				while ((line = reader.readLine()) != null) {

					if ((line.length() > 0)
							&& (line.toUpperCase().contains(highlightText.toUpperCase()))) {
						output.append(line).append("\n");
					}
				}
			} catch (Exception ignore) {
				debugTxtView.setText(debugMsgs);
				debugTxtScroll.fullScroll(View.FOCUS_DOWN);
			}
			debugTxtView.setText(output);
		}
		debugTxtScroll.fullScroll(View.FOCUS_DOWN);
	}

	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			snackBarText = "";
		}
	};
}

