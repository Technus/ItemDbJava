package com.dgs.dapc.itemDB.javafx.main.editor.contactEditor;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Contact;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.qr.ShowQRController;
import com.dgs.dapc.itemDB.javafx.urlTextField.UrlCombo;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;

public class ContactEditorController implements IWindowInitialize {
    public TextField nameInput;
    public TextArea detailsInput;
    public UrlCombo urlInput;
    public TextField imageInput;
    public ImageView imageView;
    public Button saveButton;
    public Button showQR;
    public Button printQR;
    public Button saveAndCloseButton;
    public ScrollPane imageScroll;
    public BorderPane imageBorder;
    public ToggleButton pinToggle;

    private Contact contact,parent;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(contact);
        if(parent.getExists()){
            mainController.model.logic.getContactCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(parent.getId())),parent);
        }else{
            mainController.model.logic.getContactCollection().insertOne(parent);
            Contact.COLLECTION.map.put(parent.getId(),parent);
            parent.setExists(true);
            contact.setExists(true);
        }
    }

    public void setImage(ActionEvent actionEvent) {
        imageInput.setText(Utility.selectImage(imageInput.getText()));
    }

    public void setContact(Contact parent){
        if (parent != null && this.contact == null) {
            this.parent = parent;
            this.contact = parent.cloneObjectFully();
            saveAndCloseButton.styleProperty().bind(new StringBinding() {
                {
                    bind(ContactEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return ContactEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-bg-cyan;";
                }
            });
            saveButton.styleProperty().bind(new StringBinding() {
                {
                    bind(ContactEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return ContactEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-mg-cyan;";
                }
            });
            nameInput.textProperty().bindBidirectional(this.contact.nameProperty());
            detailsInput.textProperty().bindBidirectional(this.contact.detailsProperty());
            urlInput.valueProperty().bindBidirectional(this.contact.urlProperty());
            imageInput.textProperty().bindBidirectional(this.contact.pictureProperty());
            imageView.imageProperty().bind(this.contact.imageProperty());
        }
    }

    public void print(ActionEvent actionEvent) {
        PrintQR.print(parent.getDiscriminatedId());
    }

    public void show(ActionEvent actionEvent) {
        Utility.Window<ShowQRController> window=Utility.loadFXML(ShowQRController.class.getResource("ShowQR.fxml"),"QR View: "+parent.getDiscriminatedId().toString(),getStage());
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
