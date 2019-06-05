package com.dgs.dapc.itemDB.javafx.main.tabs.items;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.DoubleSI;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.*;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.ItemEditorController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.placementEditor.PlacementEditorController;
import com.dgs.dapc.itemDB.javafx.main.tabs.util.UtilTabController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.MultiCombo;
import com.github.technus.dbAdditions.mongoDB.pojo.Tuple2;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.UnwindOptions;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.dgs.dapc.itemDB.javafx.main.MainController.MAKE_COUNT_FORMATTED_TREE;
import static com.dgs.dapc.itemDB.javafx.main.MainController.MAKE_COUNT_TREE;
import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Projections.exclude;

public class ItemsTabController implements Initializable {
    public MainController mainController;

    public TreeTableView<Object> itemsTree;
    public TreeTableColumn<INamed,String> itemsNameColumn;
    public TreeTableColumn<IStockState,Double> itemsCountColumn;
    public TreeTableColumn<IStockState,Double> itemsMinCountColumn;
    public TreeTableColumn<IStockState,Double> itemsOrdered;
    public TreeTableColumn<ISerialState,String> itemsSerialColumn;
    public TreeTableColumn<IDetailed,String> itemsDetailsColumn;
    public TreeTableColumn<Object,String> itemsCoordinatesColumn;
    public TreeTableColumn<Object,String> itemsLocationColumn;
    public TreeTableColumn<Object,Object> itemsCountParentColumn;
    public TreeTableColumn<Object,Object> itemsTagsParentColumn;
    public TreeTableColumn<Object,String> itemsTagsColumn;
    public TreeTableColumn<Object,String> itemsDesignationColumn;
    public TreeTableColumn<Object,String> itemsManufacturerColumn;

    public Spinner<Integer> spinnerPerPage;
    public Spinner<Integer> spinnerPage;
    public Pagination pagination;
    public ToggleButton serialRegExp;
    public ToggleButton nameRegExp;
    public TextField nameQueryInput;
    public TextField serialQueryInput;
    public MultiCombo<Tag> containsTagQueryInput;
    public MultiCombo<Location> containedInLocationQueryInput;
    public MultiCombo<Designation> containsDesignationQueryInput;
    public MultiCombo<Contact> containsSourceQueryInput;
    public TextField qrLinkInput;
    public Button newBasedOnButton;
    public ComboBox<UtilTabController.ModeQR> qrModeSelect;
    public Button removeButton;
    public TextField genericQueryInput;
    public ToggleButton genericRegExp;
    public ToggleButton needsOrdering;
    public ToggleButton stockLow;
    public Label countLabel;
    public ToggleButton expandToggle;
    public ImageView camImage;
    public ToggleButton scanEnable;


    private ObservableList<Bson> queryList = FXCollections.observableArrayList();
    private ObservableList<Bson> sortingList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        scanEnable.textProperty().bind(new StringBinding() {
            {
                bind(scanEnable.selectedProperty());
            }
            @Override
            protected String computeValue() {
                return scanEnable.isSelected()?"On":"Off";
            }
        });

        stockLow.setOnAction(event -> {
            if(stockLow.isSelected()) needsOrdering.setSelected(false);
        });
        needsOrdering.setOnAction(event -> {
            if(needsOrdering.isSelected()) stockLow.setSelected(false);
        });

        qrModeSelect.setItems(new ObservableListWrapper<>(Arrays.asList(UtilTabController.ModeQR.values())));
        qrModeSelect.setValue(UtilTabController.ModeQR.QUERY);

        SpinnerValueFactory.IntegerSpinnerValueFactory integerSpinnerValueFactory1 =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 50);
        integerSpinnerValueFactory1.setConverter(Utility.getSafeIntegerConverter(() -> 50));
        spinnerPerPage.setValueFactory(integerSpinnerValueFactory1);
        spinnerPerPage.focusedProperty().addListener((observable, oldValue, newValue) -> spinnerPerPage.increment(0));

        SpinnerValueFactory.IntegerSpinnerValueFactory integerSpinnerValueFactory2 =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        integerSpinnerValueFactory2.setConverter(Utility.getSafeIntegerConverter(() -> 1));
        spinnerPage.setValueFactory(integerSpinnerValueFactory2);
        spinnerPage.focusedProperty().addListener((observable, oldValue, newValue) -> spinnerPage.increment(0));

        spinnerPage.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                if (newValue <= 0) {
                    pagination.setCurrentPageIndex(0);
                }
                pagination.setCurrentPageIndex(newValue - 1);
            }
        });
        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                spinnerPage.getEditor().setText(Integer.toString(pagination.getCurrentPageIndex() + 1));
            }
        });

        queryList.addListener((InvalidationListener) observable -> pagination.setCurrentPageIndex(0));

        countLabel.textProperty().bind(new StringBinding() {
            {
                bind(pagination.currentPageIndexProperty());
                bind(spinnerPerPage.valueProperty());
                bind(queryList);
            }
            @Override
            protected String computeValue() {
                if (mainController != null) {
                    ArrayList<Bson> aggregation = new ArrayList<>(queryList);
                    aggregation.add(count());
                    MongoCollection<Item> collection = mainController.model.logic.getItemsCollection();
                    Document doc=collection.withDocumentClass(Document.class).aggregate(aggregation).allowDiskUse(true)
                            .collation(mainController.model.logic.getCollation()).first();
                    long count=doc==null?0:doc.getInteger("count");
                    long pos=spinnerPerPage.getValue() * pagination.getCurrentPageIndex();
                    return (pos+1)+" - " + (pos+spinnerPerPage.getValue()) +" / "+ count;
                }
                return null;
            }
        });

        itemsTree.rootProperty().set(Item.createPlacementsPageRoot());

        expandToggle.selectedProperty().addListener((observable, oldValue, newValue) ->
                Utility.setExpandRecursively(itemsTree.getRoot(),newValue));

        itemsTree.setSortPolicy(param -> setSortFromColumns());//disable java sorting
        itemsTree.rootProperty().get().valueProperty().bind(new ObjectBinding<Object>() {
            {
                bind(pagination.currentPageIndexProperty());
                bind(spinnerPerPage.valueProperty());
                bind(queryList);
                bind(sortingList);
                bind(itemsTree.getSortOrder());
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Object computeValue() {
                if (mainController != null) {
                    if(queryList.size()>0){
                        ArrayList<Bson> aggregation = new ArrayList<>();
                        aggregation.addAll(queryList);
                        aggregation.addAll(sortingList);
                        aggregation.add(Aggregates.skip(spinnerPerPage.getValue() * pagination.getCurrentPageIndex()));
                        aggregation.add(Aggregates.limit(spinnerPerPage.getValue()));
                        List<TreeItem<Object>> list=new ArrayList<>();
                        for (Item item : mainController.model.logic.getItemsCollection()
                                .aggregate(aggregation).allowDiskUse(true).collation(mainController.model.logic.getCollation())) {
                            list.add(item.getTreeItemPlacements());
                        }
                        itemsTree.rootProperty().get().getChildren().setAll(list);
                        Utility.setExpandRecursively(itemsTree.getRoot(),expandToggle.isSelected());
                    }else{
                        itemsTree.rootProperty().get().getChildren().clear();
                    }
                }

                return Item.createPlacementsPageRoot().getValue();
            }
        });
        itemsTree.setShowRoot(false);
        itemsTree.setRowFactory(param -> {
            TreeTableRow<Object> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.getValue() instanceof Placement) {
                    row.setStyle("-fx-control-inner-background:-fx-control-inner-background-alt;");
                } else if(newValue!=null && newValue.getValue() instanceof Item) {
                    row.setStyle("");
                } else {
                    row.setStyle("-fx-control-inner-background:-fx-bg-darkBlue;");
                }
            });
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                    e.consume();
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                    openEditor(row.getTreeItem());
                    event.consume();
                }
            });

            row.setOnDragDetected(event -> {
                if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController) &&
                        row.getTreeItem().getValue() instanceof Placement) {
                    Item item = (Item)row.getTreeItem().getParent().getValue();
                    Placement placement = (Placement) row.getTreeItem().getValue();
                    ChangesOverviewController.currentDrag=new Tuple2<>(item,placement);
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(new SnapshotParameters(), null));
                    ClipboardContent content=new ClipboardContent();
                    content.putString("Placement");
                    db.setContent(content);
                }else{
                    //todo ask to close and save placement editors?
                    ChangesOverviewController.currentDrag=null;
                }
                event.consume();
            });
            row.setOnDragEntered(event -> {
                row.setStyle("-fx-base:-fx-fg-blue;");
                event.consume();
            });
            row.setOnDragExited(event -> {
                row.setStyle("");
                event.consume();
            });
            row.setOnDragOver(Event::consume);//don't accept
            row.setOnDragDropped(event -> {
                ChangesOverviewController.currentDrag=null;
                event.consume();
            });//no action

            //row.setOnDragDetected(event -> {
            //    if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController) &&
            //            row.getTreeItem().getValue() instanceof Placement) {
            //        dragStartItem = (TreeItem)row.getTreeItem().getParent();
            //        dragStartPlacement = (TreeItem) row.getTreeItem();
            //        Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            //        db.setDragView(row.snapshot(new SnapshotParameters(), null));
            //        ClipboardContent content=new ClipboardContent();
            //        content.putString("Placement");
            //        db.setContent(content);
            //    }else{
            //        //todo ask to close and save placement editors?
            //        dragStartItem = null;
            //        dragStartPlacement = null;
            //    }
            //    event.consume();
            //});
            //row.setOnDragEntered(event -> {
            //    row.setStyle("-fx-base:-fx-fg-blue;");
            //    event.consume();
            //});
            //row.setOnDragExited(event -> {
            //    row.setStyle("");
            //    event.consume();
            //});
            //row.setOnDragOver(event -> {
            //    if(dragStartItem != (TreeItem)row.getTreeItem().getParent()){
            //        if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
            //            if (dragStartItem != null && dragStartPlacement != null) {
            //                event.acceptTransferModes(TransferMode.MOVE);
            //            }
            //        }
            //    }
            //    event.consume();
            //});
            //row.setOnDragDropped(event -> {
            //    if(dragStartItem != (TreeItem)row.getTreeItem().getParent()){
            //        if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
            //            if (dragStartItem != null && dragStartPlacement != null) {
            //                //todo ask to save or cancel operation?
            //                if (dragStartItem.getValue().getExists() && row.getTreeItem().getValue().getExists()) {
            //                    dragStartItem.placementsTree.getRoot().getChildren().remove(dragStartPlacement);
            //                    dragStartItem.save(new ActionEvent(dragStartPlacement, dragStartItem.placementsTree));
            //                    placementsTree.getRoot().getChildren().add(dragStartPlacement);
            //                    save(new ActionEvent(dragStartPlacement,placementsTree));
            //                }
            //            }
            //        }
            //        dragStartItem = null;
            //        dragStartPlacement = null;
            //    }
            //    event.consume();
            //});
            return row;
        });

        itemsNameColumn.setOnEditCommit(event -> {
            INamed named=event.getRowValue().getValue();
            named.setName(event.getNewValue());
            Item item;
            if(named instanceof Item){
                item=(Item) named;
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
            }
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        itemsNameColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        itemsNameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty());

        itemsCountColumn.setOnEditCommit(event -> {
            IStockState obj=event.getRowValue().getValue();
            Item item;
            if(obj instanceof Item){
                item=(Item) obj;
                if(event.getNewValue()==0D){
                    ((TreeItem<?>)item.getTreeItemPlacements()).getChildren().forEach(o -> {
                        if(o.getValue() instanceof Placement){
                            ((Placement) o.getValue()).setCount(0D);
                        }
                    });
                }else {
                    itemsTree.refresh();
                }
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
                ((Placement) obj).setCount(event.getNewValue());
            }
            itemsTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        itemsCountColumn.setCellValueFactory(param -> new DoubleBinding() {
            {
                bind(param.getValue().getValue().countProperty());
                bind(param.getValue().getValue().stockOptimalProperty());
            }

            @Override
            protected double computeValue() {
                return param.getValue().getValue().countProperty().get();
            }
        }.asObject());
        itemsCountColumn.setCellFactory(MAKE_COUNT_FORMATTED_TREE);

        itemsMinCountColumn.setOnEditCommit(event -> {
            IStockState obj=event.getRowValue().getValue();
            Item item;
            if(obj instanceof Item){
                item=(Item) obj;
                if(event.getNewValue()==0D){
                    ((TreeItem<?>)item.getTreeItemPlacements()).getChildren().forEach(o -> {
                        if(o.getValue() instanceof Placement){
                            ((Placement) o.getValue()).setMinCount(0D);
                        }
                    });
                }else return;
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
                ((Placement) obj).setMinCount(event.getNewValue());
            }
            itemsTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        itemsMinCountColumn.setCellValueFactory(param -> param.getValue().getValue().minCountProperty().asObject());
        itemsMinCountColumn.setCellFactory(MAKE_COUNT_TREE);

        itemsOrdered.setOnEditCommit(event -> {
            IStockState obj=event.getRowValue().getValue();
            Item item;
            if(obj instanceof Item){
                item=(Item) obj;
                if(event.getNewValue()==0D){
                    ((TreeItem<?>)item.getTreeItemPlacements()).getChildren().forEach(o -> {
                        if(o.getValue() instanceof Placement){
                            ((Placement) o.getValue()).setOrdered(0D);
                        }
                    });
                }else return;
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
                ((Placement) obj).setOrdered(event.getNewValue());
            }
            itemsTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        itemsOrdered.setCellValueFactory(param -> param.getValue().getValue().orderedProperty().asObject());
        itemsOrdered.setCellFactory(MAKE_COUNT_TREE);

        itemsSerialColumn.setOnEditCommit(event -> {
            ISerialState obj=event.getRowValue().getValue();
            if (!(obj instanceof Item)) {
                Item item=(Item)event.getRowValue().getParent().getValue();
                ((Placement) obj).setSerial(event.getNewValue());
                itemsTree.refresh();
                mainController.model.logic.getItemsCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
            }
        });
        itemsSerialColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        itemsSerialColumn.setCellValueFactory(param -> param.getValue().getValue().serialProperty());

        itemsDetailsColumn.setOnEditCommit(event -> {
            IDetailed detailed=event.getRowValue().getValue();
            detailed.setDetails(event.getNewValue());
            Item item;
            if(detailed instanceof Item){
                item=(Item) detailed;
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
            }
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        itemsDetailsColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        itemsDetailsColumn.setCellValueFactory(param -> param.getValue().getValue().detailsProperty());

        itemsCoordinatesColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof Placement) {
                return ((Placement) param.getValue().getValue()).coordinatesProperty().toStringProperty();
            } else if (param.getValue().getValue() instanceof Item) {
                return ((Item) param.getValue().getValue()).coordinatesStringProperty();
            }
            return null;
        });
        itemsLocationColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof Placement) {
                return ((Placement) param.getValue().getValue()).locationNameProperty();
            } else if (param.getValue().getValue() instanceof Item) {
                return ((Item) param.getValue().getValue()).placementsStringProperty();
            }
            return null;
        });
        itemsTagsColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof ITagged) {
                return ((ITagged) param.getValue().getValue()).tagsStringProperty();
            }
            return null;
        });
        itemsDesignationColumn.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof Placement) {
                return ((Placement) param.getValue().getValue()).designationsProperty().toStringProperty();
            } else if (param.getValue().getValue() instanceof Item) {
                return ((Item) param.getValue().getValue()).designationsStringProperty();
            }
            return null;
        });
        itemsManufacturerColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof Item){
                return ((Item) param.getValue().getValue()).manufacturersProperty().toStringProperty();
            }
            return null;
        });

        containsTagQueryInput.setRegexPredicate();
        containsTagQueryInput.setNullString("Deselect Tag");
        containsTagQueryInput.setBackingList(Tag.COLLECTION.readableAndSortableList);
        containsTagQueryInput.nullableValueProperty().addListener(new InvalidationListener() {
            private ArrayList<TreeTableColumn> column=new ArrayList<>();
            @Override
            public void invalidated(Observable observable) {
                if (containsTagQueryInput.nullableValueProperty().isEmpty()) {
                    column.forEach(col->col.setVisible(false));
                    column.clear();
                } else {
                    for (TreeTableColumn c :itemsTagsParentColumn.getColumns()) {
                        if(c.getUserData()==null) continue;
                        if(containsTagQueryInput.nullableValueProperty().contains(((Supplier<Tag>)c.getUserData()).get())){
                            column.add(c);
                            c.setVisible(true);
                        }
                    }
                }
            }
        });
        containsDesignationQueryInput.setRegexPredicate();
        containsDesignationQueryInput.setNullString("Deselect Designation");
        containsDesignationQueryInput.setBackingList(Designation.COLLECTION.readableAndSortableList);
        containedInLocationQueryInput.setRegexPredicate();
        containedInLocationQueryInput.setNullString("Deselect Location");
        containedInLocationQueryInput.setBackingList(Location.COLLECTION.readableAndSortableList);
        containedInLocationQueryInput.setResultModify(locations -> {
            new ArrayList<>(locations).forEach(loc-> {
                if(locations.contains(loc)){
                    locations.removeAll(loc.allChildren());
                }
            });
        });
        containsSourceQueryInput.setRegexPredicate();
        containsSourceQueryInput.setNullString("Deselect Contact");
        containsSourceQueryInput.setBackingList(Contact.COLLECTION.readableAndSortableList);

        qrLinkInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() == DiscriminatedObjectId.decSize) {
                try {
                    analyzeQR(new ActionEvent(this, qrLinkInput));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(()->qrLinkInput.setText(""));
            }
        });

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(itemsTree.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                TreeItem treeItem = itemsTree.getSelectionModel().getSelectedItem();
                return treeItem == null || !(treeItem.getValue() instanceof Item);
            }
        });
        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(itemsTree.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                TreeItem treeItem = itemsTree.getSelectionModel().getSelectedItem();
                return treeItem == null || treeItem.getValue()==null;
            }
        });

        itemsNameColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(itemsNameColumn, "name")));
        itemsCountParentColumn.setSortable(false);
        itemsCountColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumCount", new Document("$sum", "$placements.count"))));
            sort.add(Utility.sort(itemsCountColumn, "sumCount"));
            sort.add(project(exclude("sumCount")));
        });
        itemsMinCountColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumMinCount", new Document("$sum", "$placements.minCount"))));
            sort.add(Utility.sort(itemsMinCountColumn, "sumMinCount"));
            sort.add(project(exclude("sumMinCount")));
        });
        itemsOrdered.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumOrdered", new Document("$sum", "$placements.ordered"))));
            sort.add(Utility.sort(itemsOrdered, "sumOrdered"));
            sort.add(project(exclude("sumOrdered")));
        });
        itemsSerialColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(itemsSerialColumn, "placements.serial")));
        itemsDetailsColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(itemsDetailsColumn, "details")));
        itemsCoordinatesColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(itemsCoordinatesColumn, "placements.coordinates")));
        itemsLocationColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(unwind("$placements", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to many
            sort.add(lookup("itemsBlob", "placements.locationId", "_id", "locations"));
            sort.add(unwind("$locations", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to 1
            sort.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    first("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    push("placements", "$placements"),
                    first("tags", "$tags"),
                    push("locations","$locations")));
            sort.add(Utility.sort(itemsLocationColumn, "locations.name"));
            sort.add(project(exclude("locations")));
        });
        itemsTagsParentColumn.setSortable(false);
        itemsTagsColumn.setSortable(false);
        itemsDesignationColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("placementsCopy", "$placements")));
            sort.add(unwind("$placementsCopy", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to many
            sort.add(unwind("$placementsCopy.designationsId", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to many
            sort.add(lookup(mainController.model.logic.getItemsCollection().getNamespace().getCollectionName(),
                    "placementsCopy.designationsId", "_id", "designations"));
            sort.add(unwind("$designations", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to 1
            sort.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    first("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    first("placements", "$placements"),
                    first("tags", "$tags"),
                    push("designations","$designations")));
            sort.add(Utility.sort(itemsDesignationColumn, "designations.name"));
            sort.add(project(exclude("designations")));
        });
        itemsManufacturerColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(unwind("$manufacturersId", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to many
            sort.add(lookup(mainController.model.logic.getItemsCollection().getNamespace().getCollectionName(),
                    "manufacturersId", "_id", "manufacturers"));
            sort.add(unwind("$manufacturers", new UnwindOptions().preserveNullAndEmptyArrays(true)));//1 to 1
            sort.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    push("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    first("placements", "$placements"),
                    first("tags", "$tags"),
                    push("manufacturers","$manufacturers")));
            sort.add(Utility.sort(itemsManufacturerColumn, "manufacturers.name"));
            sort.add(project(exclude("manufacturers")));
        });
    }

    public void reloadAll() {
        queryList.setAll(Aggregates.match(Utility.queryForClass(Item.class)));
    }

    public void runSimpleQuery(){
        QueryBuilder queryBuilder=QueryBuilder.start();
        if(genericQueryInput.getText()!=null && genericQueryInput.getText().length()>0){
            QueryBuilder orQuery=QueryBuilder.start();
            Pattern _pattern,pattern;
            if(genericRegExp.isSelected()){
                try{
                    _pattern=Utility.getPattern(genericQueryInput.getText());
                }catch (PatternSyntaxException e){
                    _pattern=Pattern.compile("(?i)"+ Pattern.quote(genericQueryInput.getText()));
                    genericRegExp.setSelected(false);
                }
            }else{
                _pattern=Pattern.compile("(?i)"+ Pattern.quote(genericQueryInput.getText()));
            }
            pattern=_pattern;

            List<ObjectId> contacts=Contact.COLLECTION.map.values().parallelStream()
                    .filter(contact -> pattern.matcher(contact.getName()).find()).map(Contact::getId).collect(Collectors.toList());
            if(contacts.size()>0) {
                orQuery.or(QueryBuilder.start("sources.supplierId").in(contacts).get());
                orQuery.or(QueryBuilder.start("manufacturersId").in(contacts).get());
            }
            orQuery.or(QueryBuilder.start("name").regex(pattern).get());
            orQuery.or(QueryBuilder.start("details").regex(pattern).get());
            orQuery.or(QueryBuilder.start("placements.name").regex(pattern).get());
            orQuery.or(QueryBuilder.start("placements.details").regex(pattern).get());
            orQuery.or(QueryBuilder.start("placements.serial").regex(pattern).get());
            orQuery.or(QueryBuilder.start("sources.name").regex(pattern).get());
            orQuery.or(QueryBuilder.start("sources.details").regex(pattern).get());
            orQuery.or(QueryBuilder.start("tags.value").regex(pattern).get());

            Double val = DoubleSI.INSTANCE.fromStringOrNull(genericQueryInput.getText());
            if(val!=null){
                orQuery.or(QueryBuilder.start("tags.value").is(val).get());
            }

            queryBuilder.and(orQuery.get());
        }
        if(nameQueryInput.getText()!=null && nameQueryInput.getText().length()>0){
            QueryBuilder orQuery=QueryBuilder.start();
            Pattern pattern;
            if(nameRegExp.isSelected()){
                try{
                    pattern=Utility.getPattern(nameQueryInput.getText());
                }catch (PatternSyntaxException e){
                    pattern=Pattern.compile("(?i)"+ Pattern.quote(nameQueryInput.getText()));
                    nameRegExp.setSelected(false);
                }
            }else{
                pattern=Pattern.compile("(?i)"+ Pattern.quote(nameQueryInput.getText()));
            }
            orQuery.or(QueryBuilder.start("name").regex(pattern).get());
            orQuery.or(QueryBuilder.start("sources.name").regex(pattern).get());
            orQuery.or(QueryBuilder.start("placements.name").regex(pattern).get());
            queryBuilder.and(orQuery.get());
        }
        if(serialQueryInput.getText()!=null && serialQueryInput.getText().length()>0){
            if(serialRegExp.isSelected()){
                try {
                    queryBuilder.and(QueryBuilder.start("placements.serial").regex(Utility.getPattern(serialQueryInput.getText())).get());
                }catch (PatternSyntaxException e){
                    queryBuilder.and(QueryBuilder.start("placements.serial").regex(Pattern.compile("(?i)"+ Pattern.quote(serialQueryInput.getText()))).get());
                    serialRegExp.setSelected(false);
                }
            }else{
                queryBuilder.and(QueryBuilder.start("placements.serial").regex(Pattern.compile("(?i)"+ Pattern.quote(serialQueryInput.getText()))).get());
            }
        }
        appendContains(queryBuilder);
        queryBuilder.and(Utility.queryForClass(Item.class));

        List<Bson> query=new ArrayList<>();

        query.add(match((BasicDBObject)queryBuilder.get()));

        if(needsOrdering.isSelected()){
            query.add(unwind("$placements", new UnwindOptions().preserveNullAndEmptyArrays(true)));
            query.add(addFields(new Field<>("toOrder",
                    new Document("$subtract",
                            Arrays.asList(new Document("$sum", Arrays.asList(
                                    "$placements.count",
                                    "$placements.ordered")),
                                    "$placements.minCount")))));
            query.add(match(lt("toOrder", 0L)));
            query.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    first("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    push("placements", "$placements"),
                    first("tags", "$tags")));
        }else if(stockLow.isSelected()){
            query.add(unwind("$placements", new UnwindOptions().preserveNullAndEmptyArrays(true)));
            query.add(addFields(new Field<>("diff",
                    new Document("$subtract",
                            Arrays.asList("$placements.count","$placements.minCount")))));
            query.add(match(lt("diff", 0L)));
            query.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    first("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    push("placements", "$placements"),
                    first("tags", "$tags")));
        }

        queryList.setAll(query);
    }

    public void appendContains(QueryBuilder queryBuilder){
        if(!containsTagQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("tags.tag").in(containsTagQueryInput.nullableValueProperty().stream().map(IIdentifiable::getId).collect(Collectors.toList())).get());
        }
        if(!containedInLocationQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("placements.locationId").in(containedInLocationQueryInput.nullableValueProperty().stream()
                    .flatMap(location -> location.withAllChildren().stream().map(Location::getId)).collect(Collectors.toSet())).get());
        }
        if(!containsDesignationQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("placements.designationsId").in(containsDesignationQueryInput.nullableValueProperty().stream().map(IIdentifiable::getId).collect(Collectors.toList())).get());
        }
        if(!containsSourceQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("sources.supplierId").in(containsSourceQueryInput.nullableValueProperty().stream().map(IIdentifiable::getId).collect(Collectors.toList())).get());
        }
    }

    public void clearRecords(){
        queryList.clear();
    }

    public void reloadRecords(){
        queryList.setAll(new ArrayList<>(queryList));
    }

    public TreeItem matchPlacement(TreeItem o) {
        if (o.getValue() instanceof Item) {
            List<TreeItem> placements = new ArrayList<>();
            ((Item) o.getValue()).placementsProperty().forEach(placementTreeItem -> {
                if ((containedInLocationQueryInput.isNullSelected() || containedInLocationQueryInput.nullableValueProperty().stream().anyMatch(location -> location.getId().equals(placementTreeItem.getValue().getLocationId()))) &&
                    (containsDesignationQueryInput.isNullSelected() || placementTreeItem.getValue().getDesignationsId().stream().anyMatch(objectId -> containsDesignationQueryInput.nullableValueProperty().stream().anyMatch(designation -> designation.getId().equals(objectId))))) {
                    placements.add(placementTreeItem);
                }
            });
            if (placements.size() == 1) {
                return placements.get(0);
            }else{
                Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot find matching placement!");
                alert.initOwner(mainController.getStage());
                alert.showAndWait();
            }
        }
        return null;
    }

    public void doActionQR(TreeItem o){
        if(o==null){
            return;
        }
        switch (qrModeSelect.getValue()){
            case ADD:{
                if(o.getValue() instanceof Placement){
                    if(o.getParent().getValue() instanceof Item) {
                        ((Placement) o.getValue()).setCount(((Placement) o.getValue()).getCount() + 1D);
                        mainController.model.logic.getItemsCollection().replaceOne(
                                new Document("_id",((Item) o.getParent().getValue()).getId()),(Item)o.getParent().getValue());
                    }
                }else if(o.getValue() instanceof Item){
                    doActionQR(matchPlacement(o));
                }
                break;
            }
            case SUB:{
                if(o.getValue() instanceof Placement){
                    if(((Placement) o.getValue()).getCount()<1) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Not enough!");
                        alert.initOwner(mainController.getStage());
                        alert.showAndWait();
                    }
                    if(o.getParent().getValue() instanceof Item) {
                        ((Placement) o.getValue()).setCount(((Placement) o.getValue()).getCount() - 1D);
                        mainController.model.logic.getItemsCollection().replaceOne(
                                new Document("_id",((Item) o.getParent().getValue()).getId()),(Item)o.getParent().getValue());
                    }
                }else if(o.getValue() instanceof Item){
                    doActionQR(matchPlacement(o));
                }
                break;
            }
            case SUB_ALL:{
                if(o.getValue() instanceof Placement){
                    if(o.getParent().getValue() instanceof Item) {
                        ((Placement) o.getValue()).setCount(0D);
                        mainController.model.logic.getItemsCollection().replaceOne(
                                new Document("_id",((Item) o.getParent().getValue()).getId()),(Item)o.getParent().getValue());
                    }
                }else if(o.getValue() instanceof Item){
                    doActionQR(matchPlacement(o));
                }
                break;
            }
            case REMOVE:{
                if(o.getValue() instanceof Placement){
                    if(o.getParent().getValue() instanceof Item) {
                        Item item=((Item) o.getParent().getValue());
                        item.placementsProperty().remove(o);
                        mainController.model.logic.getItemsCollection().replaceOne(
                                new Document("_id",item.getId()),item);
                    }
                }else if(o.getValue() instanceof Item){
                    doActionQR(matchPlacement(o));
                }
                break;
            }
            case EDIT:{
                openEditor(o);
                break;
            }
        }
    }

    public void analyzeQR(ActionEvent actionEvent) {
        DiscriminatedObjectId did = new DiscriminatedObjectId(qrLinkInput.getText());
        switch (did.discriminator) {
            case Item.PREFIX: {
                QueryBuilder queryBuilder = QueryBuilder.start();
                appendContains(queryBuilder);
                queryBuilder.put("_id").is(did.id).and(Utility.queryForClass(Item.class));
                queryList.setAll(Aggregates.match((BasicDBObject) queryBuilder.get()));
                for (TreeItem<Object> itemTree : itemsTree.getRoot().getChildren()) {
                    if(itemTree.getValue() instanceof Item) {
                        itemTree.setExpanded(true);
                        itemsTree.getSelectionModel().select(itemTree);
                        Platform.runLater(()->doActionQR(itemsTree.getSelectionModel().getSelectedItem()));
                        break;
                    }
                }
                break;
            }
            case Placement.PREFIX: {
                QueryBuilder queryBuilder = QueryBuilder.start();
                appendContains(queryBuilder);
                queryBuilder.put("placements._id").is(did.id).and(Utility.queryForClass(Item.class));
                queryList.setAll(Aggregates.match((BasicDBObject) queryBuilder.get()));
                lookup:
                for (TreeItem<Object> itemTree : itemsTree.getRoot().getChildren()) {
                    if(itemTree.getValue() instanceof Item) {
                        for (TreeItem<Object> placementTree : itemTree.getChildren()) {
                            if (placementTree.getValue() instanceof Placement && did.id.equals(((Placement) placementTree.getValue()).getId())) {
                                itemTree.setExpanded(true);
                                itemsTree.getSelectionModel().select(placementTree);
                                Platform.runLater(()->doActionQR(itemsTree.getSelectionModel().getSelectedItem()));
                                break lookup;
                            }
                        }
                    }
                }
                break;
            }
            case Location.PREFIX: {
                containedInLocationQueryInput.nullableValueProperty().setAll(Location.COLLECTION.getAndMakeIfMissing(did.id));
                runSimpleQuery();
                break;
            }
            case Tag.PREFIX: {
                containsTagQueryInput.nullableValueProperty().setAll(Tag.COLLECTION.getAndMakeIfMissing(did.id));
                runSimpleQuery();
                break;
            }
            case Designation.PREFIX: {
                containsDesignationQueryInput.nullableValueProperty().setAll(Designation.COLLECTION.getAndMakeIfMissing(did.id));
                runSimpleQuery();
                break;
            }
            case Contact.PREFIX:{
                containsSourceQueryInput.nullableValueProperty().setAll(Contact.COLLECTION.getAndMakeIfMissing(did.id));
                runSimpleQuery();
                break;
            }
            case UtilTabController.CLEAR_QUERY: {
                clearQuery(actionEvent);
                runSimpleQuery();
                break;
            }
            case UtilTabController.CLEAR_DESIGNATION:{
                containsDesignationQueryInput.nullableValueProperty().clear();
                runSimpleQuery();
                break;
            }
            case UtilTabController.CLEAR_LOCATION:{
                containedInLocationQueryInput.nullableValueProperty().clear();
                runSimpleQuery();
                break;
            }
            case UtilTabController.CLEAR_TAG:{
                containsTagQueryInput.nullableValueProperty().clear();
                runSimpleQuery();
                break;
            }
            case UtilTabController.CLEAR_CONTACT:{
                containsSourceQueryInput.nullableValueProperty().clear();
                runSimpleQuery();
                break;
            }
            default:{
                if(UtilTabController.MODE_QR_MAP.containsKey(did.discriminator)){
                    qrModeSelect.setValue(UtilTabController.MODE_QR_MAP.get(did.discriminator));
                }
            }
        }
    }

    public void openEditor(TreeItem o){
        if(o==null)return;
        Object rowObj=o.getValue();
        if (rowObj instanceof Item) {
            Utility.Window<ItemEditorController> window = Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"), "Item Editor: " + ((Item) rowObj).getId().toHexString(),mainController.getStage());
            window.controller.setMainController(mainController);
            window.controller.setItem((Item) rowObj);
            window.stage.show();
        } else if (rowObj instanceof Placement) {
            Utility.Window<PlacementEditorController> window = Utility.loadFXML(PlacementEditorController.class.getResource("PlacementEditor.fxml"), "Placement / Product / Position Editor",mainController.getStage());
            window.controller.setMainController(mainController);
            window.controller.setItemPlacement((Item) o.getParent().getValue(),(Placement) rowObj,false);
            window.stage.show();
        }
    }

    public void create(ActionEvent actionEvent) {
        Item item=new Item();
        Utility.Window<ItemEditorController> window=Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"),"Item Editor: "+item.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setItem(item);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Item item=((Item)itemsTree.getSelectionModel().getSelectedItem().getValue()).cloneObjectData();
        Utility.Window<ItemEditorController> window=Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"),"Item Editor: "+item.getId().toHexString(),mainController.getStage());
        window.controller.setMainController(mainController);
        window.controller.setItem(item);
        window.stage.show();
    }

    public boolean setSortFromColumns(){
        List<Bson> sort=new ArrayList<>();
        for (TreeTableColumn col:itemsTree.getSortOrder()) {
            if(col.getUserData() instanceof IAggregateModifier) {
                ((IAggregateModifier) col.getUserData()).modifyAggregate(sort);
            }
        }
        this.sortingList.setAll(sort);
        return true;
    }

    public void removeSelected(ActionEvent actionEvent) {
        TreeItem o=itemsTree.getSelectionModel().getSelectedItem();
        if(o.getValue() instanceof Placement){
            if(o.getParent().getValue() instanceof Item) {
                Item item=((Item) o.getParent().getValue());
                Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Remove "+o.getValue()+" ?");
                alert.initOwner(mainController.getStage());
                if(ButtonType.OK==alert.showAndWait().orElse(ButtonType.CANCEL)) {
                    item.placementsProperty().remove(o);
                    mainController.model.logic.getItemsCollection().replaceOne(
                            new Document("_id", item.getId()), item);
                }
            }
        }else if(o.getValue() instanceof Item){
            Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Remove "+o.getValue()+" ?");
            alert.initOwner(mainController.getStage());
            if(ButtonType.OK==alert.showAndWait().orElse(ButtonType.CANCEL)) {
                mainController.model.logic.getItemsCollection().deleteOne(new Document("_id", ((Item) o.getValue()).getId()));
                itemsTree.rootProperty().get().getChildren().remove(o);
            }
        }
    }

    public void clearQuery(ActionEvent actionEvent) {
        containedInLocationQueryInput.nullableValueProperty().clear();
        containsTagQueryInput.nullableValueProperty().clear();
        containsDesignationQueryInput.nullableValueProperty().clear();
        containsSourceQueryInput.nullableValueProperty().clear();
        serialQueryInput.setText(null);
        nameQueryInput.setText(null);
        genericQueryInput.setText(null);
    }

    public void editFound(ActionEvent actionEvent) {
        if (mainController != null && queryList.size()>0) {
            List<Designation> designation= new ArrayList<>(containsDesignationQueryInput.nullableValueProperty());
            if(designation.size()==0){
                designation=null;
            }

            ArrayList<Bson> aggregation = new ArrayList<>();
            aggregation.addAll(queryList);
            aggregation.addAll(sortingList);
            MongoCollection<Item> collection = mainController.model.logic.getItemsCollection();
            List<TreeItem<Object>> list=new ArrayList<>();
            for (Item item : collection.aggregate(aggregation).allowDiskUse(true).collation(mainController.model.logic.getCollation())) {
                list.add(item.getTreeItemPlacements());
            }
            if(list.size()==0) {
                return;
            }

            ArrayList<Tuple2<Item,Placement>> selectedPlacements=new ArrayList<>();

            for (TreeItem<Object> itemTreeItem:list) {
                if(itemTreeItem.getValue() instanceof Item){
                    Placement placementCandidate=null;
                    boolean foundWithDesignation=false;
                    for (TreeItem<Object> placementTreeItem:itemTreeItem.getChildren()) {
                        if(placementTreeItem.getValue() instanceof Placement){
                            if(designation!=null && ((Placement) placementTreeItem.getValue()).designationsProperty().stream().anyMatch(designation::contains)){
                                //any found with designation matching is ok
                                foundWithDesignation=true;
                            }else if(foundWithDesignation){
                                //only when there was no match with designation
                                continue;
                            }
                            if(placementCandidate==null){
                                //pick first
                                placementCandidate=(Placement)placementTreeItem.getValue();
                            }else if(placementCandidate.getCount()<((Placement)placementTreeItem.getValue()).getCount()){
                                //pick the one with more!
                                placementCandidate=(Placement)placementTreeItem.getValue();
                            }
                        }
                    }
                    if(placementCandidate!=null){
                        selectedPlacements.add(new Tuple2<>((Item)itemTreeItem.getValue(),placementCandidate));
                    }
                }
            }

            Utility.Window<ChangesOverviewController> changes=Utility.loadFXML(ChangesOverviewController.class.getResource("ChangesOverview.fxml"),"Bulk edit",mainController.getStage());
            changes.controller.setMainController(mainController);
            changes.controller.setSelectedPlacements(selectedPlacements);
            changes.stage.show();
        }
    }

    public void setMainController(MainController mainController){
        this.mainController=mainController;
        scanEnable.selectedProperty().bindBidirectional(mainController.scanEnableProperty());
    }

    public void setImage(BufferedImage image){
        Platform.runLater(()->{
            camImage.setImage(SwingFXUtils.toFXImage(image,null));
        });
    }
    public void setCode(String code){
        Platform.runLater(() -> {
            qrLinkInput.setText(code);
        });
    }

    public void configureCamera(ActionEvent actionEvent) {
        mainController.configureCameraAsync(actionEvent);
    }
}
