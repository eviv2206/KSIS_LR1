package com.example.lr1;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    @FXML
    private ListView<String> destList;

    @FXML
    private Button executeBtn;

    @FXML
    private HBox hBox;


    @FXML
    private ListView<String> hostList;

    @FXML
    private ListView<String> ipList;

    @FXML
    private TextField ipText;

    @FXML
    private ListView<String> macList;

    @FXML
    private ListView<String> vendorList;


    @FXML
    private TextField maskText;

    List<String> list = new ArrayList<>();

    @FXML
    void execute(ActionEvent event) {
        list.clear();
        final String ip = ipText.getText();
        ipList.getItems().clear();
        macList.getItems().clear();
        vendorList.getItems().clear();
        hostList.getItems().clear();
        destList.getItems().clear();
        final String subnetMask = maskText.getText();
        if (!ip.equals("") && !subnetMask.equals("") && isValidSubnet(subnetMask) && isValidSubnet(ip)) {
            try {
                InetAddress inetSubnet = InetAddress.getByName(ip);

                List<String> ipAddresses = generateIpAddresses(ip, subnetMask);

                List<String> list1 = Tracert("8.8.8.8");

                ExecutorService executor = Executors.newFixedThreadPool(3000);
                for (String address : ipAddresses) {
                    executor.submit(() -> {
                        InetAddress inetAddress = null;
                        try {
                            inetAddress = InetAddress.getByName(address);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(inetAddress.getHostAddress());
                        try {
                            if (inetAddress.isReachable(5000)) {
                                System.out.println("Host " + inetAddress.getHostAddress() + " is reachable");
                                System.out.println("Host name: " + inetAddress.getHostName());
                                String mac = getMacAddress(inetAddress, inetAddress.getHostAddress());
                                String vendor = getVendorFromMac(inetAddress, mac);
                                String type = getDeviceType(inetAddress.getHostAddress(), list1);
                                if (mac != null) {
                                    System.out.println("MAC address: " + mac);
                                }
                                InetAddress finalInetAddress = inetAddress;
                                Platform.runLater(() -> {
                                    ipList.getItems().add(finalInetAddress.getHostAddress());
                                    hostList.getItems().add(finalInetAddress.getHostName());
                                    macList.getItems().add(mac);
                                    vendorList.getItems().add(vendor);
                                    destList.getItems().add(type);
                                });
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMacAddress(InetAddress address, String ip) {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                } else {
                    System.out.println("Mac address not found");
                }
            } else {
                return getMacAddressFromARP(ip);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMacAddressFromARP(String ip) {
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        if (os.contains("win")) {
            command = "arp -a " + ip;
        } else {
            command = "arp " + ip;
        }
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line.contains(ip)) {
                String[] parts = line.split(" ");
                for (String part : parts) {
                    if (isValidMacAddress(part)) {
                        return part;
                    }
                }
            }
        }
        return "unknown";
    }

    private static boolean isValidMacAddress(String mac) {
        return mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    public List<String> Tracert(String ip) {
        String tracert = "tracert";
        // overtime time
        List<String> ist1 = new ArrayList<>();
        tracert = tracert + " -d ";

        tracert = tracert + " " + ip;
        System.out.println("Executive command:" + tracert);
        try {
            command(tracert);
            list.remove(0);
            list.remove(list.size() - 1);

            for (String s : list) {
                System.out.println(s);
                if (!s.contains("Tracing") && !s.contains("over") && (s.length() > 0) && !s.contains("Trace") && !s.contains("target_name") && !s.contains("Usage:") && !s.contains("Options:")) {
                    System.out.println(s.substring(32));
                    ist1.add(s.substring(32).trim());
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return ist1;
    }

    private void command(String tracerCommand) throws IOException {
        Process process = Runtime.getRuntime().exec(tracerCommand);
        readResult(process.getInputStream());
        process.destroy();
    }

    private void readResult(InputStream inputStream) throws IOException {
        String commandInfo = null;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((commandInfo = bufferedReader.readLine()) != null) {
            list.add(commandInfo);
        }
        bufferedReader.close();
    }

    private static String getVendorFromMac(InetAddress ia, String macAddress) {
        String vendor = "";
        try {
            NetworkInterface nf = NetworkInterface.getByInetAddress(ia);
            if (nf != null) {
                vendor = nf.getDisplayName();
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        if (Objects.equals(vendor, "")) {
            try {
                String apiUrl = "https://api.macvendors.com/" + macAddress;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    vendor = response.toString();
                } else {
                    System.out.println("Error retrieving vendor information. Response code: " + responseCode);
                }
            } catch (Exception e) {
                System.out.println("Exception occurred: " + e.getMessage());
            }
        }
        if (Objects.equals(vendor, "")) {
            return "Not recognized";
        } else {
            return vendor;
        }
    }

    public String getDeviceType(String ip, List<String> list1){
        if (list1.contains(ip)){
            return "Network device";
        } else {
            return "End device";
        }
    }

    public static boolean isValidSubnet(String subnetMask) {
        if (subnetMask == null || subnetMask.isEmpty()) {
            return false;
        }
        String[] octets = subnetMask.split("\\.");
        if (octets.length != 4) {
            return false;
        }
        int[] mask = new int[4];
        try {
            for (int i = 0; i < octets.length; i++) {
                mask[i] = Integer.parseInt(octets[i]);
                if (mask[i] < 0 || mask[i] > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        String reconstructedMask = String.join(".", Arrays.stream(mask).mapToObj(Integer::toString).toArray(String[]::new));
        return subnetMask.equals(reconstructedMask);
    }

    @FXML
    void onChange(KeyEvent event) {
        maskText.clear();
        if (isValidSubnet(ipText.getText())){
            InetAddress ia = null;
            try {
                ia = InetAddress.getByName(ipText.getText());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            NetworkInterface ni = null;
            try {
                ni = NetworkInterface.getByInetAddress(ia);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            maskText.setText(prefixToSubnetMask(ni.getInterfaceAddresses().get(0).getNetworkPrefixLength()));
        }
    }

    private static String prefixToSubnetMask(int prefixLength) {
        int value = 0xffffffff << (32 - prefixLength);
        return String.format("%d.%d.%d.%d",
                (value >> 24) & 0xff,
                (value >> 16) & 0xff,
                (value >> 8) & 0xff,
                value & 0xff);
    }

    public static List<String> generateIpAddresses(String ipAddress, String subnetMask) throws UnknownHostException {
        List<String> ipAddresses = new ArrayList<>();
        int[] ipAddressParts = convertIpAddressToArray(ipAddress);
        int[] subnetMaskParts = convertIpAddressToArray(subnetMask);
        int[] networkAddressParts = new int[4];
        int[] broadcastAddressParts = new int[4];

        for (int i = 0; i < 4; i++) {
            networkAddressParts[i] = ipAddressParts[i] & subnetMaskParts[i];
            broadcastAddressParts[i] = networkAddressParts[i] | (~subnetMaskParts[i] & 0xff);
        }

        int[] currentIpAddress = networkAddressParts.clone();
        while (!convertArrayToIpAddress(currentIpAddress).equals(convertArrayToIpAddress(broadcastAddressParts))) {
            ipAddresses.add(convertArrayToIpAddress(currentIpAddress));
            incrementIpAddress(currentIpAddress);
        }
        ipAddresses.add(convertArrayToIpAddress(broadcastAddressParts));
        return ipAddresses;
    }

    public static int[] convertIpAddressToArray(String ipAddress) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(ipAddress.trim());
        byte[] bytes = inetAddress.getAddress();
        int[] parts = new int[4];
        for (int i = 0; i < 4; i++) {
            parts[i] = bytes[i] & 0xff;
        }
        return parts;
    }

    public static String convertArrayToIpAddress(int[] parts) throws UnknownHostException {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) parts[i];
        }
        InetAddress inetAddress = InetAddress.getByAddress(bytes);
        return inetAddress.getHostAddress();
    }

    public static void incrementIpAddress(int[] ipAddressParts) {
        for (int i = 3; i >= 0; i--) {
            if (ipAddressParts[i] < 255) {
                ipAddressParts[i]++;
                return;
            }
            ipAddressParts[i] = 0;
        }
    }



}