package com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.tagValueEditor;

import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class TagValueEditorController<T> implements IWindowInitialize {
    public NullCombo<Tag> tagSelect;
    public TextArea valueInput;
    public ToggleButton pinToggle;

    private MainController mainController;
    public Button saveButton;
    public TextField itemName;
    public Button saveAndCloseButton;

    private TagValue<T> tagValue,parent;
    private Item item;
    private boolean embedded;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(tagValue);
        if(parent.getExists()){
            if (item.getExists() && !embedded) {
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }else{
            item.tagsProperty().map.put(parent.getId(),parent);//safe enough
            parent.setExists(true);
            tagValue.setExists(true);
            if(item.getExists() && !embedded) {
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }
    }

    public void setItemTagValue(Item item,TagValue<T> parent,boolean embeddedEditor){
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
        }else{
            saveButton.setStyle("-fx-base: -fx-mg-red;");
        }
        if(parent !=null && this.tagValue==null){
            this.parent = parent;
            this.tagValue= parent.cloneObjectFully();
            tagSelect.setRegexPredicate();
            tagSelect.setBackingList(Tag.COLLECTION.readableAndSortableList);
            tagSelect.setNullString("Deselect Tag");
            tagSelect.setNullableValue(this.tagValue.getTag());
            tagSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> tagValue.setTag(newValue));

            valueInput.setText(this.tagValue.getValueString());
            valueInput.focusedProperty().addListener(event -> {
                try{
                    String newValue=valueInput.getText();
                    if(newValue==null || newValue.length()==0){
                        this.tagValue.setValue(null);
                    }else {
                        this.tagValue.setValueFromString(newValue);
                    }
                }catch (Exception e){
                    this.tagValue.setValue(null);
                }
                valueInput.setText(this.tagValue.getValueString());
            });
        }
        if(this.tagValue!=null && this.item!=null && this.parent!=null){
            saveAndCloseButton.disableProperty().bind(saveButton.disableProperty());
            saveButton.disableProperty().bind(new BooleanBinding() {
                {
                    bind(tagValue.tagProperty());
                }
                @Override
                protected boolean computeValue() {
                    return tagValue.getTag()==null;
                }
            });
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
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
