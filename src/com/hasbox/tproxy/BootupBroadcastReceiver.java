package com.hasbox.tproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

public class BootupBroadcastReceiver extends BroadcastReceiver {
    
    private static final String LOGTAG = BootupBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context cxt = context;
        String version = Main.getVersion(context.getApplicationContext());
        Log.i(LOGTAG, "tproxy boot version: "+version);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String basedir = cxt.getApplicationContext().getFilesDir().getAbsolutePath();
                File redsocksConfFile = new File(basedir, "redsocks.conf");
                if (redsocksConfFile.exists()) {
                    Log.i(LOGTAG, "found redsocks conf, starting proxy and setting iptables");
                    String arch = System.getProperty("os.arch");
                    Main.startRedsocks(arch, basedir);
                    Main.applyIptables(basedir);
                } else {
                    Log.i(LOGTAG, "No redsocks conf, nothing to do");
                }
            }
        });
        t.start();
    }
}
