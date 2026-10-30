package de.jasanex.pro.app;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import de.jasanex.pro.cache.StammdatenCache;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.controller.*;
import de.jasanex.pro.model.*;
import de.jasanex.pro.model.Betriebsdaten.*;
import de.jasanex.pro.security.SessionManager.Session;
import de.jasanex.pro.service.*;

/** Separat ausführen: echter Dialog + Hintergrundtask + SQL, alle Schreibvorgänge zurückgerollt. */
@EnabledIfSystemProperty(named="jasanex.employeeUiTest",matches="true")
class MitarbeiterAnlegenTest {
    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(20,TimeUnit.SECONDS);
    }
    @Test void createThroughDialogUpdatesDatabaseAndUnlocksTable() throws Exception {
        DatabaseConfig config=DatabaseConfig.load();
        try(Connection connection=config.connect()) {
            connection.setAutoCommit(false);
            int restaurant=config.restaurantId();
            Mitarbeiter admin;
            try(var s=connection.prepareStatement("SELECT mitarbeiter_id,kuerzel FROM mitarbeiter WHERE restaurant_id=? AND rolle=? AND m_status=? LIMIT 1")) {
                s.setInt(1,restaurant);s.setString(2,config.roleValue(Role.ADMIN));s.setString(3,config.activeStatus());
                try(var r=s.executeQuery()){assertTrue(r.next());admin=new Mitarbeiter(r.getInt(1),restaurant,r.getString(2),config.roleValue(Role.ADMIN),config.activeStatus());}
            }
            String code=null;
            for(int i=0;i<100;i++) {
                String candidate=String.format("%02d",i);
                try(var s=connection.prepareStatement("SELECT 1 FROM mitarbeiter WHERE restaurant_id=? AND kuerzel=?")){
                    s.setInt(1,restaurant);s.setString(2,candidate);try(var r=s.executeQuery()){if(!r.next()){code=candidate;break;}}
                }
            }
            assertNotNull(code);String selectedCode=code;String secret=UUID.randomUUID().toString();
            Session session=new Session(admin,Role.ADMIN,"UI-Regressionstest");
            BetriebService service=new BetriebService(config,session,()->(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
                if(Set.of("close","commit","setAutoCommit","setTransactionIsolation").contains(m.getName()))return null;
                try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}
            }),Clock.system(Betriebsregeln.ZEITZONE));
            CountDownLatch started=new CountDownLatch(1);Platform.startup(started::countDown);assertTrue(started.await(10,TimeUnit.SECONDS));
            Platform.setImplicitExit(false);
            AtomicReference<DashboardController> controller=new AtomicReference<>();AtomicReference<Stage> stage=new AtomicReference<>();
            try {
                fx(()->{
                    FXMLLoader loader=new FXMLLoader(Main.class.getResource("/de/jasanex/pro/view/dashboard.fxml"));
                    Parent root=loader.load();Stage window=new Stage();window.setScene(new Scene(root,1180,780));window.show();stage.set(window);
                    DashboardController dashboard=loader.getController();controller.set(dashboard);dashboard.configure(session,new StammdatenCache(),()->{});return null;
                });
                awaitUnlocked(stage.get());
                fx(()->{
                    var serviceField=DashboardController.class.getDeclaredField("service");serviceField.setAccessible(true);serviceField.set(controller.get(),service);
                    var masterField=DashboardController.class.getDeclaredField("masterController");masterField.setAccessible(true);
                    var execute=DashboardController.class.getDeclaredMethod("execute",StammdatenController.Aktion.class,String.class);execute.setAccessible(true);
                    ((StammdatenController)masterField.get(controller.get())).configure(service,(action,message)->{
                        try{execute.invoke(controller.get(),action,message);}catch(Exception e){throw new RuntimeException(e);}
                    });
                    ((Button)stage.get().getScene().lookup("#masterButton")).fire();
                    AtomicReference<Throwable> dialogFailure=new AtomicReference<>();
                    Platform.runLater(()->{
                        DialogPane pane=null;
                        try {
                            pane=Window.getWindows().stream().filter(w->w!=stage.get() && w.getScene()!=null && w.getScene().getRoot() instanceof DialogPane)
                                    .map(w->(DialogPane)w.getScene().getRoot()).findFirst().orElseThrow();
                            GridPane grid=(GridPane)pane.getContent();
                            ((TextField)field(grid,0)).setText(selectedCode);
                            ((TextField)field(grid,3)).setText("15,50");
                            ((PasswordField)field(grid,4)).setText("kurz");((PasswordField)field(grid,5)).setText("kurz");
                            ((Button)pane.lookupButton(ButtonType.OK)).fire();
                            assertTrue(pane.getScene()!=null && pane.getScene().getWindow().isShowing(),"Ungültige Eingaben müssen im geöffneten Formular korrigierbar bleiben.");
                            assertTrue(grid.getChildren().stream().anyMatch(n -> n instanceof Label l && l.getText().contains("zwölf")));
                            assertEquals(selectedCode,((TextField)field(grid,0)).getText());
                            ((PasswordField)field(grid,4)).setText(secret);((PasswordField)field(grid,5)).setText(secret);
                            ((Button)pane.lookupButton(ButtonType.OK)).fire();
                        } catch(Throwable e){dialogFailure.set(e);if(pane!=null)((Button)pane.lookupButton(ButtonType.CANCEL)).fire();}
                    });
                    Button create=stage.get().getScene().getRoot().lookupAll(".button").stream().filter(n->n instanceof Button b && b.getText().equals("+ Neu anlegen")).map(n->(Button)n).findFirst().orElseThrow();
                    create.fire();if(dialogFailure.get()!=null)throw new AssertionError(dialogFailure.get());return null;
                });
                awaitUnlocked(stage.get());
                fx(()->{
                    assertEquals("Mitarbeiter gespeichert.",((Label)stage.get().getScene().lookup("#message")).getText());
                    var table=(TableView<?>)stage.get().getScene().lookup("#personal");assertFalse(table.isDisabled());
                    assertTrue(table.getItems().stream().anyMatch(p->p instanceof Personal employee && employee.kuerzel().equals(selectedCode)));return null;
                });
                try(var s=connection.prepareStatement("SELECT passwort_hash FROM mitarbeiter WHERE restaurant_id=? AND kuerzel=?")){
                    s.setInt(1,restaurant);s.setString(2,selectedCode);try(var r=s.executeQuery()){assertTrue(r.next());assertTrue(new de.jasanex.pro.security.PasswordHasher().verify(secret.toCharArray(),r.getString(1)));}
                }
            } finally {
                fx(()->{if(controller.get()!=null)controller.get().dispose();if(stage.get()!=null)stage.get().close();return null;});
                Platform.exit();connection.rollback();
            }
        }
    }
    private static Node field(GridPane grid,int row) {
        return grid.getChildren().stream().filter(n->Objects.equals(GridPane.getColumnIndex(n),1) && Objects.equals(GridPane.getRowIndex(n),row)).findFirst().orElseThrow();
    }
    private static void awaitUnlocked(Stage stage) throws Exception {
        for(int i=0;i<150;i++){
            if(fx(()->!((Button)stage.getScene().lookup("#refreshButton")).isDisabled()))return;
            Thread.sleep(100);
        }
        fail("Oberfläche bleibt gesperrt.");
    }
}
