package de.jasanex.pro.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import de.jasanex.pro.model.*;
import de.jasanex.pro.service.AuthService.LoginResult;

/** Opt-in, benötigt eine Windows-Desktopsitzung. Testdaten bleiben im Arbeitsspeicher. */
@EnabledIfSystemProperty(named="jasanex.uiTest", matches="true")
class ViewTest {
    @Test void loadsBothViewsAndReturnsToLogin() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown); assertTrue(ready.await(15, TimeUnit.SECONDS));
        FutureTask<Void> check = new FutureTask<>(() -> {
            Main app = new Main(); Stage stage = new Stage();
            try {
                app.start(stage);
                stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
                assertNotNull(stage.getScene().lookup("#password"));
                ((Button) stage.getScene().lookup("#loginButton")).fire();
                assertFalse(((Label) stage.getScene().lookup("#error")).getText().isBlank());
                snapshot(stage, "login.png");
                Mitarbeiter user = new Mitarbeiter(999999, 1, "QA", "Admin", "TEST");
                var showDashboard = Main.class.getDeclaredMethod("showDashboard", LoginResult.class);
                showDashboard.setAccessible(true);
                showDashboard.invoke(app, new LoginResult(user, Role.ADMIN, "Testrestaurant · UI-Prüfung", Map.of(user.id(),user), List.of()));
                var controllerField=Main.class.getDeclaredField("dashboardController");controllerField.setAccessible(true);
                var controller=(de.jasanex.pro.controller.DashboardController)controllerField.get(app);
                // UI-Test ohne Schreibzugriff: ausschließlich flüchtige Ansichts-Testdaten.
                var shifts=List.of(new de.jasanex.pro.model.Betriebsdaten.Schicht(1,user.id(),"QA","Admin","Head",java.time.LocalDateTime.of(2026,10,30,12,0),null,"",null,""),
                    new de.jasanex.pro.model.Betriebsdaten.Schicht(2,2,"KT","Mitarbeiter","Küche",java.time.LocalDateTime.of(2026,10,30,12,0),null,"",null,""));
                controller.render(new de.jasanex.pro.model.Betriebsdaten.Snapshot(Map.of(user.id(),user),List.of(),
                    List.of(new de.jasanex.pro.model.Betriebsdaten.Personal(user.id(),"QA","Admin","AKTIV",java.math.BigDecimal.ZERO)),List.of(),List.of(),shifts));
                var busy=controller.getClass().getDeclaredMethod("setBusy",boolean.class);busy.setAccessible(true);busy.invoke(controller,false);
                stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
                assertEquals("QA", ((Label) stage.getScene().lookup("#username")).getText());
                assertEquals("ADMIN", ((Label) stage.getScene().lookup("#role")).getText());
                assertEquals(5,((javafx.scene.layout.VBox)stage.getScene().lookup("#categories")).getChildren().size());
                snapshot(stage, "dashboard.png");
                ((Button)stage.getScene().lookup("#masterButton")).fire();
                stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
                assertTrue(stage.getScene().lookup("#master").isVisible());
                assertEquals(1,((TableView<?>)stage.getScene().lookup("#personal")).getItems().size());
                snapshot(stage,"stammdaten.png");
                // Modalformular öffnen, visuell erfassen und ohne Speichern schließen.
                Platform.runLater(() -> {
                    for (javafx.stage.Window window : List.copyOf(javafx.stage.Window.getWindows())) {
                        if (window instanceof Stage popup && popup != stage && popup.getScene()!=null
                                && popup.getScene().getRoot() instanceof DialogPane pane) {
                            try { popup.getScene().getRoot().applyCss();popup.getScene().getRoot().layout();snapshot(popup,"mitarbeiter-formular.png"); }
                            catch(Exception e){throw new RuntimeException(e);}
                            ((Button)pane.lookupButton(ButtonType.CANCEL)).fire();
                        }
                    }
                });
                Button create=stage.getScene().getRoot().lookupAll(".button").stream().filter(n -> n instanceof Button b && b.getText().equals("+ Neu anlegen")).map(n -> (Button)n).findFirst().orElseThrow();
                create.fire();
                Button logout = stage.getScene().getRoot().lookupAll(".button").stream().filter(n -> n instanceof Button b && b.getText().equals("Abmelden")).map(n -> (Button)n).findFirst().orElseThrow();
                logout.fire();
                assertNotNull(stage.getScene().lookup("#password"));
                assertTrue(((PasswordField) stage.getScene().lookup("#password")).getText().isEmpty());
            } finally { app.stop(); stage.close(); }
            return null;
        });
        Platform.runLater(check);
        try { check.get(30, TimeUnit.SECONDS); } finally { Platform.exit(); }
    }
    private void snapshot(Stage stage, String name) throws Exception {
        WritableImage image = stage.getScene().snapshot(null);
        BufferedImage out = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<out.getHeight(); y++) for (int x=0; x<out.getWidth(); x++) out.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        Path folder = Path.of("target", "ui-check"); Files.createDirectories(folder);
        ImageIO.write(out, "png", folder.resolve(name).toFile());
    }
}
