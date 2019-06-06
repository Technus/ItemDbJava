package com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.sourceEditor;

import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Contact;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.dgs.dapc.itemDB.javafx.urlTextField.UrlCombo;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SourceEditorController implements IWindowInitialize {
    public TextField nameInput;
    public NullCombo<Contact> supplierSelect;
    public UrlCombo sourceURL;
    public TextArea detailsInput;
    public ToggleButton pinToggle;
    public ToggleButton supplierRegexp;

    private MainController mainController;
    public Button saveButton;
    public TextField itemName;
    public Button saveAndCloseButton;

    private Source source,parent;
    private Item item;
    private boolean embedded;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(source);
        if(parent.getExists()){
            if (item.getExists() && !embedded) {
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }else{
            if(item.sourcesProperty().stream().noneMatch(sourceTreeItem -> sourceTreeItem.getValue()==parent)) {
                item.sourcesProperty().add(new TreeItem<>(parent));
            }
            parent.setExists(true);
            source.setExists(true);
            if(item.getExists() && !embedded) {
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }
    }

    public void setItemSource(Item item,Source parent,boolean embeddedEditor){
        embedded=embeddedEditor;
        if(item!=null && this.item ==null){
            this.item =item;
            if(embedded){
                saveAndCloseButton.styleProperty().bind(new StringBinding() {
                    {
                        bind(parent.existsProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return parent.getExists() ? "-fx-base: -fx-bg-orange;" : "-fx-base: -fx-bg-red;";
                    }
                });
                saveButton.styleProperty().bind(new StringBinding() {
                    {
                        bind(parent.existsProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return parent.getExists() ? "-fx-base: -fx-mg-orange;" : "-fx-base: -fx-mg-red;";
                    }
                });
            }else {
                saveAndCloseButton.styleProperty().bind(new StringBinding() {
                    {
                        bind(parent.existsProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return parent.getExists() ? "" : "-fx-base: -fx-bg-cyan;";
                    }
                });
                saveButton.styleProperty().bind(new StringBinding() {
                    {
                        bind(parent.existsProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return parent.getExists() ? "" : "-fx-base: -fx-mg-cyan;";
                    }
                });
            }
            itemName.textProperty().bind(item.nameProperty());
        }
        if(parent !=null && this.source==null){
            this.parent = parent;
            this.source= parent.cloneObjectFully();
            nameInput.textProperty().bindBidirectional(this.source.nameProperty());
            sourceURL.valueProperty().bindBidirectional(this.source.urlProperty());
            detailsInput.textProperty().bindBidirectional(this.source.detailsProperty());
            supplierSelect.setRegexPredicate();
            supplierSelect.setNullString("Deselect Contact");
            supplierSelect.setBackingList(Contact.COLLECTION.readableAndSortableList);
            supplierSelect.setNullableValue(this.source.getSupplier());
            supplierSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> source.setSupplier(newValue));
        }
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
        supplierRegexp.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                supplierSelect.setRegexPredicate();
            }else {
                supplierSelect.setPredicate();
            }
        });
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
