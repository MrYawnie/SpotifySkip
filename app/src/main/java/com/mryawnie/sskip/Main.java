package com.mryawnie.sskip;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

/**
 * Created by MrYawnie on 30.6.2016.
 */
public class Main implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam lpparam) throws Throwable {
        // Enables unlimited skips
        if (!lpparam.packageName.equals("com.spotify.music"))
            return;

        lpparam.res.setReplacement("com.spotify.music", "bool", "is_tablet", true);


    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam packageParam ) throws Throwable {
        if (packageParam.packageName.equalsIgnoreCase("com.spotify.music")) {
            XposedBridge.log("We are in Spotify!");
            //find LoadedFlags class
            //Class<?> loadedFlagsCls = XposedHelpers.findClass("com.spotify.mobile.android.service.feature.LoadedFlags", packageParam.classLoader); //versions <= 5.8
            Class<?> loadedFlagsCls = XposedHelpers.findClass("com.spotify.android.flags.LoadedFlags", packageParam.classLoader); // versions => 5.9
            //iterate through all methods to find our flag class
            Class<?> flagCls = null;
            for (Method m : loadedFlagsCls.getMethods()) {
                if (m.getName().equalsIgnoreCase("a") && m.getParameterTypes().length == 1) {
                    flagCls = m.getParameterTypes()[0];//actually it's just a lucky guess
                }
            }
            //failsafe check and nice info
            if (flagCls == null) {
                XposedBridge.log("Flag class not found!");
                return;
            }
            //hook this method so we can modify flag state
            XposedHelpers.findAndHookMethod(loadedFlagsCls, "a", flagCls, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //get flag name
                    String flag = (String) XposedHelpers.getObjectField(param.args[0], "b");
                    //disable chosen flags
                    if (flag.equalsIgnoreCase("ads") || flag.equalsIgnoreCase("shuffle_restricted")) {
                        param.setResult(false);
                    }
                    if (flag.equalsIgnoreCase("on-demand")/* || flag.equalsIgnoreCase("high-bitrate")*/){
                        param.setResult(true);
                    }
                }
            });
        }
    }
}

