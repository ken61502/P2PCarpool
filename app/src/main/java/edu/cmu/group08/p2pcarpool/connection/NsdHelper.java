/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.group08.p2pcarpool.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NsdHelper {
    enum Status {STARTED, STOPPED, REGISTERED, UNREGISTERED};

    public static final String DISCOVER_HEADER = "CP:";
    private static final String SYSTEM_SENDER = "System Message";
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "NsdHelper";
    public String mServiceName = "";

    private int mGroupId = 0;

    private Context mContext;
    private Handler mHandler;
    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private SharedPreferences mSettings;
    private Status mDiscoveryStatus = Status.STOPPED;
    private Status mRegistryStatus = Status.UNREGISTERED;

    NsdServiceInfo mService;
    private HashMap<Integer, NsdServiceInfo> mServiceCandidate = new HashMap<>();

    public NsdHelper(Context context, Handler handler, String serviceName) {
        mContext = context;
        mHandler = handler;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        String service_info;
        mSettings = mContext.getSharedPreferences(Settings.PROFILE_NAME, 0);
        service_info = serviceName                                   + "#"
                + mSettings.getString("price_per_passenger", "$ 10") + "#"
//                + mSettings.getString("car", "BMW 428")              + "#"
                + mSettings.getString("max_passengers", "4");
        mServiceName = service_info;
        Log.d(TAG, mServiceName);
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        //mNsdManager.init(mContext.getMainLooper(), this);

    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
//                } else if (service.getServiceName().equals(DISCOVER_HEADER + mServiceName)) {
//                    Log.d(TAG, "Same machine: " + DISCOVER_HEADER + mServiceName);
                } else if (service.getServiceName().startsWith(DISCOVER_HEADER)){
                    Log.d(TAG, "service.getServiceName()=" + service.getServiceName());
                    addCandidate(service);
//                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                removeCandidate(service);
                if (mService == service) {
                    mService = null;
                }
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
//                stopDiscovery();
//                discoverServices();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(DISCOVER_HEADER + mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                Log.e(TAG, "IP" + mService.getHost().toString());
                Log.e(TAG, "PORT" + Integer.toString(mService.getPort()));
//                sendHandlerMessage("error", mService.getHost().toString()+":"+Integer.toString(mService.getPort()), -1, false, SYSTEM_SENDER);
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                Log.d(TAG, "Service Registered");
                mServiceName = NsdServiceInfo.getServiceName().substring(DISCOVER_HEADER.length());
            }
            
            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }
            
            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }
            
        };
    }

    public void selectGroup(int id) {
        mNsdManager.resolveService(mServiceCandidate.get(id), mResolveListener);
    }

    public void addCandidate(NsdServiceInfo service) {
        mServiceCandidate.put(mGroupId, service);
        String service_info = service.getServiceName();
        service_info = service_info.replaceAll("\\([0-9]+\\)", "").trim();
        sendHandlerMessage("add", service_info, mGroupId,false,SYSTEM_SENDER);
        mGroupId++;
    }

    public void removeCandidate(NsdServiceInfo service) {
        int groupId = -1;

        Iterator it = mServiceCandidate.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, NsdServiceInfo> entry = (Map.Entry) it.next();
            if (service.getServiceName().equals(entry.getValue().getServiceName())) {
                groupId = entry.getKey();
                it.remove();
                break;
            }
        }
        if (groupId > -1) {
            sendHandlerMessage("remove", service.getServiceName() + " removed", groupId,false,SYSTEM_SENDER);
        }
    }

    public synchronized void registerService(int port) {
        if (mRegistryStatus == Status.UNREGISTERED && mNsdManager != null) {
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setPort(port);
            serviceInfo.setServiceName(DISCOVER_HEADER + mServiceName);
            serviceInfo.setServiceType(SERVICE_TYPE);

            mNsdManager.registerService(
                    serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

            mRegistryStatus = Status.REGISTERED;
        }
    }

    public synchronized void tearDown() {
        if (mRegistryStatus == Status.REGISTERED && mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mRegistryStatus = Status.UNREGISTERED;
        }
    }

    public synchronized void discoverServices() {
        if (mDiscoveryStatus == Status.STOPPED && mNsdManager != null) {
            sendHandlerMessage("clear_all", null, -1, false, SYSTEM_SENDER);
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            mDiscoveryStatus = Status.STARTED;
        }
    }
    
    public synchronized void stopDiscovery() {
        if (mDiscoveryStatus == Status.STARTED && mNsdManager != null) {
            sendHandlerMessage("clear_all", null, -1, false, SYSTEM_SENDER);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mDiscoveryStatus = Status.STOPPED;
        }
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }


    public synchronized void sendHandlerMessage(String op, String msg, int id, boolean self, String sender) {
        Bundle messageBundle = new Bundle();
        messageBundle.putString("op", op);
        messageBundle.putString("msg", msg);
        messageBundle.putInt("id", id);
        messageBundle.putString("sender", sender);
        messageBundle.putBoolean("self", self);
        Message message = new Message();
        message.setData(messageBundle);
        mHandler.sendMessage(message);
    }


}
