package org.allin.enq.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by santiagocarullo on 10/25/15.
 */
public class NetworkUtils {

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifiManager.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }

    public static  String getDeviceIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    public static InetAddress getBroadcastAddress2(Context context) throws UnknownHostException, SocketException {

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback())
                continue;    // Don't want to broadcast to the loopback interface
            for (InterfaceAddress interfaceAddress :
                    networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null)
                    continue;

                return broadcast;
            }
        }

        return null;
    }

    public static void sendBroadcastMessage(Context context, String message, Integer port) throws IOException {
        DatagramSocket broadcastSocket = new DatagramSocket();
        InetAddress broadcastAddress = NetworkUtils.getBroadcastAddress2(context);
        DatagramPacket packet = new DatagramPacket(message.getBytes(),message.getBytes().length, broadcastAddress, port);
        broadcastSocket.send(packet);
        broadcastSocket.close();
    }

    public static String receiveSingleTCPMessage(Integer port, Integer timeout) throws IOException {

        ServerSocket serverSocket = null;

        try {
             serverSocket = new ServerSocket(port);
             String response = null;
             serverSocket.setSoTimeout(timeout);
             Socket socket = serverSocket.accept();
             BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             while (response == null) response = socketReader.readLine();
            serverSocket.close();
             socket.close();
             return response;
        } catch (IOException e) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    throw e;
                } catch (IOException e1) {
                    throw e1;
                }
            }
        }

        return null;
    }
}
