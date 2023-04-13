package com.example.lr1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 706, 400);
        stage.setTitle("LAN Scanner");
        stage.setFullScreen(false);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        if (!isConfigValid()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText("Обнаружено нелегальное использование программы");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch();
    }

    private boolean isConfigValid() {
        String expectedHash = "F6092657599A0846C190E06F8151A718E9854AA745AB74B3B84DEA786360BECA"; // заменить на свою версию

        // Получение версии Windows
        String windowsVersion = System.getProperty("os.version");

        String motherboardSerial = null;
        try {
            Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            Scanner scanner = new Scanner(process.getInputStream());
            scanner.next();
            motherboardSerial = scanner.next();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Преобразование данных в хеш-сумму
        String input = windowsVersion + motherboardSerial;
        String hash = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02X", b));
            }
            hash = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Сверка хеш-суммы с ожидаемой версией
        return hash.equals(expectedHash);
    }
}