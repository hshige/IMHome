package com.example.imhome;

import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private LocationManager locationManager = null;

	private TextView resText = null;
	private TextView resText2 = null;

	private String stationName = "";
	private String time = "";
	private String nearestStation = "";
	private String mailAddress = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resText2 = (TextView) findViewById(R.id.textView2);
		resText = (TextView) findViewById(R.id.textView3);
		Button button1 = (Button) findViewById(R.id.button1);
		Button button2 = (Button) findViewById(R.id.button2);
		Button button3 = (Button) findViewById(R.id.button3);
		button1.setOnClickListener(mButton1Listener);
		button2.setOnClickListener(mButton2Listener);
		button3.setOnClickListener(mButton3Listener);
	}

	private OnClickListener mButton1Listener = new OnClickListener() {
		public void onClick(View v) {
			// やらないとはまる
			if (locationManager != null) {
				locationManager.removeUpdates(mLocationListener);
			}
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			// 3Gまたはwifiから位置情報を取得する設定
			boolean networkFlg = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			// GPSから位置情報を取得する設定
			boolean gpsFlg = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 1000L, 0,
					mLocationListener);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 1000L, 0, mLocationListener);
		}
	};

	private OnClickListener mButton2Listener = new OnClickListener() {
		public void onClick(View v) {
			String requestURLstation = "http://api.ekispert.com/v1/xml/search/course/light?key=Kbc6cjMTWMQUzCkC&from="
					+ stationName
					+ "&to="
					+ nearestStation
					+ "&limitedExpress=false&bus=false&redirect=true";
			// Toast.makeText(getApplicationContext(), requestURLstation,
			// Toast.LENGTH_LONG).show();
			TaskTransfer tasktransfer = new TaskTransfer();
			tasktransfer.execute(requestURLstation);
		}
	};

	private OnClickListener mButton3Listener = new OnClickListener() {
		public void onClick(View v) {
			Uri uri = Uri.parse("mailto:" + mailAddress);
			Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
			intent.putExtra(Intent.EXTRA_SUBJECT, "今" + stationName + "駅");
			intent.putExtra(Intent.EXTRA_TEXT, time + "時ごろに" + nearestStation
					+ "駅に着く");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	};

	private LocationListener mLocationListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}

		public void onLocationChanged(Location location) {
			String latitude = Double.toString(location.getLatitude());
			String longitude = Double.toString(location.getLongitude());

			String requestURL = "http://map.simpleapi.net/stationapi?y="
					+ latitude + "&x=" + longitude + "&output=json";
			TaskStation taskstation = new TaskStation();
			taskstation.execute(requestURL);

			String message = "";
			message += ("Latitude" + latitude);
			message += "\n";
			message += ("Longitude" + longitude);
			message += "\n";
			message += ("Accuracy" + Float.toString(location.getAccuracy()));
			// Toast.makeText(getApplicationContext(), message,
			// Toast.LENGTH_LONG)
			// .show();
			// 1回しか呼ばない
			locationManager.removeUpdates(mLocationListener);

			// yahooMap(latitude,longitude);
			// getReuest(latitude, longitude);
		}
	};

	@Override
	protected void onPause() {
		if (locationManager != null) {
			locationManager.removeUpdates(mLocationListener);
		}
		super.onPause();
	}

	protected class TaskStation extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(params[0]);
			String rtn = "";
			try {
				HttpResponse response = client.execute(get);
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
					byte[] result = EntityUtils.toByteArray(response
							.getEntity());
					rtn = new String(result, "UTF-8");
				}
			} catch (Exception e) {
			}
			client.getConnectionManager().shutdown();
			return rtn;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				// JSONArrayがエントリのため、これをしないと例外で落ちる
				String jsonBase = "{\"root\":" + result + "}";
				JSONObject json = new JSONObject(jsonBase);
				// Toast.makeText(getApplicationContext(), json.toString(),
				// Toast.LENGTH_LONG).show();
				JSONObject obj = json.getJSONArray("root").getJSONObject(0);
				stationName = obj.getString("name");
				int textLong = stationName.length();
				stationName = stationName.substring(0, textLong - 1);
				resText.setText(stationName);
			} catch (JSONException e) {
				resText.setText("Json Error!!!" + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	protected class TaskTransfer extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... params) {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(params[0]);
			String rtn = "";
			try {
				HttpResponse response = client.execute(get);
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
					rtn = EntityUtils.toString(response.getEntity(), "UTF-8");
					// resText.setText(rtn);
					// byte[] result =
					// EntityUtils.toByteArray(response.getEntity());
					// rtn = EntityUtils.toString(result, "UTF-8");
				}
			} catch (Exception e) {
			}
			client.getConnectionManager().shutdown();
			return rtn;
		}

		@Override
		protected void onPostExecute(String result) {
			int index = result.indexOf("orange_txt\">");
			index += "orange_txt\">".length();

			time = result.substring(index, index + 5);
			resText2.setText(time);
		}
	}
}
