package de.jasanex.pro.controller;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import de.jasanex.pro.cache.StammdatenCache;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.Betriebsdaten.*;
import de.jasanex.pro.security.SessionManager.Session;
import de.jasanex.pro.service.*;
import de.jasanex.pro.controller.StammdatenController.Form;

public final class DashboardController {
    @FunctionalInterface public interface LogoutHandler { void run() throws Exception; }
    @FXML private Label restaurant,username,role,clock,businessDay,staffCount,shiftCount,message,syncStatus;
    @FXML private VBox navigation,categories;
    @FXML private ScrollPane overview;
    @FXML private VBox master;
    @FXML private StammdatenController masterController;
    @FXML private Button masterButton,checkInButton,logoutButton,refreshButton;
    @FXML private ProgressIndicator progress;
    private Timeline timer;
    private LogoutHandler logout;
    private StammdatenCache cache;
    private BetriebService service;
    private Snapshot data;
    private boolean busy,disposed;
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r -> {Thread t=new Thread(r,"jasanex-betrieb");t.setDaemon(true);return t;});

    public void configure(Session session,StammdatenCache cache,LogoutHandler logout) {
        this.logout=logout;this.cache=cache;
        restaurant.setText(session.restaurant());username.setText(session.user().kuerzel());role.setText(session.role().name());
        staffCount.setText(Integer.toString(cache.mitarbeiter().size()));shiftCount.setText(Integer.toString(cache.schichten().size()));
        try {
            service=new BetriebService(DatabaseConfig.load(),session);
            masterController.configure(service,this::execute);
            masterButton.setVisible(service.verwaltung());masterButton.setManaged(service.verwaltung());
            refresh();
        } catch (Exception e) { message.setText("Lokale Konfiguration konnte nicht geladen werden.");checkInButton.setDisable(true);masterButton.setDisable(true); }
        updateClock();timer=new Timeline(new KeyFrame(Duration.seconds(1),event -> updateClock()));
        timer.setCycleCount(Timeline.INDEFINITE);timer.play();
    }
    private void updateClock() {
        ZonedDateTime now=ZonedDateTime.now(Betriebsregeln.ZEITZONE);
        clock.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        businessDay.setText(Betriebsregeln.geschaeftstag(now).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
    @FXML private void refresh() { if(service!=null) execute(null,"Daten aktualisiert."); }
    private void execute(StammdatenController.Aktion action,String success) {
        if(busy || disposed) return;
        setBusy(true);message.getStyleClass().remove("error");
        message.setText(action==null ? "Daten werden geladen …" : "Änderung wird gespeichert …");
        boolean[] committed={false};
        Task<Snapshot> task=new Task<>() {
            @Override protected Snapshot call() throws Exception {
                if(action!=null) { action.run();committed[0]=true; }
                return service.load();
            }
        };
        task.setOnSucceeded(event -> {
            if(disposed)return;
            try { render(task.getValue());message.setText(success); }
            catch(RuntimeException e) {
                message.getStyleClass().add("error");
                message.setText(committed[0] ? "Änderung in der Datenbank gespeichert, aber Anzeige fehlgeschlagen. Bitte aktualisieren."
                        : "Daten geladen, aber Anzeige fehlgeschlagen. Bitte aktualisieren.");
            } finally { setBusy(false); }
        });
        task.setOnFailed(event -> {
            if(disposed)return;setBusy(false);Throwable failure=task.getException();
            message.getStyleClass().add("error");
            message.setText(committed[0] ? "Gespeichert, aber Ansicht konnte nicht aktualisiert werden. Bitte Aktualisieren drücken."
                    : failure instanceof IllegalArgumentException ? "Nicht gespeichert: " + failure.getMessage()
                    : "Vorgang nicht bestätigt. Bitte Verbindung prüfen und Daten aktualisieren, bevor du erneut speicherst.");
            // Eingabefehler dürfen die Stammdatenverwaltung nicht dauerhaft sperren.
            // Bei unklarer Commitlage bleiben Aktionen bis zur Aktualisierung gesperrt.
            if(committed[0] || !(failure instanceof IllegalArgumentException)) {
                checkInButton.setDisable(true);master.setDisable(true);categories.setDisable(true);syncStatus.setText("Aktualisierung erforderlich");
            }
        });
        executor.submit(task);
    }
    private void setBusy(boolean value) {
        busy=value;progress.setVisible(value);progress.setManaged(value);
        refreshButton.setDisable(value);logoutButton.setDisable(value);
        checkInButton.setDisable(value || data==null);master.setDisable(value);categories.setDisable(value);
    }
    public void render(Snapshot value) {
        data=value;cache.replace(value);masterController.show(value);
        staffCount.setText(Integer.toString(value.mitarbeiter().size()));shiftCount.setText(Integer.toString(value.schichten().size()));
        syncStatus.setText("Stand: " + LocalTime.now(Betriebsregeln.ZEITZONE).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        categories.getChildren().clear();
        for(String category:Betriebsregeln.TAETIGKEITEN) {
            List<Schicht> shifts=value.schichten().stream().filter(s -> Betriebsregeln.kategorie(s.taetigkeit()).equals(category)).toList();
            VBox section=new VBox();section.getStyleClass().add("section-card");
            String title=switch(category){case "Head" -> "SCHICHTLEITUNG & MANAGEMENT";case "Küche" -> "KÜCHEN-TEAM";case "Autofahrer" -> "AUTOFAHRER";case "Fahrradfahrer" -> "FAHRRADFAHRER";default -> "SONSTIGES";};
            Label heading=new Label(title + "   ·   " + shifts.size()+" aktiv");heading.getStyleClass().add("on-dark");
            HBox header=new HBox(heading);header.getStyleClass().add("section-header");section.getChildren().add(header);
            FlowPane cards=new FlowPane(12,12);cards.getStyleClass().add("shift-cards");
            if(shifts.isEmpty()) {Label empty=new Label("Keine Mitarbeiter eingestempelt.");empty.getStyleClass().add("muted");cards.getChildren().add(empty);}
            for(Schicht shift:shifts) cards.getChildren().add(card(shift));
            section.getChildren().add(cards);categories.getChildren().add(section);
        }
    }
    private VBox card(Schicht shift) {
        VBox card=new VBox(8);card.getStyleClass().add("employee-card");card.setPrefWidth(285);
        Label title=new Label(shift.kuerzel()+"  ·  "+shift.rolle());title.getStyleClass().add("section-title");
        Label time=new Label("Seit "+shift.checkIn().format(DateTimeFormatter.ofPattern("dd.MM. HH:mm")));time.getStyleClass().add("muted");
        card.getChildren().addAll(title,time);
        if(shift.fahrzeugId()!=null) card.getChildren().add(new Label("Fahrzeug: "+shift.fahrzeug()));
        if(shift.handyId()!=null) card.getChildren().add(new Label("Handy: "+shift.handy()));
        Button out=new Button("Ausstempeln");out.getStyleClass().add("checkout-button");out.setOnAction(e -> checkOut(shift));card.getChildren().add(out);return card;
    }
    @FXML private void checkIn() {
        if(data==null || busy)return;
        Form form=new Form("Mitarbeiter einstempeln");
        List<de.jasanex.pro.model.Mitarbeiter> available=data.mitarbeiter().values().stream()
                .filter(m -> service.aktivStatus().equals(m.status()) && data.schichten().stream().noneMatch(s -> s.mitarbeiterId()==m.id()))
                .sorted(Comparator.comparing(de.jasanex.pro.model.Mitarbeiter::kuerzel)).toList();
        ComboBox<de.jasanex.pro.model.Mitarbeiter> employee=form.combo("Mitarbeiter",available,null);
        employee.setConverter(new javafx.util.StringConverter<>() {
            public String toString(de.jasanex.pro.model.Mitarbeiter m){return m==null?"":m.kuerzel()+" · "+m.rolle();}
            public de.jasanex.pro.model.Mitarbeiter fromString(String s){return null;}
        });
        PasswordField password=form.password("Mitarbeiterpasswort");
        ComboBox<String> activity=form.combo("Tätigkeit",Betriebsregeln.TAETIGKEITEN,"Küche");
        TextField reason=form.text("Begründung bei Sonstiges","");
        ComboBox<Fahrzeug> vehicle=form.combo("Fahrzeug (Fahrer)",List.of(),null);
        ComboBox<Handy> phone=form.combo("Handy (Fahrer)",data.handys().stream().filter(h -> !h.belegt() && "VERFUEGBAR".equals(h.status())).toList(),null);
        Runnable update=() -> {
            boolean driver=Betriebsregeln.fahrer(activity.getValue());vehicle.setDisable(!driver);phone.setDisable(!driver);
            vehicle.getItems().setAll(data.fahrzeuge().stream().filter(f -> !f.belegt() && "VERFUEGBAR".equals(f.status()) && ("Autofahrer".equals(activity.getValue())?"Auto":"Fahrrad").equals(f.typ())).toList());
            vehicle.setValue(null);phone.setValue(null);reason.setDisable(!"Sonstiges".equals(activity.getValue()));
        };
        activity.valueProperty().addListener((o,a,b)->update.run());update.run();
        Optional<CheckIn> result=form.show(() -> {
            if(employee.getValue()==null || password.getText().isEmpty())throw new IllegalArgumentException("Bitte Mitarbeiter und Passwort angeben.");
            boolean driver=Betriebsregeln.fahrer(activity.getValue());
            if(driver && (vehicle.getValue()==null || phone.getValue()==null))throw new IllegalArgumentException("Fahrer benötigen ein passendes Fahrzeug und ein Handy.");
            if("Sonstiges".equals(activity.getValue()) && reason.getText().isBlank())throw new IllegalArgumentException("Bitte die sonstige Tätigkeit begründen.");
            return new CheckIn(employee.getValue().id(),activity.getValue(),reason.getText(),password.getText().toCharArray(),driver?vehicle.getValue().id():null,driver?phone.getValue().id():null);
        });
        password.clear();result.ifPresent(input -> execute(() -> service.checkIn(input),"Mitarbeiter eingestempelt."));
    }
    private void checkOut(Schicht shift) {
        Form form=new Form(shift.kuerzel()+" ausstempeln");form.add("Beginn",new Label(shift.checkIn().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        ComboBox<String> vehicle=null,phone=null;TextField vehicleNote=null,phoneNote=null;
        if(shift.fahrzeugId()!=null){vehicle=form.combo("Fahrzeug geprüft",List.of("In Ordnung","Mangel"),null);vehicleNote=form.text("Fahrzeug-Mangel","");}
        if(shift.handyId()!=null){phone=form.combo("Handy geprüft",List.of("In Ordnung","Mangel"),null);phoneNote=form.text("Handy-Mangel","");}
        final ComboBox<String> v=vehicle,h=phone;final TextField vn=vehicleNote,hn=phoneNote;
        form.show(() -> new CheckOut(shift.id(),pruefung(v,vn),pruefung(h,hn))).ifPresent(input -> execute(() -> service.checkOut(input),"Mitarbeiter ausgestempelt."));
    }
    private Pruefung pruefung(ComboBox<String> choice,TextField note) {
        if(choice==null)return null;
        if(choice.getValue()==null)throw new IllegalArgumentException("Bitte den Zustand jeder Ressource bestätigen.");
        boolean ok="In Ordnung".equals(choice.getValue());
        if(!ok && note.getText().isBlank())throw new IllegalArgumentException("Bitte den Mangel beschreiben.");return new Pruefung(ok,note.getText());
    }
    @FXML private void showOverview(){overview.setVisible(true);overview.setManaged(true);master.setVisible(false);master.setManaged(false);}
    @FXML private void showMaster(){if(service!=null && service.verwaltung()){overview.setVisible(false);overview.setManaged(false);master.setVisible(true);master.setManaged(true);}}
    @FXML private void toggleNavigation(){boolean visible=!navigation.isVisible();navigation.setVisible(visible);navigation.setManaged(visible);}
    @FXML private void logout(){if(busy)return;try{logout.run();}catch(Exception e){message.setText("Loginansicht konnte nicht geöffnet werden. Bitte neu starten.");}}
    public void dispose(){disposed=true;if(timer!=null)timer.stop();executor.shutdown();if(masterController!=null)masterController.show(new Snapshot(Map.of(),List.of(),List.of(),List.of(),List.of(),List.of()));data=null;}
}
