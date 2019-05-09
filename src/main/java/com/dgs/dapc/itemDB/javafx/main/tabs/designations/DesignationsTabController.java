package com.dgs.dapc.itemDB.javafx.main.tabs.designations;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Designation;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.designationEditor.DesignationEditorController;
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

public class DesignationsTabController implements Initializable {
    public TableView<Designation> designationsTable;
    public TableColumn<Designation,String> designationsNameColumn;
    public TableColumn<Designation,String> designationsDetailsColumn;
    public MainController mainController;
    public Button newBasedOnButton;
    public Button removeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        designationsTable.setItems(Designation.COLLECTION.readableAndSortableList);
        designationsTable.setRowFactory( tv -> {
            TableRow<Designation> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                if (e.getClickCount() % 2 == 0 && e.getButton().equals(MouseButton.PRIMARY))
                    e.consume();
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                    Designation rowData = row.getItem();
                    Utility.Window<DesignationEditorController> window=Utility.loadFXML(DesignationEditorController.class.getResource("DesignationEditor.fxml"),"Designation Editor: "+rowData.getId().toHexString());
                    window.controller.setMainController(mainController);
                    window.controller.setDesignation(rowData);
                    window.stage.show();
                    event.consume();
                }
            });
            return row;
        });

        designationsNameColumn.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            mainController.model.logic.getDesignationCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())),event.getRowValue());
        });
        designationsNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        designationsNameColumn.setCellValueFactory(param-> param.getValue().nameProperty());

        designationsDetailsColumn.setOnEditCommit(event -> {
            event.getRowValue().setDetails(event.getNewValue());
            mainController.model.logic.getDesignationCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getId())),event.getRowValue());
        });
        designationsDetailsColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        designationsDetailsColumn.setCellValueFactory(param-> param.getValue().detailsProperty());

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(designationsTable.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                return designationsTable.getSelectionModel().getSelectedItem()==null;
            }
        });
        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(designationsTable.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                return designationsTable.getSelectionModel().getSelectedItem() == null;
            }
        });
    }

    public void create(ActionEvent actionEvent) {
        Designation designation=new Designation();
        Utility.Window<DesignationEditorController> window=Utility.loadFXML(DesignationEditorController.class.getResource("DesignationEditor.fxml"),"Designation Editor: "+designation.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setDesignation(designation);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Designation designation=designationsTable.getSelectionModel().getSelectedItem().cloneObjectData();
        Utility.Window<DesignationEditorController> window=Utility.loadFXML(DesignationEditorController.class.getResource("DesignationEditor.fxml"),"Designation Editor: "+designation.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setDesignation(designation);
        window.stage.show();
    }

    public void removeSelected(ActionEvent actionEvent) {
        Designation designation=designationsTable.getSelectionModel().getSelectedItem();
        if(ButtonType.OK==new Alert(Alert.AlertType.CONFIRMATION,"Remove "+designation+" ?").showAndWait().orElse(ButtonType.CANCEL)) {
            mainController.model.logic.getDesignationCollection().deleteOne(new Document("_id", designation.getId()));
            designationsTable.getItems().remove(designation);
        }
    }
}
