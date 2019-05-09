package com.dgs.dapc.itemDB.javafx.main.editor.tagEditor;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.dgs.dapc.itemDB.javafx.qr.ShowQRController;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;

public class TagEditorController implements IWindowInitialize {
    public TextField nameInput;
    public TextArea detailsInput;
    public NullCombo<Class> typeSelect;
    public NullCombo<Class> converterSelect;
    public Button saveButton;
    public Button printQR;
    public Button showQR;
    public Button saveAndCloseButton;
    public ToggleButton pinToggle;

    private Tag tag,parent;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        boolean typeCheck=parent.getType()==tag.getType()&&parent.getConverter()==tag.getConverter();
        parent.setFully(tag);
        if(parent.getExists()){
            if(typeCheck){
                mainController.model.logic.getTagCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(parent.getId())), parent);
            }else {
                Alert alert=new Alert(Alert.AlertType.WARNING,"Warning you are about to change a tag!\nYou changed type or converter it can break stuff!", ButtonType.OK,ButtonType.CANCEL);
                alert.initOwner(stage);//to get on top?
                if(alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK) {
                    mainController.model.logic.getTagCollection()
                            .replaceOne(new BsonDocument().append("_id", new BsonObjectId(parent.getId())), parent);
                }
            }
        }else{
            mainController.model.logic.getTagCollection().insertOne(parent);
            Tag.COLLECTION.map.put(parent.getId(),parent);
            parent.setExists(true);
            tag.setExists(true);
        }
    }

    public void setTag(Tag parent){
        if(parent !=null && this.tag==null){
            this.parent = parent;
            this.tag= parent.cloneObjectFully();
            saveAndCloseButton.styleProperty().bind(new StringBinding() {
                {
                    bind(TagEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return TagEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-bg-cyan;";
                }
            });
            saveButton.styleProperty().bind(new StringBinding() {
                {
                    bind(TagEditorController.this.parent.existsProperty());
                }
                @Override
                protected String computeValue() {
                    return TagEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-mg-cyan;";
                }
            });
            nameInput.textProperty().bindBidirectional(this.tag.nameProperty());
            detailsInput.textProperty().bindBidirectional(this.tag.detailsProperty());

            typeSelect.setRegexPredicate();
            typeSelect.setNullText("Deselect Data Type");
            typeSelect.setBackingList(Tag.TYPES_LIST);
            typeSelect.setValue(this.tag.getType());
            typeSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> tag.setType(newValue));
            typeSelect.setFilter(t -> {
                if(t==null){
                    return false;
                }
                return t.toString().toLowerCase().contains(typeSelect.getEditor().textProperty().getValueSafe().toLowerCase());
            });

            converterSelect.setRegexPredicate();
            converterSelect.setNullText("Deselect String Converter");
            converterSelect.setBackingList(Tag.CONVERTERS_LIST);
            converterSelect.setValue(this.tag.getConverter());
            converterSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> tag.setConverter(newValue));
            converterSelect.setFilter(t -> {
                if(t==null){
                    return false;
                }
                return t.toString().toLowerCase().contains(converterSelect.getEditor().textProperty().getValueSafe().toLowerCase());
            });
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
