package com.dgs.dapc.itemDB.javafx.main.tabs.tags;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.tagEditor.TagEditorController;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.Document;

import java.net.URL;
import java.util.ResourceBundle;

public class TagsTabController implements Initializable {
    public TableColumn<Tag,String> tagsNameColumn;
    public TableColumn<Tag,String> tagsDetailsColumn;
    public TableColumn<Tag,Class> tagsDataTypeColumn;
    public TableColumn<Tag,Class> tagsConverterTypeColumn;
    public TableView<Tag> tagsTable;
    public MainController mainController;
    public Button newBasedOnButton;
    public Button removeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tagsTable.setItems(Tag.COLLECTION.readableAndSortableList);
        tagsTable.setRowFactory( tv -> {
            TableRow<Tag> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY)
                    e.consume();
            });
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY && !row.isEmpty()) {
                    Tag rowData = row.getItem();
                    Utility.Window<TagEditorController> window=Utility.loadFXML(TagEditorController.class.getResource("TagEditor.fxml"),"Tag Editor: "+rowData.getId().toHexString(),mainController.getStage());
                    window.controller.setMainController(mainController);
                    window.controller.setTag(rowData);
                    window.stage.show();
                    e.consume();
                }
            });
            return row;
        });
        tagsNameColumn.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            mainController.model.logic.getTagCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())), event.getRowValue());

        });
        tagsNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tagsNameColumn.setCellValueFactory(param-> param.getValue().nameProperty());

        tagsDetailsColumn.setOnEditCommit(event -> {
            event.getRowValue().setDetails(event.getNewValue());
            mainController.model.logic.getTagCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())), event.getRowValue());

        });
        tagsDetailsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tagsDetailsColumn.setCellValueFactory(param-> param.getValue().detailsProperty());

        tagsDataTypeColumn.setCellValueFactory(param -> param.getValue().typeProperty());
        tagsConverterTypeColumn.setCellValueFactory(param -> param.getValue().converterProperty());

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(tagsTable.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                return tagsTable.getSelectionModel().getSelectedItem()==null;
            }
        });
        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(tagsTable.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                return tagsTable.getSelectionModel().getSelectedItem() == null;
            }
        });
    }

    public void create(ActionEvent actionEvent) {
        Tag tag=new Tag();
        Utility.Window<TagEditorController> window=Utility.loadFXML(TagEditorController.class.getResource("TagEditor.fxml"),"Tag Editor: "+tag.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setTag(tag);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Tag tag=tagsTable.getSelectionModel().getSelectedItem().cloneObjectData();
        Utility.Window<TagEditorController> window=Utility.loadFXML(TagEditorController.class.getResource("TagEditor.fxml"),"Tag Editor: "+tag.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setTag(tag);
        window.stage.show();
    }

    public void removeSelected(ActionEvent actionEvent) {
        Tag tag=tagsTable.getSelectionModel().getSelectedItem();
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Remove "+tag+" ?");
        alert.initOwner(mainController.getStage());
        if(ButtonType.OK==alert.showAndWait().orElse(ButtonType.CANCEL)) {
            mainController.model.logic.getTagCollection().deleteOne(new Document("_id", tag.getId()));
            Tag.COLLECTION.map.remove(tag.getId());
        }
    }
}
