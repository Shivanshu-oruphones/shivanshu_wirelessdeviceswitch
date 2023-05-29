package com.pervacio.wds.custom.externalstorage;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.StatFs;
import android.os.storage.StorageManager;

import com.pervacio.wds.app.DLog;
import com.pervacio.wds.app.EMGlobals;
import com.pervacio.wds.custom.APPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class AppUtils {
    static EMGlobals emGlobals = new EMGlobals();

    public static String getSDCardPath() {
        LinkedHashMap<String, String> mountedPaths = getMountPoints();
        String internalPath = "";
        String usbStoragePath = "";
        String sdCardPath = "";
        boolean fuseFS = false;
        if (mountedPaths.size() > 0) {
            LinkedHashMap<String, String> voldMountPoints = getVoldMountPoints(mountedPaths);
            if (voldMountPoints.size() > 0) {
                Iterator<String> iter = voldMountPoints.keySet().iterator();
                while (iter.hasNext()) {
                    String path = iter.next();
                    String mediaType = voldMountPoints.get(path);
                    if ("SD".equalsIgnoreCase(mediaType) || "SDIO".equalsIgnoreCase(
                            mediaType)/* for samsung M919 - 4.3 */) {
                        sdCardPath = path;
                    } else {
                        usbStoragePath = path;
                        String fsType = mountedPaths.get(path);
                        if ("fuse".equalsIgnoreCase(fsType)) {
                            fuseFS = true;
                        }
                    }
                }
            }
        }
        try {
            StorageManager storageManager = (StorageManager) emGlobals.getmContext().getSystemService(Context.STORAGE_SERVICE);
            Method method = storageManager.getClass().getDeclaredMethod("getVolumeList");
            method.setAccessible(true);
            Object[] volumes = (Object[]) method.invoke(storageManager);
            for (Object object : volumes) {
                Class<? extends Object> svCls = object.getClass();
                boolean removable = (Boolean) getDeclaredFieldValue("mRemovable", svCls, object);
                boolean emulated = (Boolean) getDeclaredFieldValue("mEmulated", svCls, object);
                boolean mounted = true;
                Object state = AppUtils.getDeclaredFieldValue("mState", svCls, object);
                if (state != null && !state.equals("mounted")) {
                    mounted = false;
                }
                String path = AppUtils.getDeclaredFieldValue("mPath", svCls, object).toString();
                if (removable && !emulated && mounted) {
                    if (state == null || state.toString().length() == 0) {
                        state = getVolumeState(emGlobals.getmContext(), path);
                    }
                    if ("mounted".equals(state) || "mounted_ro".equals(state)) {
                        long sdTotal = getMemorySize(path, true);
                        if (sdTotal > 0) {
                            return path;
                        }
                    }
                    if (state == null || Build.MODEL.equals("LG-D686")) {
                        return path;
                    }


                }
            }
        } catch (Exception e) {
            DLog.log("Exception in getSDCardPath(Context) : " + e.getMessage());
        }
        return sdCardPath;
    }

    public static IInterface getSystemServiceInterface(String serviceName) {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            Object binderProxy = getServiceMethod.invoke(null, serviceName);
            Method method = binderProxy.getClass().getDeclaredMethod("getInterfaceDescriptor");
            method.setAccessible(true);
            String interfaceDescriptor = method.invoke(binderProxy).toString();
            Class<?> stubCls = Class.forName(interfaceDescriptor + "$Stub");
            method = stubCls.getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object interfaceProxy = method.invoke(null, binderProxy);
            IInterface iInterface = (IInterface) interfaceProxy;
            return iInterface;
        } catch (Exception e) {
            DLog.log("Exception in getSystemServiceInterface(String): " + e.getMessage());
        }
        return null;
    }

    private static String getVolumeState(Context context, String mountPoint) {
        try {
            IInterface iInterface = getSystemServiceInterface("mMountService");
            Method method = iInterface.getClass().getDeclaredMethod("getVolumeState", Boolean.TYPE);
            method.setAccessible(true);
            String state = (String) method.invoke(mountPoint);
            DLog.log(mountPoint + " state: " + state);

        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return "";
    }


    static LinkedHashMap<String, String> getMountPoints() {
        LinkedHashMap<String, String> mountPoints = new LinkedHashMap<String, String>();
        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (!line.startsWith("/dev/block/vold/")) {
                        line = line.replaceAll("//", "/");
                    }
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] fields = line.split("\\s+");
                        if (fields.length > 2) {
                            String mntPath = fields[1];
                            String fsType = fields[2];
                            DLog.log("mntpath: " + mntPath + " " + fsType);
                            if (!mountPoints.containsKey(mntPath)) {
                                mountPoints.put(mntPath, fsType);
                            }
                        }
                    }
                }
                scanner.close();
            }
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return mountPoints;
    }

    public static LinkedHashMap<String, String> getVoldMountPoints(LinkedHashMap<String, String> mountedPoints) {
        LinkedHashMap<String, String> voldMountPoints = new LinkedHashMap<String, String>();
        try {
            File voldFile = new File("/system/etc/internal_sd.fstab");// for
            // Huawei
            // device(U8686-Prism
            // II)
            if (!voldFile.exists()) {
                voldFile = new File("/system/etc/vold.fstab.nand");// ALCATEL
                // ONE TOUCH
                // Fierce/5020T
            }
            if (!voldFile.exists()) {
                voldFile = new File("/system/etc/vold.fstab");
            }
            if (voldFile.exists()) {
                Scanner scanner;
                scanner = new Scanner(voldFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("dev_mount")) {
                        String[] fields = line.split("\\s+");
                        if (fields.length > 4) {
                            String mntPath = fields[2];
                            if (mntPath.contains(":")) {
                                mntPath = mntPath.substring(0, mntPath.indexOf(":"));
                            }
                            if (!mountedPoints.containsKey(mntPath)) {
                                try {
                                    File file = new File(mntPath);
                                    if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
                                        mntPath = file.getCanonicalPath();
                                    }
                                } catch (Exception e) {
                                    DLog.log("Exception : " + e.getMessage());
                                }
                            }
                            if (mountedPoints.containsKey(mntPath)) {
                                String mediaType = "";
                                for (int i = 4; i < fields.length; i++) {
                                    if (fields[i].contains("mmc_host")) {
                                        String type = getMediaPathType("/sys/" + fields[i]);
                                        if (type != null && type.length() > 0) {
                                            mediaType = type;
                                            break;
                                        }
                                    }
                                }
                                if (voldMountPoints.containsKey(mntPath)) {
                                    String type = voldMountPoints.get(mntPath);
                                    if (!"SD".equalsIgnoreCase(type) && mediaType.length() > 0) {
                                        voldMountPoints.put(mntPath, mediaType);
                                    }
                                } else {
                                    voldMountPoints.put(mntPath, mediaType);
                                }
                                DLog.log("vold path: " + mntPath + " " + mediaType);
                            }
                        }
                    }
                }
                scanner.close();
            } else {
                voldFile = new File("/system/etc/vold.conf");
                if (voldFile.exists()) {
                    Scanner scanner = new Scanner(voldFile);
                    boolean structStarted = false;
                    String mntPath = "";
                    String mediaPath = "";
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        DLog.log("[" + line + "]");
                        line = line.trim();
                        if (structStarted) {
                            if (line.endsWith("}")) {
                                if (mntPath.length() > 0 && mediaPath.length() > 0
                                        && mountedPoints.containsKey(mntPath)) {
                                    String mediaType = "";
                                    String type = getMediaPathType("/sys/" + mediaPath);
                                    if (type != null && type.length() > 0) {
                                        mediaType = type;
                                    }
                                    voldMountPoints.put(mntPath, mediaType);
                                    DLog.log("vold path: " + mntPath + " " + mediaType);
                                }
                                structStarted = false;
                                mntPath = "";
                                mediaPath = "";
                            } else if (line.startsWith("media_path") || line.startsWith("mount_point")) {
                                String[] fields = line.split("\\s+");
                                if (fields.length > 1) {
                                    if (fields[0].equals("media_path")) {
                                        mediaPath = fields[1];
                                        DLog.log("mediaPath: " + mediaPath);
                                    } else {
                                        mntPath = fields[1];
                                        DLog.log("mntPath: " + mntPath);
                                    }
                                }
                            }
                        } else if (line.startsWith("volume_") && line.endsWith("{")) {
                            structStarted = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return voldMountPoints;
    }

    static String getMediaPathType(String mediaPath) {
        String type = "";
        try {
            if (mediaPath.endsWith("mmc_host")) {
                try {
                    File mmcHost = new File(mediaPath);
                    if (mmcHost.exists() && mmcHost.isDirectory()) {
                        File[] mmcFolders = mmcHost.listFiles();
                        String mmcSubFolder = "";
                        int mmcSubfoldersCount = 0;
                        for (int i = 0; i < mmcFolders.length; i++) {
                            String subFolderName = mmcFolders[i].getName();
                            if (mmcFolders[i].isDirectory() && subFolderName.startsWith("mmc")) {
                                mmcSubFolder = subFolderName;
                                mmcSubfoldersCount++;
                            }
                        }
                        if (mmcSubfoldersCount == 1) {
                            mediaPath += "/" + mmcSubFolder;
                        }
                    }
                } catch (Exception ex) {
                    DLog.log("Exception : " + ex.getMessage());
                }
            }
            File mmc = new File(mediaPath);
            if (!mmc.exists()) {
                int lastSlash = mediaPath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    mediaPath = mediaPath.substring(0, lastSlash);
                }
                mmc = new File(mediaPath);
            }
            if (mmc.isDirectory()) {
                File[] subMmcs = mmc.listFiles();
                for (int j = 0; j < subMmcs.length; j++) {
                    String subMmcName = subMmcs[j].getName();
                    if (subMmcName.startsWith("mmc")) {
                        String typeFileName = mediaPath + "/" + subMmcName + "/" + "type";
                        File typeFile = new File(typeFileName);
                        if (typeFile.exists()) {
                            StringBuilder sb = getFileContents(typeFile);
                            type = sb.toString().trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return type;
    }

    static StringBuilder getFileContents(File typeFile) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(typeFile));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return sb;
    }

    public static Object getDeclaredFieldValue(String fieldName, Class<?> cls, Object receiver) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object object = field.get(receiver);
            return object;
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return null;
    }

    public static long getMemorySize(String pathName, boolean total) {
        try {
            File path = new File(pathName);
            StatFs stat = new StatFs(path.getPath());
            long blockSize;
            if (hasJellybeanMR2()) {
                blockSize = stat.getBlockSizeLong();
            } else {
                blockSize = stat.getBlockSize();
            }
            if (total) {
                if (hasJellybeanMR2()) {
                    return stat.getBlockCountLong() * blockSize / 1024L;
                } else {
                    return stat.getBlockCount() * blockSize / 1024L;
                }
            } else {
                if (hasJellybeanMR2()) {
                    return stat.getAvailableBlocksLong() * blockSize / 1024L;
                } else {
                    return stat.getAvailableBlocks() * blockSize / 1024L;
                }
            }
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return 0;
    }

    public static boolean isSDCardPresent() {
        String sdCardPath = getSDCardPath();
        if (sdCardPath.length() == 0) {
            return false;
        }
        long sdCardSize = getMemorySize(sdCardPath, true);

        if (sdCardSize == 0) {
            return false;
        }

        return true;
    }

    /**
     * This method will
     *
     * @return String Path - Returns path if sdcard is adopted as internal storage or else retuns
     * empty
     */
    public static String getExtendedMemoryPath() {
        String mntPath = "";
        String fsType = "";
        String path;
        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    DLog.log("getMountPoints: " + line);
                    if (!line.startsWith("/dev/block/dm")) {
                        line = line.replaceAll("//", "/");
                    }
                    if (line.startsWith("/dev/block/dm")) {
                        String[] fields = line.split("\\s+");
                        if (fields.length > 2) {
                            path = fields[1];
                            fsType = fields[2];
                            if (path.contains("mnt/expand")) {
                                mntPath = path;
                            }
                        }
                    }
                }
                scanner.close();
            }
        } catch (Exception e) {
            DLog.log("Exception : " + e.getMessage());
        }
        return mntPath;
    }

    private static boolean hasJellybeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
}
