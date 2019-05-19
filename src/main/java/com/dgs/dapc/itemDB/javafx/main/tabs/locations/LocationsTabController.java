package com.dgs.dapc.itemDB.javafx.main.tabs.locations;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Location;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.locationEditor.LocationEditorController;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.Document;

import java.net.URL;
import java.util.ResourceBundle;

public class LocationsTabController implements Initializable {
    public TreeTableView<Location> locationsTree;
    public TreeTableColumn<Location,String> locationsNameColumn;
    public TreeTableColumn<Location, String> locationCoordinateLimitsColumn;
    public TreeTableColumn<Location,String> locationDetailsColumn;
    public MainController mainController;
    public Button newBasedOnButton;
    public Button removeButton;
    public ToggleButton expandToggle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        locationsTree.setShowRoot(false);
        locationsTree.setRoot(Location.ROOT);

        Utility.setExpandRecursively(Location.ROOT,expandToggle.isSelected());
        expandToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Utility.setExpandRecursively(Location.ROOT,newValue);
        });

        locationsTree.setRowFactory(tv -> {
            TreeTableRow<Location> row = new TreeTableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                    e.consume();
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                    Location rowData = row.getItem();
                    Utility.Window<LocationEditorController> window=Utility.loadFXML(LocationEditorController.class.getResource("LocationEditor.fxml"),"Location Editor: "+rowData.getId().toHexString(),mainController.getStage());
                    window.controller.setMainController(mainController);
                    window.controller.setLocation(rowData);
                    window.stage.show();
                    event.consume();
                }
            });
            return row;
        });
        locationDetailsColumn.setOnEditCommit(event -> {
            event.getRowValue().getValue().setDetails(event.getNewValue());
            mainController.model.logic.getLocationCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getValue().getId())),event.getRowValue().getValue());
        });
        locationDetailsColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        locationDetailsColumn.setCellValueFactory(param-> param.getValue().getValue().detailsProperty());

        locationsNameColumn.setOnEditCommit(event -> {
            event.getRowValue().getValue().setName(event.getNewValue());
            mainController.model.logic.getLocationCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(event.getRowValue().getValue().getId())),event.getRowValue().getValue());
        });
        locationsNameColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        locationsNameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty());

        locationCoordinateLimitsColumn.setCellValueFactory(param -> param.getValue().getValue().coordinateLimitsProperty().toStringProperty());

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(locationsTree.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                return locationsTree.getSelectionModel().getSelectedItem()==null;
            }
        });
        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(locationsTree.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                TreeItem treeItem = locationsTree.getSelectionModel().getSelectedItem();
                return treeItem == null || treeItem.getValue()==null;
            }
        });
    }

    public void create(ActionEvent actionEvent) {
        Location location=new Location();
        Utility.Window<LocationEditorController> window=Utility.loadFXML(LocationEditorController.class.getResource("LocationEditor.fxml"),"Location Editor: "+location.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setLocation(location);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Location location=locationsTree.getSelectionModel().getSelectedItem().getValue().cloneObjectData();
        Utility.Window<LocationEditorController> window=Utility.loadFXML(LocationEditorController.class.getResource("LocationEditor.fxml"),"Location Editor: "+location.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setLocation(location);
        window.stage.show();
    }

    public void removeSelected(ActionEvent actionEvent) {
        TreeItem<Location> o=locationsTree.getSelectionModel().getSelectedItem();
        if(ButtonType.OK==new Alert(Alert.AlertType.CONFIRMATION,"Remove "+o.getValue()+" ?").showAndWait().orElse(ButtonType.CANCEL)) {
            mainController.model.logic.getLocationCollection().deleteOne(new Document("_id", o.getValue().getId()));
            o.getParent().getChildren().remove(o);
        }
    }
}
