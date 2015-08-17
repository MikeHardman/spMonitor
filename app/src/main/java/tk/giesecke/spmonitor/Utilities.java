package tk.giesecke.spmonitor;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/** spMonitor - Utilities
 *
 * Utilities used by SplashActivity and/or spMonitor
 *
 * @author Bernd Giesecke
 * @version 0.1 beta August 13, 2015.
 */
class Utilities {

	/**
	 * Scan the local subnet and return a list of IP addresses found
	 *
	 * @param subnet
	 *            Base IP address of the subnet
	 * @return <ArrayList>hosts</ArrayList>
	 *            Array list with all found IP addresses
	 */
	private static ArrayList<String> scanSubNet(String subnet){
		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		subnet = subnet.substring(0, subnet.lastIndexOf("."));
		subnet += ".";
		for(int i=0; i<255; i++){
			try {
				/** IP address under test */
				/** IP address under test */
				InetAddress inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
				if(inetAddress.isReachable(500)){
					hosts.add(inetAddress.getHostName());
					Log.d("spMonitor", inetAddress.getHostName());
				}
			} catch (IOException e) {
				Log.d("spMonitor", "Exception "+e);
				e.printStackTrace();
			}
		}

		return hosts;
	}

	/**
	 * Sends a HTTP request to an IP to test if it is the spMonitor
	 *
	 * @param ip
	 *            IP address to test
	 * @return resultToDisplay
	 *            HTTP response from the IP
	 */
	public static String checkDeviceIP(String ip) {
		/** URL including command e to be send to the spMonitor device */
		String urlString="http://"+ip+"/arduino/e"; // URL to call
		/** Response from the URL under test */
		String resultToDisplay;

		spMonitor.client.setConnectTimeout(5, TimeUnit.SECONDS); // connect timeout
		spMonitor.client.setReadTimeout(5, TimeUnit.SECONDS);    // socket timeout

		/** Request to get HTTP content from URL */
		Request request = new Request.Builder()
				.url(urlString)
				.build();

		/** Response from spMonitor device */
		Response response;
		try {
			response = spMonitor.client.newCall(request).execute();
			resultToDisplay = response.body().string();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return resultToDisplay;
	}

	/**
	 * Customized alert
	 *
	 * @return <String>deviceIP</String>context
	 *            URL address of the spMonitor device
	 */
	public static String searchDeviceIP() {
		/** Instance of WiFi manager */
		WifiManager wm = (WifiManager) spMonitor.appContext.getSystemService(Context.WIFI_SERVICE);
		/** IP address assigned to this device */
		@SuppressWarnings("deprecation") String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		/* List with all IP address found on this subnet */
		ArrayList<String> hosts = Utilities.scanSubNet(ip);
		spMonitor.deviceIP = "";
		for (int i=0; i<hosts.size(); i++) {
			ip = hosts.get(i);
			/** Result of check if spMonitor is on this IP address */
			String result = Utilities.checkDeviceIP(ip);
			if (result.startsWith("F ")) {
				spMonitor.deviceIP = "http://"+ip+"/arduino/";
				return spMonitor.deviceIP;
			}
		}
		return spMonitor.deviceIP;
	}

	/**
	 * Check if JSON object is valid
	 *
	 * @param test
	 *            String with JSON object or array
	 * @return boolean
	 *            true if "test" us a JSON object or array
	 *            false if no JSON object or array
	 */
	public static boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Start animation of refresh icon in action bar
	 */
	public static void startRefreshAnim() {
		/** Progressbar that will be shown instead of image button during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.pb_refresh_rot);
		refreshRot.setVisibility(View.VISIBLE);
		spMonitor.isCommunicating = true;
	}

	/**
	 * Stop animation of refresh icon in action bar
	 */
	public static void stopRefreshAnim() {
		/** Progressbar that will be shown instead of image button during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.pb_refresh_rot);
		refreshRot.setVisibility(View.INVISIBLE);
		spMonitor.isCommunicating = false;
	}

	/**
	 * Check if the device is a tablet or a phone
	 * by checking device configuration
	 *
	 * @param context
	 *          Application context
	 * @return <boolean>isTablet</boolean>
	 *          True if device is a tablet
	 *          False if device is a phone
	 */
	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Get min and max value of 2 series, depending on which series are
	 * visible
	 *
	 * @return <double[]>minMax</double[]>
	 *              minMax[0] = min value or 0 if no series is shown
	 *              minMax[1] = max value or 2000 if no series is shown
	 */
	public static double[] getMinMax() {
		/** Holding the min and max values from comparison result */
		double[] minMax = new double[2];
		minMax[0] = 0;
		minMax[1] = 2000;

		if (spMonitor.showSolar && spMonitor.showCons) {
			minMax[0] = Math.min(spMonitor.solarSeries.getLowestValueY(), spMonitor.consSeries.getLowestValueY());
			minMax[1] = Math.max(spMonitor.solarSeries.getHighestValueY(), spMonitor.consSeries.getHighestValueY());
		} else if (spMonitor.showSolar) {
			minMax[0] = spMonitor.solarSeries.getLowestValueY();
			minMax[1] = spMonitor.solarSeries.getHighestValueY();
		} else {
			minMax[0] = spMonitor.consSeries.getLowestValueY();
			minMax[1] = spMonitor.consSeries.getHighestValueY();
		}
		return minMax;
	}

	/**
	 * Load plot series with data received from database
	 *
	 * @param data
	 *        database cursor with recorded values
	 *        each cursor entry has 8 values
	 *        cursor[0] = year stamp
	 *        cursor[1] = month stamp
	 *        cursor[2] = day stamp
	 *        cursor[3] = hour stamp
	 *        cursor[4] = minute stamp
	 *        cursor[5] = sensor power
	 *        cursor[6] = consumed power
	 *        cursor[7] = light value
	 */
	public static void fillSeries(Cursor data) {

		data.moveToFirst();
		spMonitor.solarEnergy = 0f;
		spMonitor.consEnergy = 0f;

		ArrayList<Long> tempTimeStamps;
		ArrayList<Float> tempSolarStamps;
		ArrayList<Float> tempConsStamps;
		ArrayList<Long> tempLightStamps;
		if (spMonitor.showingLog) {
			tempTimeStamps = spMonitor.timeStamps;
			tempSolarStamps = spMonitor.solarPower;
			tempConsStamps = spMonitor.consumPower;
			tempLightStamps = spMonitor.lightValue;
		} else {
			tempTimeStamps = spMonitor.timeStampsCont;
			tempSolarStamps = spMonitor.solarPowerCont;
			tempConsStamps = spMonitor.consumPowerCont;
			tempLightStamps = spMonitor.lightValueCont;
		}
		tempTimeStamps.clear();
		tempSolarStamps.clear();
		tempConsStamps.clear();
		tempLightStamps.clear();
		for (int cursorIndex=0; cursorIndex<data.getCount(); cursorIndex++) {
			/** Gregorian calender to calculate the time stamp */
			Calendar timeCal = new GregorianCalendar(
					1970,
					1,
					1,
					data.getInt(3),
					data.getInt(4),
					0);
			tempTimeStamps.add(timeCal.getTimeInMillis());
			spMonitor.dayToShow = String.valueOf(data.getInt(0)) + "/" +
					String.valueOf(data.getInt(1)) + "/" +
					String.valueOf(data.getInt(2));
			tempSolarStamps.add(data.getFloat(5));
			tempConsStamps.add(data.getFloat(6));
			tempLightStamps.add(data.getLong(7));
			spMonitor.solarEnergy += data.getFloat(5)/60/1000;
			spMonitor.consEnergy += data.getFloat(6)/60/1000;
			data.moveToNext();
		}

		if (spMonitor.showingLog) {
			/** Text view to show consumed / produced energy */
			TextView energyText = (TextView) spMonitor.appView.findViewById(R.id.tv_cons_energy);
			energyText.setVisibility(View.VISIBLE);
			energyText.setText("Consumed: " + String.format("%.3f", spMonitor.consEnergy) + "kWh");
			energyText = (TextView) spMonitor.appView.findViewById(R.id.tv_solar_energy);
			energyText.setVisibility(View.VISIBLE);
			energyText.setText("Produced: " + String.format("%.3f", spMonitor.solarEnergy) + "kWh");
		}
		/** Text view to show max consumed / produced power */
		TextView maxPowerText = (TextView) spMonitor.appView.findViewById(R.id.tv_cons_max);
		maxPowerText.setText("(" + String.format("%.0f", Collections.max(tempConsStamps)) + "W)");
		maxPowerText = (TextView) spMonitor.appView.findViewById(R.id.tv_solar_max);
		maxPowerText.setText("(" + String.format("%.0f", Collections.max(tempSolarStamps)) + "W)");
	}

	/**
	 * Get current date & time as string
	 *
	 * @return <code>int[]</code>
	 *            Date and time as integer values
	 *            int[0] = year
	 *            int[1] = month
	 *            int[2] = day
	 */
	public static int[] getCurrentDate() {
		/** Integer array for return values */
		int[] currTime = new int[3];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();

		/** Today's month */
		currTime[1] = cal.get(Calendar.MONTH) + 1;

		/** Today's year */
		currTime[0] = cal.get(Calendar.YEAR);

		/** Today's day */
		currTime[2] = cal.get(Calendar.DATE);

		return currTime;
	}
}
