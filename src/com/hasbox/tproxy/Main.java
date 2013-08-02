package com.hasbox.tproxy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hasbox.tproxy.ShellCommand.CommandResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Formatter;

public class Main extends PreferenceActivity {
	public static final String PREFS_NAME = "prefs";
    private static final int REDSOCKS_HTTP_PORT = 8123;
    private static final String LOGTAG = Main.class.getSimpleName();
    private static final String REDSOCK_PID_FILE = "redsocks.pid";
	final int START = 1;
	final int STOP = 2;
	String basedir = null;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    File f = new File("/system/xbin/iptables");
		if (!f.exists()) {
			f = new File("/system/bin/iptables");
			if (!f.exists()) {
				alert("No iptables binary found on your ROM !", this);
			}
		}
		f = new File("/system/xbin/su");
		if (!f.exists()) {
			f = new File("/system/bin/su");
			if (!f.exists()) {
				alert("No su binary found on your ROM !", this);
			}
		}
		try {
		 basedir = getBaseContext().getFilesDir().getAbsolutePath();
		} catch (Exception e) {}

		copyfile("redsocks-armv71");
		copyfile("redsocks-i686");
		copyfile("runproxy.sh");
		copyfile("redirect.sh");

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.mainview);
		Button closeButton = new Button(this);
		closeButton.setText("DONE");
		closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Main.this.finish();
            }
        });
		setListFooter(closeButton);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	
		CheckBoxPreference cb = (CheckBoxPreference) findPreference("isEnabled");

		PackageInfo pi = null;
		try {
        	pi = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {}
		findPreference("version").setSummary("TransProxy "+pi.versionName);
		
		cb.setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange( Preference preference,  Object newValue) {	
                final CheckBoxPreference cbp = (CheckBoxPreference) preference;
                	Boolean ret = proxy((Boolean)newValue ? START : STOP);
            		//setenabled(checklistener());
                	setenabled(checkPIDFile());
					return ret;
			}
		});
		
		//setenabled(checklistener());
		setenabled(checkPIDFile());
	}

	public boolean proxy(int action) {

		if (action == START) { // start proxy
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

			String host = settings.getString("proxyHost", "");
			String port = settings.getString("proxyPort", "");
			Boolean auth = settings.getBoolean("isAuthEnabled", false);
			// need to make sure we escape any slashes in username & passwd fields
			// as redsocks then passes them through sprintf before sending in HTTP header  
			String user = settings.getString("username", "").replace("\\", "\\\\");
			Log.d("TPROXY", "user:"+user);
			String pass = settings.getString("password", "");
			String proxy_type = settings.getString("proxyType", "http");
			
			String ipaddr;
			String arch = System.getProperty("os.arch");
			Log.d("tproxy", "arch:"+arch);

			if (host.trim().equals("")) {
				alert("Hostname/IP is empty", null);
				return false;
			}
			if (port.trim().equals("")) {
				alert("Port is NULL", null);
				return false;
			}
			if (auth) {
				if (user.trim().equals("")) {
					alert("Auth is enabled but username is NULL", null);
					return false;
				}
				if (pass.trim().equals("")) {
					alert("Auth is enabled but password is NULL", null);
					return false;
				}
			}
			try {
				InetAddress addr = InetAddress.getByName(host.trim());
				ipaddr = addr.getHostAddress();
			} catch (UnknownHostException e) {
				alert("Cannot resolve hostname "+host, null); 
				return false;
			}
			Log.v("tproxy","proxy.sh start " + basedir + " "
					+"type=" + proxy_type + " "
					+"host=" + ipaddr + " "
					+"port=" + port.trim() + " "
					+"auth=" + auth + " "
					+"user=" + user.trim() + " "
					+"pass=*****"
					+"arch="+arch);
			
            String preface = "base { \n" +
                    "log_debug = off; \n" +
                    "log_info = off; \n" +
                    "log = stderr; \n" +
                    "daemon = on; \n" +
                    "redirector = iptables; \n" +
                    "} \n";
			
			String template1 = "redsocks { \n"+
			    "local_ip = 127.0.0.1;\n"+
			    "local_port = "+REDSOCKS_HTTP_PORT+";\n"+
			    "ip = %s;\n"+
			    "port = %s;\n"+
			    "type = http-connect;\n"+
			    "login = \"%s\";\n"+
			    "password = \"%s\";\n"+
			   "}\n"; 
			String template2 = "redsocks {\n"+
			    "local_ip = 127.0.0.1;\n"+
			    "local_port = 8124;\n"+
			    "ip = %s;\n"+
			    "port = %s;\n"+
			    "type = http-connect;\n"+
			    "login = \"%s\";\n"+
			    "password = \"%s\";\n"+
			   "}\n";
			
			try {
			    Log.d("TPROXY", "CONF FILE: "+basedir);
			    File f = new File(basedir+"/redsocks.conf");
			    f.createNewFile();
			    Log.d("TPROXY", "EXISTS"+f.exists());
                FileWriter writer = new FileWriter(f);
                String out1 = String.format(template1, ipaddr, port.trim(), user.trim(), pass.trim());
                String out2 = String.format(template2, ipaddr, port.trim(), user.trim(), pass.trim());
                writer.write(preface+out1+out2);
                writer.close();
            } catch (IOException e) {
                Log.e("TPROXY", "", e);
            }
			
			ShellCommand cmd = new ShellCommand();
			//CommandResult r = cmd.sh.runWaitFor(basedir+"/runproxy.sh start " + basedir +" " + arch);
			CommandResult r = cmd.sh.runWaitFor(basedir+"/redsocks-"+arch+" -p "+basedir+"/"+REDSOCK_PID_FILE+" -c "+basedir+"/redsocks.conf" );

			if (!r.success()) {
			    Log.v("tproxy", "Error starting proxy.sh (" + r.stderr + ")");
				cmd.sh.runWaitFor(basedir+"/proxy.sh stop "+ basedir);
				alert ("Failed to stop proxy.sh ("+ r.stderr + ")", null);
			    return false;
			}
			
			//FIXME: HACK, do this properly by having call back once redsocks pid file is detected!
			try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			
			if (checkPIDFile()) {
				 r = cmd.su.runWaitFor(basedir+"/redirect.sh start " + proxy_type);
				 
				 if (!r.success()) {
					    Log.v("tproxy", "Error starting redirect.sh (" + r.stderr +")");
						cmd.sh.runWaitFor(basedir+"/proxy.sh stop "+ basedir);
						alert ("Failed to start redirect.sh ("+ r.stderr + ")", null);
					    return false;
				} else {
					 	Log.v("tproxy", "Successfully ran redirect.sh start " + proxy_type);
						return true;
				}
			} else {
				alert("Proxy failed to start", null);
				return false;
			}
		} else { // stop tproxy
		 	Log.v("tproxy", "Successfully ran redirect.sh stop");
			ShellCommand cmd = new ShellCommand();
			//cmd.sh.runWaitFor(basedir+"/proxy.sh stop "+basedir);
			cmd.sh.runWaitFor("kill `cat "+basedir+"/redsocks.pid`");
			new File(basedir, "redsocks.pid").delete();
			new File(basedir, "redsocks.conf").delete();
			cmd.su.runWaitFor(basedir+"/redirect.sh stop");
			return true;
		}
    }

	public void copyfile(String file) {
		String of = file;
		File f = new File(of);

		if (!f.exists()) {
			try {
				InputStream in = getAssets().open(file);
				FileOutputStream out = getBaseContext().openFileOutput(of, MODE_PRIVATE);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				in.close();
				Runtime.getRuntime().exec("chmod 700 " + basedir + "/" + of);
			} catch (IOException e) {
			}
		}
	}

	public void alert(String msg, Activity a) {

		final Activity act = a;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg).setCancelable(false).setNegativeButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (act != null)
							act.finish();
						else
							dialog.cancel();
					}
				}).show();		
	}
	
// FIXME: needs to run on background Thread !!!	
//	public boolean checklistener() {
//		Socket socket = null;
//		try {
//			socket = new Socket("127.0.0.1", REDSOCKS_HTTP_PORT);
//		} catch (Exception e) {
//		    Log.e(LOGTAG, "could not connect to redsocks http port:"+REDSOCKS_HTTP_PORT, e);
//		}
//
//		if (socket != null && socket.isConnected()) {
//			try {
//				socket.close();
//			} catch (Exception e) {
//			}
//			return true;
//		} else {
//			return false;
//		}
//	}
	
	public boolean checkPIDFile() {
	    File pidfile = new File(basedir, REDSOCK_PID_FILE);
	    return pidfile.exists(); 
	}
	
	public void setenabled(boolean b) {

	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	SharedPreferences.Editor editor = settings.edit();
	Log.v("tproxy","Enabled = "+b);
	
	CheckBoxPreference cb = (CheckBoxPreference) findPreference("isEnabled");

	cb.setChecked(b);
	
	findPreference("username").setEnabled(!b);
	findPreference("password").setEnabled(!b);
	findPreference("isAuthEnabled").setEnabled(!b);
	findPreference("proxyType").setEnabled(!b);
	findPreference("proxyHost").setEnabled(!b);
	findPreference("proxyPort").setEnabled(!b);

	editor.putBoolean("isEnabled", b);
    editor.commit();
	}
}
