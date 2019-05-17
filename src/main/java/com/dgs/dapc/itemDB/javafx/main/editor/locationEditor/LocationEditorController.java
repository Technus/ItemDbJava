package com.dgs.dapc.itemDB.javafx.main.editor.locationEditor;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Location;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.dgs.dapc.itemDB.javafx.qr.ShowQRController;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;

public class LocationEditorController implements IWindowInitialize {
    public TextField nameInput;
    public TextArea detailsInput;
    public TextField coordinateLimitsInput;
    public TextField imageInput;
    public ImageView imageView;
    public Button saveButton;
    public Button showQR;
    public Button printQR;
    public NullCombo<Location> parentSelect;
    public Button saveAndCloseButton;
    public ScrollPane imageScroll;
    public BorderPane imageBorder;
    public ToggleButton pinToggle;

    private Location location,parent;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(location);
        if(parent.getExists()){
            mainController.model.logic.getLocationCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(parent.getId())),parent);
        }else{
            mainController.model.logic.getLocationCollection().insertOne(parent);
            Location.COLLECTION.map.put(parent.getId(),parent);
            parent.setExists(true);
            location.setExists(true);
        }
    }

    public void setImage(ActionEvent actionEvent) {
        imageInput.setText(Utility.selectImage(imageInput.getText()));
    }

    public void setLocation(Location parent){
        if (parent != null && this.location == null) {
            this.parent = parent;
            this.location = parent.cloneObjectFully();
            saveAndCloseButton.styleProperty().bind(new StringBinding() {
                {
                    bind(LocationEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return LocationEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-bg-cyan;";
                }
            });
            saveButton.styleProperty().bind(new StringBinding() {
                {
                    bind(LocationEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return LocationEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-mg-cyan;";
                }
            });
            nameInput.textProperty().bindBidirectional(this.location.nameProperty());
            detailsInput.textProperty().bindBidirectional(this.location.detailsProperty());
            imageInput.textProperty().bindBidirectional(this.location.pictureProperty());
            imageView.imageProperty().bind(this.location.imageProperty());
            coordinateLimitsInput.setText(Utility.DOUBLE_LIST_CONVERTER.toString(this.location.getCoordinateLimits()));
            coordinateLimitsInput.focusedProperty().addListener(event -> {
                try{
                    this.location.setCoordinateLimits(Utility.DOUBLE_LIST_CONVERTER.fromString(coordinateLimitsInput.getText()));
                }catch (Exception e){
                    e.printStackTrace();
                    this.location.coordinateLimitsProperty().clear();
                    coordinateLimitsInput.textProperty().set("");
                }
            });
            parentSelect.setRegexPredicate();
            parentSelect.setNullString("Deselect Location");
            parentSelect.setBackingList(Location.COLLECTION.readableAndSortableList);
            Platform.runLater(()->parentSelect.setNullableValue(this.location.getParent()));
            parentSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> location.setParent(newValue));
        }
    }

    public void print(ActionEvent actionEvent) {
        PrintQR.print(parent.getDiscriminatedId());
    }

    public void show(ActionEvent actionEvent) {
        Utility.Window<ShowQRController> window=Utility.loadFXML(ShowQRController.class.getResource("ShowQR.fxml"),"QR View: "+parent.getDiscriminatedId().toString());
        window.controller.setQrImage(parent.getDiscriminatedId());
        window.stage.show();
    }

    private Stage stage;

    @Override
    public void initializeStage(Stage stage) {
        this.stage=stage;
        saveAndCloseButton.setOnAction(event -> {
            save(event);
            mainController.editors.remove(this);
            stage.close();
        });
        stage.setOnCloseRequest(event -> mainController.editors.remove(this));
        pinToggle.setSelected(stage.isAlwaysOnTop());
        pinToggle.selectedProperty().addListener((observable, oldValue, newValue) -> stage.setAlwaysOnTop(newValue));
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
