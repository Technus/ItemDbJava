package com.dgs.dapc.itemDB.javafx.main.tabs.items;

import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.github.technus.dbAdditions.mongoDB.pojo.Tuple2;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
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
    private MainController mainController;
    private ObservableList<Tuple2<Item,Placement>> selectedPlacements;

    public void setSelectedPlacements(List<Tuple2<Item,Placement>> selectedPlacements) {
        placementsTable.setItems(FXCollections.observableArrayList(selectedPlacements));
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

        //placementsTable.setRowFactory(param -> {
        //    TableRow<Tuple2<Item,Placement>> row=new TableRow<>();
        //    row.setOnDragDetected();
        //    return row;
        //});
        //placementsTable.setOnDragEntered(event -> {
        //    placementsTable.setStyle("-fx-base:-fx-fg-blue;");
        //    event.consume();
        //});
        //placementsTable.setOnDragExited(event -> {
        //    placementsTable.setStyle("");
        //    event.consume();
        //});
        //placementsTable.setOnDragOver(event -> {
        //    if(currentDrag!= ItemEditorController.this){
        //        if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
        //            if (currentDrag != null && dragStartPlacement != null) {
        //                event.acceptTransferModes(TransferMode.MOVE);
        //            }
        //        }
        //    }
        //    event.consume();
        //});
        //placementsTable.setOnDragDropped(event -> {
        //
        //});
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void addToAll(ActionEvent actionEvent) {
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"This action will add to all selected placements!", ButtonType.OK,ButtonType.CANCEL);
        if(alert.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.CANCEL){
            for (Tuple2<Item, Placement> tuple :selectedPlacements) {
                tuple.getY().setCount(tuple.getY().getCount()+countSpinner.getValue());
                mainController.model.logic.getItemsCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(tuple.getX().getId())), tuple.getX());
            }
        }
    }

    public void subFromAll(ActionEvent actionEvent) {
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"This action will subtract from all selected placements!", ButtonType.OK,ButtonType.CANCEL);
        if(alert.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.CANCEL){
            for (Tuple2<Item, Placement> tuple :selectedPlacements) {
                if(tuple.getY().getCount()<1){
                    new Alert(Alert.AlertType.WARNING,"Insufficient material!",ButtonType.OK).showAndWait();
                    return;
                }
            }
            for (Tuple2<Item, Placement> tuple :selectedPlacements) {
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
