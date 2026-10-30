package de.jasanex.pro.controller;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.service.AuthService;
import de.jasanex.pro.service.AuthService.LoginResult;

public final class LoginController {
    @FunctionalInterface public interface LoginHandler { void accept(LoginResult result) throws Exception; }
    @FXML private TextField kuerzel;
    @FXML private PasswordField password;
    @FXML private Button loginButton;
    @FXML private Label error;
    @FXML private ProgressIndicator progress;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "jasanex-login"); thread.setDaemon(true); return thread;
    });
    private LoginHandler handler;
    private Task<LoginResult> active;
    private boolean disposed;
    public void configure(LoginHandler handler) { this.handler = handler; }
    @FXML private void login() {
        if (active != null) return;
        error.setText("");
        if (kuerzel.getText().isBlank() || password.getText().isEmpty()) {
            error.setText("Bitte Mitarbeiterkürzel und Passwort eingeben."); return;
        }
        String username = kuerzel.getText().trim();
        char[] secret = password.getText().toCharArray(); password.clear();
        busy(true);
        Task<LoginResult> task = new Task<>() {
            @Override protected LoginResult call() throws Exception {
                try { return new AuthService(DatabaseConfig.load()).login(username, secret); }
                finally { Arrays.fill(secret, '\0'); }
            }
        };
        active = task;
        task.setOnSucceeded(event -> {
            active = null; busy(false);
            if (disposed) return;
            try { handler.accept(task.getValue()); }
            catch (Exception e) { error.setText("Die Dashboardansicht konnte nicht geöffnet werden."); }
        });
        task.setOnFailed(event -> {
            active = null; busy(false);
            if (disposed) return;
            Throwable failure = task.getException();
            error.setText(failure instanceof AuthService.LoginException || failure instanceof IllegalStateException
                    ? failure.getMessage() : "Daten konnten nicht geladen werden. Bitte XAMPP und lokale Konfiguration prüfen.");
        });
        executor.submit(task);
    }
    private void busy(boolean value) {
        kuerzel.setDisable(value); password.setDisable(value); loginButton.setDisable(value);
        progress.setVisible(value); progress.setManaged(value);
        loginButton.setText(value ? "Anmeldung wird geprüft …" : "Anmelden  →");
    }
    public void dispose() { disposed = true; password.clear(); executor.shutdown(); }
}
