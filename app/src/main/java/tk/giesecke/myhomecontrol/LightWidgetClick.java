package tk.giesecke.myhomecontrol;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class LightWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LIGHT-W";

	public LightWidgetClick() {
		super("LightWidgetClick");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Bundle received when service is called */
			Bundle extras = intent.getExtras();
			if (extras != null) {
				/** App widget id */
				int mAppWidgetId = extras.getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);

				// If they gave us an intent without the widget id, just bail.
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
					return;
				}

				/** App widget manager for all widgets of this app */
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

				// TODO if we are not on home wifi send command over MQTT instead of WiFi

				/** A HTTP client to access the ESP device */
				// Set timeout to 5 minutes in case we have a lot of data to load
				OkHttpClient client = new OkHttpClient.Builder()
						.connectTimeout(300, TimeUnit.SECONDS)
						.writeTimeout(10, TimeUnit.SECONDS)
						.readTimeout(300, TimeUnit.SECONDS)
						.build();

				/** URL to be called */
				String urlString = MyHomeControl.SECURITY_URL_FRONT_1 + "/?b"; // URL to call

				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP = " + urlString);

				/** Request to ESP device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from ESP device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							int status = response.code();
							if (status == 200) {
								LightWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId, true);
								/** Timer to change back widget icon */
								Timer timer = new Timer();
								timer.schedule(new changeBackWidget(this, appWidgetManager, mAppWidgetId), 120000);
							}
						}
					} catch (IOException e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP failed = " + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Class to switch back the widget icon after 2 minutes
	 */
	private class changeBackWidget extends TimerTask
	{
		/** Application context */
		final Context context;
		/** Instance of app widget manager */
		final AppWidgetManager appWidgetManager;
		/** ID of widget */
		final int appWidgetId;

		public changeBackWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetId = appWidgetId;
		}

		@Override
		public void run() {
			LightWidget.updateAppWidget(context, appWidgetManager, appWidgetId, false);
		}
	}
}
