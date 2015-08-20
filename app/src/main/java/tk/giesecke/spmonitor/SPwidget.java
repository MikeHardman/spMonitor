package tk.giesecke.spmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;

/** spMonitor - SPwidget
 *
 * Implementation of App Widget functionality.
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class SPwidget extends AppWidgetProvider {

	/** broadcast signature for widget update */
	public static final String SP_WIDGET_UPDATE = "SP_WIDGET_UPDATE";

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		//*******************************************************
		// Receiver for the widget (click on widget, update service,
		// disable, ... we handle only the update requests here
		//*******************************************************

		super.onReceive(context, intent);

		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				SPwidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		/** Remote views of the widgets */
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sp_widget);

		if (SP_WIDGET_UPDATE.equals(intent.getAction())) {
			for (int appWidgetId : appWidgetIds) {
				appWidgetManager.updateAppWidget(appWidgetId, views);
			}

			onUpdate(context, appWidgetManager, appWidgetIds);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// When the user deletes the widget, delete the preference associated with it.
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		/** Instance of the shared preferences */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor",0);
		mPrefs.edit().putInt("wNums",0).apply();
		/** Intent to start scheduled update of the widgets */
		Intent intent = new Intent(SPwidget.SP_WIDGET_UPDATE);
		/** Pending intent for broadcast message to update widgets */
		PendingIntent pendingIntent = PendingIntent.getBroadcast(
				context, 2701, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		/** Alarm manager for scheduled widget updates */
		AlarmManager alarmManager = (AlarmManager) context.getSystemService
				(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                            int appWidgetId) {

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Construct the RemoteViews object
		RemoteViews views;
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
		if (mPrefs.getBoolean("wSizeLarge",true)) {
			views = new RemoteViews(context.getPackageName(), R.layout.sp_widget_large);
		} else {
			views = new RemoteViews(context.getPackageName(), R.layout.sp_widget);
		}

		// Create an Intent to launch MainActivity
		/** Intent to start app if widget is pushed */
		Intent intent1 = new Intent(context, SplashActivity.class);
		intent1.putExtra("appWidgetId", appWidgetId);
		// Creating a pending intent, which will be invoked when the user
		// clicks on the widget
		/** Pending intent to start app if widget is pushed */
		PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 0,
				intent1, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the battery icon
		views.setOnClickPendingIntent(R.id.rlWidget1, pendingIntent1);

		/** URL of the spMonitor device */
		String deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

		// 1) Check if we have an IP address
		if (deviceIP.equalsIgnoreCase("no IP saved")) { // No spMonitor device available
			views.setTextViewText(R.id.tv_widgetRow1Value,
					context.getResources().getString(R.string.widgetCommError1));
			views.setTextViewText(R.id.tv_widgetRow2Value,
					context.getResources().getString(R.string.widgetCommError2));
			views.setTextViewText(R.id.tv_widgetRow3Value,
					context.getResources().getString(R.string.widgetCommError3));
		} else {
			// 2) Check if WiFi is enabled
			/** Access to connectivity manager */
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			android.net.NetworkInfo wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			wifiOn.getDetailedState();
			if (wifiOn.getDetailedState().name().equalsIgnoreCase("DISCONNECTED")) {
				views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().getString(R.string.widgetCommError1));
				views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().getString(R.string.widgetCommError2));
				views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().getString(R.string.widgetCommError3));
			} else {
				/** String list with parts of the URL */
				String[] ipValues = deviceIP.split("/");
				/** String with the URL to get the data */
				String urlString="http://"+ipValues[2]+"/data/get"; // URL to call
				/** Response from the spMonitor device or error message */
				String resultToDisplay = "";
				/** A HTTP client to access the spMonitor device */
				OkHttpClient client = new OkHttpClient();
				/** Solar power received from spMonitor device as minute average */
				Float solarPowerMin = 0.0f;
				/** Consumption received from spMonitor device as minute average */
				Float consPowerMin = 0.0f;

				/** Request to spMonitor device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from spMonitor device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							resultToDisplay = response.body().string();
						}
					} catch (IOException e) {
						views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().getString(R.string.widgetCommError1));
						views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().getString(R.string.widgetCommError2));
						views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().getString(R.string.widgetCommError3));
					}
				}

				if (resultToDisplay.equalsIgnoreCase("")) {
					views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().getString(R.string.widgetCommError1));
					views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().getString(R.string.widgetCommError2));
					views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().getString(R.string.widgetCommError3));
				} else {
					// decode JSON
					if (Utilities.isJSONValid(resultToDisplay)) {
						try {
							/** JSON object containing result from server */
							JSONObject jsonResult = new JSONObject(resultToDisplay);
							/** JSON object containing the values */
							JSONObject jsonValues = jsonResult.getJSONObject("value");

							try {
								solarPowerMin = Float.parseFloat(jsonValues.getString("S"));
								consPowerMin = Float.parseFloat(jsonValues.getString("C"));
							} catch (Exception ignore) {
								views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().getString(R.string.widgetCommError1));
								views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().getString(R.string.widgetCommError2));
								views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().getString(R.string.widgetCommError3));
							}

							/** Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = solarPowerMin + consPowerMin;

							views.setTextViewText(R.id.tv_widgetRow1Value, String.format("%.0f", resultPowerMin) + "W");
							views.setTextViewText(R.id.tv_widgetRow2Value, String.format("%.0f", Math.abs(consPowerMin)) + "W");
							views.setTextViewText(R.id.tv_widgetRow3Value, String.format("%.0f", solarPowerMin) + "W");

							if (consPowerMin > 0.0d) {
								views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
										.getColor(android.R.color.holo_red_light));
							} else {
								views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
										.getColor(android.R.color.holo_green_light));
							}
						} catch (Exception ignore) {
							views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().getString(R.string.widgetCommError1));
							views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().getString(R.string.widgetCommError2));
							views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().getString(R.string.widgetCommError3));
						}
					}
				}
			}
		}
		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}

