package com.dgs.dapc.itemDB.javafx.main.tabs.items;

import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.placementEditor.PlacementEditorController;
import com.github.technus.dbAdditions.mongoDB.pojo.Tuple2;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.dgs.dapc.itemDB.Utility.THE_DOUBLE_CONVERTER;

public class ChangesOverviewController implements IWindowInitialize {
    public static Tuple2<Item,Placement> currentDrag;

    public Button removeButton;
    public TableView<Tuple2<Item,Placement>> placementsTable;
    public Button subButton,addButton;
    public ToggleButton pinToggle;
    public Spinner<Double> countSpinner;

    public TableColumn<Tuple2<Item,Placement>,String> itemNameColumn;
    public TableColumn<Tuple2<Item,Placement>,String> manufacturerColumn;
    public TableColumn<Tuple2<Item,Placement>,String> designationsColumn;
    public TableColumn<Tuple2<Item,Placement>,Double> quantityColumn;
    public TableColumn<Tuple2<Item,Placement>,Double> minimalColumn;
    public TableColumn<Tuple2<Item,Placement>,Double> purchasedColumn;
    public TableColumn<Tuple2<Item,Placement>,String> serialColumn;
    public TableColumn<Tuple2<Item,Placement>,String> detailsColumn;
    public TableColumn<Tuple2<Item,Placement>,String> locationColumn;
    public TableColumn<Tuple2<Item,Placement>,String> rowColColumn;

    private MainController mainController;

    public void setSelectedPlacements(List<Tuple2<Item,Placement>> selectedPlacements) {
        placementsTable.getItems().setAll(selectedPlacements);
        placementsTable.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Tuple2<Item, Placement>>) c -> {
                    removeButton.setDisable(c.getList().isEmpty());
                });
        placementsTable.getItems()
                .addListener((ListChangeListener<Tuple2<Item, Placement>>) c -> {
                    subButton.setDisable(c.getList().isEmpty());
                    addButton.setDisable(c.getList().isEmpty());
                });

        countSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0,Double.MAX_VALUE,1));
        countSpinner.getValueFactory().setConverter(THE_DOUBLE_CONVERTER);
        countSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> countSpinner.increment(0));

        itemNameColumn.setCellValueFactory(param -> param.getValue().getX().nameProperty());
        manufacturerColumn.setCellValueFactory(param -> param.getValue().getX().manufacturersProperty().toStringProperty());
        designationsColumn.setCellValueFactory(param -> param.getValue().getX().designationsStringProperty());
        quantityColumn.setCellValueFactory(param -> param.getValue().getY().countProperty().asObject());
        minimalColumn.setCellValueFactory(param -> param.getValue().getY().minCountProperty().asObject());
        purchasedColumn.setCellValueFactory(param -> param.getValue().getY().orderedProperty().asObject());
        serialColumn.setCellValueFactory(param -> param.getValue().getY().serialProperty());
        detailsColumn.setCellValueFactory(param -> param.getValue().getY().detailsProperty());
        locationColumn.setCellValueFactory(param -> param.getValue().getY().locationNameProperty());
        rowColColumn.setCellValueFactory(param -> param.getValue().getY().coordinatesProperty().toStringProperty());

        //placementsTable.setRowFactory(param -> {
        //    TableRow<Tuple2<Item,Placement>> row=new TableRow<>();
        //    row.setOnDragDetected();
        //    return row;
        //});
        placementsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        placementsTable.setOnDragEntered(event -> {
            placementsTable.setStyle("-fx-base:-fx-fg-blue;");
            event.consume();
        });
        placementsTable.setOnDragExited(event -> {
            placementsTable.setStyle("");
            event.consume();
        });
        placementsTable.setOnDragOver(event -> {
            if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
                if (currentDrag != null) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });
        placementsTable.setOnDragDropped(event -> {
            placementsTable.getItems().add(currentDrag);
            currentDrag=null;
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void addToAll(ActionEvent actionEvent) {
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"This action will add to all placements!", ButtonType.OK,ButtonType.CANCEL);
        alert.initOwner(stage);
        if(alert.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.CANCEL){
            for (Tuple2<Item, Placement> tuple :placementsTable.getItems()) {
                tuple.getY().setCount(tuple.getY().getCount()+countSpinner.getValue());
                mainController.model.logic.getItemsCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(tuple.getX().getId())), tuple.getX());
            }
        }
    }

    public void subFromAll(ActionEvent actionEvent) {
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"This action will subtract from all placements!", ButtonType.OK,ButtonType.CANCEL);
        alert.initOwner(stage);
        if(alert.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.CANCEL){
            for (Tuple2<Item, Placement> tuple :placementsTable.getItems()) {
                if(tuple.getY().getCount()<1){
                    Alert insufficient=new Alert(Alert.AlertType.WARNING,"Insufficient material!",ButtonType.OK);
                    insufficient.initOwner(stage);
                    insufficient.showAndWait();
                    return;
                }
            }
            for (Tuple2<Item, Placement> tuple :placementsTable.getItems()) {
                tuple.getY().setCount(tuple.getY().getCount()-countSpinner.getValue());
                mainController.model.logic.getItemsCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(tuple.getX().getId())), tuple.getX());
            }
        }
    }

    public void removeFromList(ActionEvent actionEvent) {
        new ArrayList<>(placementsTable.getSelectionModel().getSelectedItems())
                .forEach(itemPlacementTuple2 -> placementsTable.getItems().remove(itemPlacementTuple2));
    }

    private Stage stage;

    @Override
    public void initializeStage(Stage stage) {
        this.stage=stage;
        stage.setOnCloseRequest(event -> mainController.editors.remove(this));
        pinToggle.setSelected(stage.isAlwaysOnTop());
        pinToggle.selectedProperty().addListener((observable, oldValue, newValue) -> stage.setAlwaysOnTop(newValue));
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
