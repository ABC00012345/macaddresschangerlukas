package com.lukas.macchanger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String MAC_ADDRESS_KEY = "macAddress";
    EditText newmacTextEdit;
	TextView macview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
		
		// show current mac in text view
		macview = findViewById(R.id.macview);
		try {
			Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat /sys/class/net/wlan0/address"});
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuilder output = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				output.append(line);
			}
            macview.setText("Current MAC: " + output);
			try {
				int exitCode = process.waitFor();
				reader.close(); // Close the reader
			} catch (InterruptedException e) {} // Wait for the process to finish
		} catch (IOException e) {}

        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstStart = prefs.getBoolean("isFirstStart", true);
        if (isFirstStart) {
            // Call the function for first app start
            saveMacAddress();

            // Mark the app as not being launched for the first time anymore
            prefs.edit().putBoolean("isFirstStart", false).apply();
        }

        String macAddress = prefs.getString(MAC_ADDRESS_KEY, "");

        if (!macAddress.isEmpty()) {
            TextView macaddrtextview = findViewById(R.id.macaddrtext);
            macaddrtextview.setText("Old Mac since App Start: " + macAddress);
        }

        Button change_btn = findViewById(R.id.changebtn);
        change_btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCommandAvailableWithSu("ifconfig") || !isCommandAvailableWithSu("ip")) {
						showAlert("Can't Change", "Required commands are not available. Need to install BusyBox");
					} else {
						newmacTextEdit = findViewById(R.id.newmacte);
						String mac = newmacTextEdit.getText().toString(); // Convert EditText to String
						changeMac(mac);
					}
				}
			});
			
			
		Button restore_btn = findViewById(R.id.restorebtn);
        restore_btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCommandAvailableWithSu("ifconfig") || !isCommandAvailableWithSu("ip")) {
						showAlert("Can't Change", "Required commands are not available. Need to install BusyBox");
					} else {
					    String mac = prefs.getString(MAC_ADDRESS_KEY, "");
						if (!(mac == "")){
						    changeMac(mac);
						} else {
							Toast.makeText(MainActivity.this, "Failed to restore mac, please restore it manually. You have get warned that the MAC can't get saved!", Toast.LENGTH_LONG);
						}
					}
				}
			});
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.ok, null)
			.show();
    }

    private boolean isCommandAvailableWithSu(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "which " + command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            int exitCode = process.waitFor(); // Wait for the process to finish
            reader.close(); // Close the reader

            return exitCode == 0 && line != null && !line.isEmpty();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void saveMacAddress() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat /sys/class/net/wlan0/address"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            try {
                process.waitFor();
                String macAddress = output.toString();
                prefs.edit().putString(MAC_ADDRESS_KEY, macAddress).apply();
            } catch (InterruptedException e) {
                Toast.makeText(MainActivity.this, "Failed to save MAC Address. !! Before changing, write down your real MAC !!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Failed to save MAC Address. !! Before changing, write down your real MAC !!", Toast.LENGTH_LONG).show();
        }
    }

    private void changeMac(String mac) {
		try {
			Runtime.getRuntime().exec("su -c ip link set down wlan0");
			Thread.sleep(500);
			Runtime.getRuntime().exec("su -c ifconfig wlan0 hw ether " + mac);
			Thread.sleep(500);
			Runtime.getRuntime().exec("su -c ip link set up wlan0");

			macview = findViewById(R.id.macview);
			Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat /sys/class/net/wlan0/address"});
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			StringBuilder output = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				output.append(line);
			}

			int exitCode = process.waitFor(); // Wait for the process to finish
			reader.close(); // Close the reader

			
			if (exitCode == 0) {
				String infos = output.toString();
				showAlert("Mac Changed", "New Mac: \n" + infos + "\nMAC may reset after WIFI reconnect");
				macview.setText("Current MAC: " + infos);
			} else {
				showAlert("Failed", "Failed to change MAC address.");
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
}

