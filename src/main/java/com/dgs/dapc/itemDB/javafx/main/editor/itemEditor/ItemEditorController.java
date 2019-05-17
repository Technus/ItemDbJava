package com.dgs.dapc.itemDB.javafx.main.editor.itemEditor;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Contact;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.contactEditor.ContactEditorController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.placementEditor.PlacementEditorController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.sourceEditor.SourceEditorController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.tagValueEditor.TagValueEditorController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.dgs.dapc.itemDB.javafx.qr.ShowQRController;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;

import static com.dgs.dapc.itemDB.javafx.main.MainController.MAKE_COUNT_FORMATTED_TREE;
import static com.dgs.dapc.itemDB.javafx.main.MainController.MAKE_COUNT_TREE;

public class ItemEditorController implements IWindowInitialize {
    public static ItemEditorController dragStartEditor;
    public static TreeItem<Placement> dragStartPlacement;

    public TextField nameInput;
    public TextArea detailsInput;
    public TextField imageInput;
    public ImageView imageView;

    public TableView<Contact> manufacturersList;
    public TableColumn<Contact, String> manufacturersNameColumn;
    public TableColumn<Contact, String> manufacturersURLColumn;
    public TableColumn<Contact, String> manufacturersDetailsColumn;

    public TableView<TagValue> tagsList;
    public TableColumn<TagValue, String> tagsValueColumn;
    public TableColumn<TagValue, String> tagsNameColumn;

    public TreeTableView<Placement> placementsTree;
    public TreeTableColumn<Placement, String> placementsNameColumn;
    public TreeTableColumn<Placement, String> placementsLocationColumn;
    public TreeTableColumn<Placement, String> placementsCoordinatesColumn;
    public TreeTableColumn<Placement, Double> placementsQuantityColumn;
    public TreeTableColumn<Placement, Double> placementsMinimalColumn;
    public TreeTableColumn<Placement, Double> placementsPurchasedColumn;
    public TreeTableColumn<Placement, String> placementsSerialColumn;
    public TreeTableColumn<Placement, String> placementsDesignationsColumn;
    public TreeTableColumn<Placement, String> placementsDetailsColumn;

    public TreeTableView<Source> sourcesTree;
    public TreeTableColumn<Source, String> sourcesNameColumn;
    public TreeTableColumn<Source, String> sourcesURLColumn;
    public TreeTableColumn<Source, String> sourcesDetailsColumn;
    public TreeTableColumn<Source, String> sourcesContactColumn;
    public ToggleButton pinToggle;

    private MainController mainController;
    public Button saveButton;

    public Button manufacturerClear;
    public NullCombo<Contact> manufacturerSelect;
    public Button manufacturerAdd;

    public Button tagClear;
    public NullCombo<Tag> tagSelect;
    public Button tagAdd;
    public Button placementClear;
    public Button placementAdd;
    public Button sourceClear;
    public Button sourceAdd;
    public Button placementBasedOn;
    public Button sourceBasedOn;
    public Button showQR;
    public Button printQR;
    public Button manufacturerRemove;
    public Button placementRemove;
    public Button sourceRemove;
    public Button tagRemove;
    public Button saveAndCloseButton;
    public ScrollPane imageScroll;
    public BorderPane imageBorder;

    private Item item, parent;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(item);
        if (parent.getExists()) {
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(parent.getId())), parent);
        } else {
            mainController.model.logic.getItemsCollection().insertOne(parent);
            parent.setExists(true);
            item.setExists(true);
        }
    }

    public void setImage(ActionEvent actionEvent) {
        imageInput.setText(Utility.selectImage(imageInput.getText()));
    }

    public void setItem(Item parent) {
        if (parent != null && this.item == null) {
            this.parent = parent;
            this.item = parent.cloneObjectFully();
            saveAndCloseButton.styleProperty().bind(new StringBinding() {
                {
                    bind(ItemEditorController.this.parent.existsProperty());
                }

                @Override
                protected String computeValue() {
                    return ItemEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-bg-cyan;";
                }
            });
            saveButton.styleProperty().bind(new StringBinding() {
                {
                    bind(ItemEditorController.this.parent.existsProperty());
                }

                @Override
                protected String computeValue() {
                    return ItemEditorController.this.parent.getExists() ? "" : "-fx-base: -fx-mg-cyan;";
                }
            });
            nameInput.textProperty().bindBidirectional(this.item.nameProperty());
            detailsInput.textProperty().bindBidirectional(this.item.detailsProperty());
            imageInput.textProperty().bindBidirectional(this.item.pictureProperty());
            imageView.imageProperty().bind(this.item.imageProperty());

            manufacturersList.setItems(this.item.manufacturersProperty());
            manufacturersList.setRowFactory( tv -> {
                TableRow<Contact> row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                    if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                        e.consume();
                });
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                        Contact rowData = row.getItem();
                        Utility.Window<ContactEditorController> window=Utility.loadFXML(ContactEditorController.class.getResource("ContactEditor.fxml"),"Contact Editor: "+rowData.getId().toHexString());
                        window.controller.setMainController(mainController);
                        window.controller.setContact(rowData);
                        window.stage.show();
                        event.consume();
                    }
                });
                return row;
            });
            manufacturersNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
            manufacturersURLColumn.setCellValueFactory(param -> param.getValue().urlProperty().nameProperty());
            manufacturersURLColumn.setCellFactory(param -> {
                TextFieldTableCell<Contact,String> cell=new TextFieldTableCell<>();
                cell.setOnMouseClicked(event -> {
                    if(cell.getTableRow().getItem() instanceof Contact){
                        Contact contact=(Contact)cell.getTableRow().getItem();
                        if(contact.urlProperty().linkProperty().getValueSafe().length()>0){
                            mainController.hostServices.showDocument(contact.urlProperty().getLink());
                        }
                    }
                });
                cell.setStyle("-fx-text-fill:-fx-text-blue;");
                return cell;
            });
            manufacturersDetailsColumn.setCellValueFactory(param -> param.getValue().detailsProperty());

            tagsList.setItems(this.item.tagsProperty().readableAndSortableList);
            tagsList.setRowFactory(tv -> {
                TableRow<TagValue> row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                    if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                        e.consume();
                });
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                        TagValue rowData = row.getItem();
                        Utility.Window<TagValueEditorController> window = Utility.loadFXML(TagValueEditorController.class.getResource("TagValueEditor.fxml"), "Tag Value Editor");
                        window.controller.setMainController(mainController);
                        window.controller.setItemTagValue(this.item,rowData,true);
                        window.stage.show();
                        event.consume();
                    }
                });
                return row;
            });

            tagsNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());

            tagsValueColumn.setOnEditCommit(event -> {
                event.getRowValue().setValueFromString(event.getNewValue());
                tagsList.refresh();
            });
            tagsValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            tagsValueColumn.setCellValueFactory(param -> param.getValue().valueStringProperty());

            placementsTree.setShowRoot(false);
            placementsTree.setRoot(this.item.getTreeItemPlacements());
            placementsTree.setRowFactory(tv -> {
                TreeTableRow<Placement> row = new TreeTableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                    if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                        e.consume();
                });
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 &&event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                        Placement rowData = row.getItem();
                        Utility.Window<PlacementEditorController> window = Utility.loadFXML(PlacementEditorController.class.getResource("PlacementEditor.fxml"), "Placement / Product / Position Editor: " + rowData.getId().toHexString());
                        window.controller.setMainController(mainController);
                        window.controller.setItemPlacement(this.item,rowData,true);
                        window.stage.show();
                        event.consume();
                    }
                });
                row.setOnDragDetected(event -> {
                    if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
                        dragStartEditor = ItemEditorController.this;
                        dragStartPlacement = row.getTreeItem();
                        Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                        db.setDragView(row.snapshot(new SnapshotParameters(), null));
                        ClipboardContent content=new ClipboardContent();
                        content.putString("Placement");
                        db.setContent(content);
                    }else{
                        //todo ask to close and save placement editors?
                        dragStartEditor = null;
                        dragStartPlacement = null;
                    }
                    event.consume();
                });
                return row;
            });
            placementsTree.setOnDragEntered(event -> {
                placementsTree.setStyle("-fx-base:-fx-fg-blue;");
                event.consume();
            });
            placementsTree.setOnDragExited(event -> {
                placementsTree.setStyle("");
                event.consume();
            });
            placementsTree.setOnDragOver(event -> {
                if(dragStartEditor!=ItemEditorController.this){
                    if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
                        if (dragStartEditor != null && dragStartPlacement != null) {
                            event.acceptTransferModes(TransferMode.MOVE);
                        }
                    }
                }
                event.consume();
            });
            placementsTree.setOnDragDropped(event -> {
                if(dragStartEditor!=ItemEditorController.this){
                    if (mainController.editors.stream().noneMatch(o -> o instanceof PlacementEditorController)) {
                        if (dragStartEditor != null && dragStartPlacement != null) {
                            //todo ask to save or cancel operation?
                            if (dragStartEditor.parent.getExists() && parent.getExists()) {
                                dragStartEditor.placementsTree.getRoot().getChildren().remove(dragStartPlacement);
                                dragStartEditor.save(new ActionEvent(dragStartPlacement,dragStartEditor.placementsTree));
                                placementsTree.getRoot().getChildren().add(dragStartPlacement);
                                save(new ActionEvent(dragStartPlacement,placementsTree));
                            }
                        }
                    }
                    dragStartEditor = null;
                    dragStartPlacement = null;
                }
                event.consume();
            });

            placementsNameColumn.setOnEditStart(event -> {
                event.getRowValue().getValue().setName(event.getNewValue());
            });
            placementsNameColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            placementsNameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty());

            placementsLocationColumn.setCellValueFactory(param -> param.getValue().getValue().locationNameProperty());
            placementsCoordinatesColumn.setCellValueFactory(param -> param.getValue().getValue().coordinatesProperty().toStringProperty());

            placementsQuantityColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setCount(event.getNewValue());
                placementsTree.refresh();
            });
            placementsQuantityColumn.setCellValueFactory(param -> param.getValue().getValue().countProperty().asObject());
            placementsQuantityColumn.setCellFactory(MAKE_COUNT_FORMATTED_TREE);

            placementsMinimalColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setMinCount(event.getNewValue());
                placementsTree.refresh();
            });
            placementsMinimalColumn.setCellValueFactory(param -> param.getValue().getValue().minCountProperty().asObject());
            placementsMinimalColumn.setCellFactory(MAKE_COUNT_TREE);

            placementsPurchasedColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setOrdered(event.getNewValue());
                placementsTree.refresh();
            });
            placementsPurchasedColumn.setCellValueFactory(param -> param.getValue().getValue().orderedProperty().asObject());
            placementsPurchasedColumn.setCellFactory(MAKE_COUNT_TREE);
            placementsSerialColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setSerial(event.getNewValue());
            });
            placementsSerialColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            placementsSerialColumn.setCellValueFactory(param -> param.getValue().getValue().serialProperty());
            placementsDesignationsColumn.setCellValueFactory(param -> param.getValue().getValue().designationsProperty().toStringProperty());
            placementsDetailsColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setDetails(event.getNewValue());
            });
            placementsDetailsColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            placementsDetailsColumn.setCellValueFactory(param -> param.getValue().getValue().detailsProperty());

            sourcesTree.setShowRoot(false);
            sourcesTree.setRoot(this.item.getTreeItemSources());
            sourcesTree.setRowFactory(tv -> {
                TreeTableRow<Source> row = new TreeTableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                    if (e.getClickCount() == 2 && e.getButton()==MouseButton.PRIMARY)
                        e.consume();
                });
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 &&event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                        Source rowData = row.getItem();
                        Utility.Window<SourceEditorController> window = Utility.loadFXML(SourceEditorController.class.getResource("SourceEditor.fxml"), "Source Editor");
                        window.controller.setMainController(mainController);
                        window.controller.setItemSource(this.item,rowData,true);
                        window.stage.show();
                        event.consume();
                    }
                });
                return row;
            });
            sourcesNameColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setName(event.getNewValue());
            });
            sourcesNameColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            sourcesNameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty());

            sourcesURLColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setUrl(event.getNewValue());
                sourcesTree.refresh();
            });
            sourcesURLColumn.setCellValueFactory(param -> param.getValue().getValue().urlProperty().nameProperty());
            sourcesURLColumn.setCellFactory(param -> {
                TextFieldTreeTableCell<Source,String> cell=new TextFieldTreeTableCell<>();
                cell.setOnMouseClicked(event -> {
                    Source source= cell.getTreeTableRow().getItem();
                    if(source != null){
                        if(source.urlProperty().linkProperty().getValueSafe().length()>0){
                            mainController.hostServices.showDocument(source.urlProperty().getLink());
                        }
                    }
                });
                cell.setStyle("-fx-text-fill:-fx-text-blue;");
                return cell;
            });

            sourcesDetailsColumn.setOnEditCommit(event -> {
                event.getRowValue().getValue().setDetails(event.getNewValue());
            });
            sourcesDetailsColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
            sourcesDetailsColumn.setCellValueFactory(param -> param.getValue().getValue().detailsProperty());

            sourcesContactColumn.setCellValueFactory(param -> param.getValue().getValue().supplierNameProperty());

            manufacturerSelect.setRegexPredicate();
            manufacturerSelect.setNullString("Deselect Contact");
            manufacturerSelect.setBackingList(Contact.COLLECTION.readableAndSortableList);
            manufacturerAdd.disableProperty().bind(new BooleanBinding() {
                {
                    bind(manufacturerSelect.nullableValueProperty());
                    bind(item.manufacturersProperty());
                }

                @Override
                protected boolean computeValue() {
                    Contact contact = manufacturerSelect.getNullableValue();
                    if (contact == null) {
                        return true;
                    }
                    for (ObjectId id : item.getManufacturersId()) {
                        if (contact.getId().equals(id)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            manufacturerClear.disableProperty().bind(new BooleanBinding() {
                {
                    bind(item.manufacturersProperty());
                }

                @Override
                protected boolean computeValue() {
                    return item.manufacturersProperty().size() == 0;
                }
            });
            manufacturerRemove.disableProperty().bind(new BooleanBinding() {
                {
                    bind(manufacturersList.getSelectionModel().selectedItemProperty());
                }
                @Override
                protected boolean computeValue() {
                    return manufacturersList.getSelectionModel().getSelectedItem()==null;
                }
            });

            tagSelect.setRegexPredicate();
            tagSelect.setNullString("Deselect Tag");
            tagSelect.setBackingList(Tag.COLLECTION.readableAndSortableList);
            tagAdd.disableProperty().bind(new BooleanBinding() {
                {
                    bind(item.tagsProperty().map);
                    bind(tagSelect.nullableValueProperty());
                }

                @Override
                protected boolean computeValue() {
                    Tag tag = tagSelect.getNullableValue();
                    if (tag == null) {
                        return true;
                    }
                    for (ObjectId id : item.getTagsId()) {
                        if (id.equals(tag.getId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            tagClear.disableProperty().bind(new BooleanBinding() {
                {
                    bind(item.tagsProperty().map);
                }

                @Override
                protected boolean computeValue() {
                    return item.tagsProperty().map.size() == 0;
                }
            });
            tagRemove.disableProperty().bind(new BooleanBinding() {
                {
                    bind(tagsList.getSelectionModel().selectedItemProperty());
                }
                @Override
                protected boolean computeValue() {
                    return tagsList.getSelectionModel().getSelectedItem()==null;
                }
            });

            placementClear.disableProperty().bind(new BooleanBinding() {
                {
                    bind(item.placementsProperty());
                }

                @Override
                protected boolean computeValue() {
                    return item.placementsProperty().size() == 0;
                }
            });
            placementBasedOn.disableProperty().bind(new BooleanBinding() {
                {
                    bind(placementsTree.getSelectionModel().selectedItemProperty());
                }

                @Override
                protected boolean computeValue() {
                    return placementsTree.getSelectionModel().getSelectedItem() == null || placementsTree.getSelectionModel().getSelectedItem().getValue() == null;
                }
            });
            placementRemove.disableProperty().bind(new BooleanBinding() {
                {
                    bind(placementsTree.getSelectionModel().selectedItemProperty());
                }
                @Override
                protected boolean computeValue() {
                    return placementsTree.getSelectionModel().getSelectedItem()==null;
                }
            });

            sourceClear.disableProperty().bind(new BooleanBinding() {
                {
                    bind(item.sourcesProperty());
                }

                @Override
                protected boolean computeValue() {
                    return item.sourcesProperty().size() == 0;
                }
            });
            sourceBasedOn.disableProperty().bind(new BooleanBinding() {
                {
                    bind(sourcesTree.getSelectionModel().selectedItemProperty());
                }

                @Override
                protected boolean computeValue() {
                    return sourcesTree.getSelectionModel().getSelectedItem() == null || sourcesTree.getSelectionModel().getSelectedItem().getValue() == null;
                }
            });
            sourceRemove.disableProperty().bind(new BooleanBinding() {
                {
                    bind(sourcesTree.getSelectionModel().selectedItemProperty());
                }
                @Override
                protected boolean computeValue() {
                    return sourcesTree.getSelectionModel().getSelectedItem()==null;
                }
            });
        }
    }

    public void print(ActionEvent actionEvent) {
        PrintQR.print(parent.getDiscriminatedId());
    }

    public void show(ActionEvent actionEvent) {
        Utility.Window<ShowQRController> window = Utility.loadFXML(ShowQRController.class.getResource("ShowQR.fxml"), "QR View: " + parent.getDiscriminatedId().toString());
        window.controller.setQrImage(parent.getDiscriminatedId());
        window.stage.show();
    }

    public void manufacturerAdd(ActionEvent actionEvent) {
        Contact contact = manufacturerSelect.getNullableValue();
        item.manufacturersProperty().add(contact);
        manufacturerSelect.setNullableValue(null);
    }

    public void manufacturerClear(ActionEvent actionEvent) {
        item.manufacturersProperty().clear();
    }

    public void manufacturerRemove(ActionEvent actionEvent) {
        item.manufacturersProperty().remove(manufacturersList.getSelectionModel().getSelectedItem());
    }

    public void tagClear(ActionEvent actionEvent) {
        item.tagsProperty().map.clear();
    }

    @SuppressWarnings("unchecked")
    public void tagAdd(ActionEvent actionEvent) {
        TagValue tagValue = new TagValue(tagSelect.getNullableValue(), null);
        Utility.Window<TagValueEditorController> window = Utility.loadFXML(TagValueEditorController.class.getResource("TagValueEditor.fxml"), "TagValue Editor");
        window.controller.setMainController(mainController);
        window.controller.setItemTagValue(item,tagValue,true);
        if(tagValue.getTag().getType()==Void.class){
            window.controller.save(actionEvent);
        }else{
            window.stage.show();
        }
        tagSelect.setNullableValue(null);
    }

    public void tagRemove(ActionEvent actionEvent) {
        item.tagsProperty().map.remove(tagsList.getSelectionModel().getSelectedItem().getId());
    }

    public void placementClear(ActionEvent actionEvent) {
        item.placementsProperty().clear();
    }

    public void placementAdd(ActionEvent actionEvent) {
        Placement placement = new Placement();
        Utility.Window<PlacementEditorController> window = Utility.loadFXML(PlacementEditorController.class.getResource("PlacementEditor.fxml"), "Placement / Product / Position Editor: " + placement.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setItemPlacement(item,placement,true);
        window.stage.show();
    }

    public void placementBasedOn(ActionEvent actionEvent) {
        Placement placement = placementsTree.getSelectionModel().getSelectedItem().getValue().cloneObjectData();
        Utility.Window<PlacementEditorController> window = Utility.loadFXML(PlacementEditorController.class.getResource("PlacementEditor.fxml"), "Placement / Product / Position Editor: " + placement.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setItemPlacement(item,placement,true);
        window.stage.show();
    }

    public void placementRemove(ActionEvent actionEvent) {
        item.placementsProperty().remove(placementsTree.getSelectionModel().getSelectedItem());
    }

    public void sourceClear(ActionEvent actionEvent) {
        item.sourcesProperty().clear();
    }

    public void sourceAdd(ActionEvent actionEvent) {
        Utility.Window<SourceEditorController> window = Utility.loadFXML(SourceEditorController.class.getResource("SourceEditor.fxml"), "Source Editor");
        window.controller.setMainController(mainController);
        window.controller.setItemSource(item,new Source(),true);
        window.stage.show();
    }

    public void sourceBasedOn(ActionEvent actionEvent) {
        Source source = sourcesTree.getSelectionModel().getSelectedItem().getValue().cloneObjectData();
        Utility.Window<SourceEditorController> window = Utility.loadFXML(SourceEditorController.class.getResource("SourceEditor.fxml"), "Source Editor");
        window.controller.setMainController(mainController);
        window.controller.setItemSource(item,source,true);
        window.stage.show();
    }

    public void sourceRemove(ActionEvent actionEvent) {
        item.sourcesProperty().remove(sourcesTree.getSelectionModel().getSelectedItem());
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
