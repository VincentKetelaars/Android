package nl.vincentketelaars.wiebetaaltwat.other;

import android.content.Context;
import android.widget.Toast;


public class Resources {
	// Private file
	public final static String privateFile = "my_private_file_v6"; // my_private_file is already used, use version number
	// SharedPreferences
	public final static String inlogFile = "Inlog";
	// WBW Url
	public final static String WBWUrl = "https://www.wiebetaaltwat.nl";
	// Password of bks key
	public final static String bksPassword = "pass1234";
	public final static String bksFile = "res/raw/mynewkey.bks";
	
	public static void showToast(Context context, String text, int position, int duration) {
		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(position, 0, 0);
		toast.show();
	}

}
