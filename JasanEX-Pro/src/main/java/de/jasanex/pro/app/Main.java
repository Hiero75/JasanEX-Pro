package de.jasanex.pro.app;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import de.jasanex.pro.cache.StammdatenCache;
import de.jasanex.pro.controller.*;
import de.jasanex.pro.security.SessionManager;
import de.jasanex.pro.service.AuthService.LoginResult;

public final class Main extends Application {
    private Stage stage;
    private final SessionManager session = new SessionManager();
    private final StammdatenCache cache = new StammdatenCache();
    private LoginController loginController;
    private DashboardController dashboardController;
    @Override public void start(Stage stage) throws IOException {
        this.stage = stage;
        stage.setTitle("JasanEX Pro"); stage.setMinWidth(900); stage.setMinHeight(650);
        showLogin(); stage.show();
    }
    private FXMLLoader loader(String name) {
        return new FXMLLoader(Main.class.getResource("/de/jasanex/pro/view/" + name + ".fxml"));
    }
    private void show(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1180, 780);
            scene.getStylesheets().add(Main.class.getResource("/de/jasanex/pro/css/style.css").toExternalForm());
            stage.setScene(scene);
        } else stage.getScene().setRoot(root);
    }
    public void showLogin() throws IOException {
        if (dashboardController != null) { dashboardController.dispose(); dashboardController = null; }
        session.clear(); cache.clear();
        FXMLLoader loader = loader("login"); Parent root = loader.load();
        loginController = loader.getController(); loginController.configure(this::showDashboard);
        show(root);
    }
    private void showDashboard(LoginResult result) throws IOException {
        FXMLLoader loader = loader("dashboard"); Parent root = loader.load();
        session.start(result); cache.replace(result);
        try {
            dashboardController = loader.getController();
            dashboardController.configure(session.current(), cache, this::showLogin);
            show(root); loginController.dispose(); loginController = null;
        } catch (RuntimeException e) { session.clear(); cache.clear(); throw e; }
    }
    @Override public void stop() {
        if (loginController != null) loginController.dispose();
        if (dashboardController != null) dashboardController.dispose();
        session.clear(); cache.clear();
    }
    public static void main(String[] args) { launch(args); }
}
