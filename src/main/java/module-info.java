module com.example.lr1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.lr1 to javafx.fxml;
    exports com.example.lr1;
}