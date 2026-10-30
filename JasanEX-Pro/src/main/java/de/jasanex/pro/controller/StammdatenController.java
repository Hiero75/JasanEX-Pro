package de.jasanex.pro.controller;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import de.jasanex.pro.model.Betriebsdaten.*;
import de.jasanex.pro.service.BetriebService;

/** Tabellenansicht und Eingabedialoge, ohne Datenbankcode. */
public final class StammdatenController {
    @FunctionalInterface public interface Aktion { void run() throws Exception; }
    @FunctionalInterface public interface Ausfuehren { void run(Aktion action, String success); }
    @FXML private TableView<Personal> personal;
    @FXML private TableView<Fahrzeug> fahrzeuge;
    @FXML private TableView<Handy> handys;
    @FXML private TabPane tabs;
    private BetriebService service;
    private Ausfuehren ausfuehren;

    @FXML private void initialize() {
        column(personal,"Kürzel",Personal::kuerzel); column(personal,"Rolle",Personal::rolle);
        column(personal,"Status",Personal::status); column(personal,"€/Stunde",p -> p.stundensatz().toPlainString());
        column(fahrzeuge,"Nummer",Fahrzeug::nummer); column(fahrzeuge,"Typ",Fahrzeug::typ);
        column(fahrzeuge,"Kennzeichen",Fahrzeug::kennzeichen); column(fahrzeuge,"Marke",Fahrzeug::marke);
        column(fahrzeuge,"Status",f -> f.belegt() ? "IN BENUTZUNG" : f.status());
        column(handys,"Bezeichnung",Handy::bezeichnung); column(handys,"Mobilnummer",Handy::mobilnummer);
        column(handys,"Status",h -> h.belegt() ? "IN BENUTZUNG" : h.status());
        for (TableView<?> table : List.of(personal,fahrzeuge,handys)) {
            table.setPlaceholder(new Label("Keine Einträge vorhanden."));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
    }
    private <T> void column(TableView<T> table,String title,Function<T,String> value) {
        TableColumn<T,String> column=new TableColumn<>(title);
        column.setCellValueFactory(c -> new ReadOnlyStringWrapper(Objects.toString(value.apply(c.getValue()),"")));
        table.getColumns().add(column);
    }
    public void configure(BetriebService service,Ausfuehren ausfuehren) { this.service=service; this.ausfuehren=ausfuehren; }
    public void show(Snapshot data) {
        personal.getItems().setAll(data.personal()); fahrzeuge.getItems().setAll(data.fahrzeuge()); handys.getItems().setAll(data.handys());
    }
    @FXML private void neu() { edit(true); }
    @FXML private void bearbeiten() { edit(false); }
    private void edit(boolean neu) {
        switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> { Personal selected=personal.getSelectionModel().getSelectedItem(); if(neu || selected!=null) personalDialog(neu?null:selected); else hinweis(); }
            case 1 -> { Fahrzeug selected=fahrzeuge.getSelectionModel().getSelectedItem(); if(neu || selected!=null) fahrzeugDialog(neu?null:selected); else hinweis(); }
            case 2 -> { Handy selected=handys.getSelectionModel().getSelectedItem(); if(neu || selected!=null) handyDialog(neu?null:selected); else hinweis(); }
            default -> { }
        }
    }
    private void personalDialog(Personal old) {
        Form form=new Form("Mitarbeiter " + (old==null ? "anlegen" : "bearbeiten"));
        TextField kuerzel=form.text("Kürzel (max. 2 Zeichen)",old==null?"":old.kuerzel());
        ComboBox<String> rolle=form.combo("Rolle",service.rollen(),old==null?"Mitarbeiter":old.rolle());
        ComboBox<String> status=form.combo("Status",List.of(service.aktivStatus(),"INAKTIV"),old==null?service.aktivStatus():old.status());
        TextField wage=form.text("Stundensatz (€)",old==null?"0.00":old.stundensatz().toPlainString());
        PasswordField password=form.password(old==null?"Passwort (mind. 12 Zeichen)":"Neues Passwort (leer = behalten)");
        PasswordField repeat=form.password("Neues Passwort wiederholen");
        Optional<PersonalEingabe> result=form.show(() -> {
            if (!password.getText().equals(repeat.getText())) throw new IllegalArgumentException("Die Passwörter stimmen nicht überein.");
            BigDecimal rate;
            try { rate=new BigDecimal(wage.getText().trim().replace(',','.')); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Bitte einen gültigen Stundensatz eingeben."); }
            PersonalEingabe input=new PersonalEingabe(old==null?null:old.id(),kuerzel.getText(),rolle.getValue(),status.getValue(),rate,password.getText().toCharArray());
            try {
                service.validatePersonal(input);
                boolean duplicate=personal.getItems().stream().anyMatch(p -> !Objects.equals(input.id(),p.id()) && p.kuerzel().equalsIgnoreCase(input.kuerzel().trim()));
                if(duplicate) throw new IllegalArgumentException("Dieses Kürzel ist bereits vergeben. Bitte ein anderes wählen.");
                return input;
            } catch(RuntimeException e) { Arrays.fill(input.passwort(),'\0');throw e; }
        });
        password.clear(); repeat.clear();
        result.ifPresent(input -> ausfuehren.run(() -> service.savePersonal(input),"Mitarbeiter gespeichert."));
    }
    private void fahrzeugDialog(Fahrzeug old) {
        Form form=new Form("Fahrzeug " + (old==null ? "anlegen" : "bearbeiten"));
        ComboBox<String> typ=form.combo("Typ",List.of("Auto","Fahrrad"),old==null?"Auto":old.typ());
        TextField nummer=form.text("Nummer",old==null?"":old.nummer());
        TextField kennzeichen=form.text("Kennzeichen",old==null?"":old.kennzeichen());
        TextField marke=form.text("Marke",old==null?"":old.marke());
        ComboBox<String> status=form.combo("Status",List.of("VERFUEGBAR","DEFEKT","GESPERRT"),old==null?"VERFUEGBAR":old.status());
        form.show(() -> new FahrzeugEingabe(old==null?null:old.id(),typ.getValue(),nummer.getText(),kennzeichen.getText(),marke.getText(),status.getValue()))
                .ifPresent(input -> ausfuehren.run(() -> service.saveFahrzeug(input),"Fahrzeug gespeichert."));
    }
    private void handyDialog(Handy old) {
        Form form=new Form("Handy " + (old==null ? "anlegen" : "bearbeiten"));
        TextField name=form.text("Bezeichnung",old==null?"":old.bezeichnung());
        TextField nummer=form.text("Mobilnummer",old==null?"":old.mobilnummer());
        ComboBox<String> status=form.combo("Status",List.of("VERFUEGBAR","DEFEKT","GESPERRT"),old==null?"VERFUEGBAR":old.status());
        PasswordField pin=form.password("Neue SIM-PIN (leer = behalten)");
        var result=form.show(() -> new HandyEingabe(old==null?null:old.id(),name.getText(),nummer.getText(),status.getValue(),pin.getText()));
        pin.clear();result.ifPresent(input -> ausfuehren.run(() -> service.saveHandy(input),"Handy gespeichert."));
    }
    @FXML private void loeschen() {
        StammdatenArt art; int id; String name;
        switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> { Personal p=personal.getSelectionModel().getSelectedItem(); if(p==null){hinweis();return;} art=StammdatenArt.MITARBEITER;id=p.id();name=p.kuerzel(); }
            case 1 -> { Fahrzeug f=fahrzeuge.getSelectionModel().getSelectedItem(); if(f==null){hinweis();return;} art=StammdatenArt.FAHRZEUG;id=f.id();name=f.toString(); }
            case 2 -> { Handy h=handys.getSelectionModel().getSelectedItem(); if(h==null){hinweis();return;} art=StammdatenArt.HANDY;id=h.id();name=h.toString(); }
            default -> { return; }
        }
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"„"+name+"“ endgültig löschen?\nDatensätze mit verknüpfter Historie bleiben geschützt.",ButtonType.CANCEL,ButtonType.OK);
        alert.setTitle("Löschen bestätigen"); alert.setHeaderText("Stammdatensatz löschen");alert.initOwner(tabs.getScene().getWindow());
        if(alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) ausfuehren.run(() -> service.delete(art,id),"Datensatz gelöscht.");
    }
    private void hinweis() { new Alert(Alert.AlertType.INFORMATION,"Bitte zuerst einen Datensatz auswählen.",ButtonType.OK).showAndWait(); }

    /** Validierungsfehler lassen das Formular geöffnet. */
    public static final class Form {
        private final Dialog<Object> dialog=new Dialog<>();
        private final GridPane grid=new GridPane();
        private final Label error=new Label();
        private int row;
        public Form(String title) {
            dialog.setTitle("JasanEX Pro – " + title);dialog.setHeaderText(title);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
            dialog.getDialogPane().getStylesheets().add(StammdatenController.class.getResource("/de/jasanex/pro/css/style.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("data-dialog");
            grid.setHgap(16);grid.setVgap(12);grid.setPrefWidth(520);
            dialog.getDialogPane().setContent(grid);error.setWrapText(true);error.getStyleClass().add("error");
        }
        public void add(String label,Node field) { grid.add(new Label(label),0,row);grid.add(field,1,row++); }
        public TextField text(String label,String value) { TextField f=new TextField(Objects.toString(value,""));add(label,f);return f; }
        public PasswordField password(String label) { PasswordField f=new PasswordField();add(label,f);return f; }
        public <T> ComboBox<T> combo(String label,List<T> values,T selected) {
            ComboBox<T> f=new ComboBox<>();f.getItems().setAll(values);f.setValue(selected);f.setMaxWidth(Double.MAX_VALUE);add(label,f);return f;
        }
        @SuppressWarnings("unchecked") public <T> Optional<T> show(Supplier<T> value) {
            grid.add(error,0,row,2,1);Object[] result=new Object[1];
            dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION,event -> {
                try { result[0]=value.get(); } catch (IllegalArgumentException e) { error.setText(e.getMessage());event.consume(); }
            });
            dialog.setResultConverter(button -> button==ButtonType.OK ? result[0] : null);
            return (Optional<T>)(Optional<?>)dialog.showAndWait();
        }
    }
}
