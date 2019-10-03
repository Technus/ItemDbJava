package com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.placementEditor;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Designation;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Location;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.designationEditor.DesignationEditorController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.dgs.dapc.itemDB.javafx.qr.ShowQRController;
import com.dgs.dapc.itemDB.javafx.spinnerValueFactory.DoubleInfinitySpinnerValueFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.bson.types.ObjectId;

import static com.dgs.dapc.itemDB.Utility.THE_DOUBLE_CONVERTER;

public class PlacementEditorController implements IWindowInitialize {
    public TextField nameInput;
    public TextField serialInput;
    public TextArea detailsInput;
    public TextField coordinatesInput;
    public NullCombo<Location> locationSelect;
    public Spinner<Double> quantitySpinner;
    public Spinner<Double> minimalSpinner;
    public Spinner<Double> orderedSpinner;
    public TableView<Designation> designationsList;
    public TableColumn<Designation,String> designationsNameColumn;
    public TableColumn<Designation,String> designationsDetailsColumn;
    public Button saveButton;
    public Button designationClear;
    public NullCombo<Designation> designationSelect;
    public Button designationAdd;
    public TextField itemName;
    public Button showQR;
    public Button printQR;
    public Button designationRemove;
    public Button saveAndCloseButton;
    public TextField imageInput;
    public ScrollPane imageScroll;
    public BorderPane imageBorder;
    public ImageView imageView;
    public ToggleButton pinToggle;
    public ToggleButton locationRegexp;
    public ToggleButton designationRegexp;

    private Placement placement,parent;
    private Item item;
    private boolean embedded;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.editors.add(this);
    }

    public void save(ActionEvent actionEvent) {
        parent.setFully(placement);
        if(parent.getExists()){
            if (item.getExists() && !embedded) {
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }else{
            if(item.placementsProperty().stream().noneMatch(placementTreeItem -> placementTreeItem.getValue()==parent)) {
                item.placementsProperty().add(new TreeItem<>(parent));
            }
            parent.setExists(true);
            placement.setExists(true);
            if(item.getExists() && !embedded){
                mainController.model.logic.getItemsCollection().
                        replaceOne((BasicDBObject) QueryBuilder.start().put("_id").is(item.getId()).get(), item);
            }
        }
    }

    public void setItemPlacement(Item item,Placement parent,boolean embeddedEditor){
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
        if(parent !=null && this.placement==null){
            this.parent = parent;
            this.placement= parent.cloneObjectFully();
            imageInput.textProperty().bindBidirectional(this.placement.pictureProperty());
            nameInput.textProperty().bindBidirectional(this.placement.nameProperty());
            serialInput.textProperty().bindBidirectional(this.placement.serialProperty());
            detailsInput.textProperty().bindBidirectional(this.placement.detailsProperty());
            coordinatesInput.setText(Utility.DOUBLE_LIST_CONVERTER.toString(this.placement.getCoordinates()));
            coordinatesInput.focusedProperty().addListener(event -> {
                try{
                    this.placement.setCoordinates(Utility.DOUBLE_LIST_CONVERTER.fromString(coordinatesInput.getText()));
                }catch (Exception e){
                    e.printStackTrace();
                    this.placement.coordinatesProperty().clear();
                    coordinatesInput.textProperty().set("");
                }
            });

            locationSelect.setRegexPredicate();
            locationSelect.setNullString("Deselect Location");
            locationSelect.setBackingList(Location.COLLECTION.readableAndSortableList);
            Platform.runLater(()->locationSelect.setNullableValue(this.placement.getLocation()));
            locationSelect.nullableValueProperty().addListener((observable, oldValue, newValue) -> placement.setLocation(newValue));

            {
                DoubleInfinitySpinnerValueFactory doubleSpinnerValueFactory =
                        new DoubleInfinitySpinnerValueFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, this.placement.getCount());
                doubleSpinnerValueFactory.setConverter(THE_DOUBLE_CONVERTER);
                quantitySpinner.setValueFactory(doubleSpinnerValueFactory);
            }
            quantitySpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                quantitySpinner.increment(0);
            });
            quantitySpinner.getValueFactory().setConverter(Utility.THE_DOUBLE_CONVERTER);
            quantitySpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue!=null){
                    this.placement.setCount(newValue);
                }
            });
            {
                SpinnerValueFactory.DoubleSpinnerValueFactory doubleSpinnerValueFactory = new SpinnerValueFactory.
                        DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, this.placement.getMinCount());
                doubleSpinnerValueFactory.setConverter(THE_DOUBLE_CONVERTER);
                minimalSpinner.setValueFactory(doubleSpinnerValueFactory);
            }
            minimalSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                minimalSpinner.increment(0);
            });
            minimalSpinner.getValueFactory().setConverter(Utility.THE_DOUBLE_CONVERTER);
            minimalSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue!=null){
                    this.placement.setMinCount(newValue);
                }
            });
            {
                SpinnerValueFactory.DoubleSpinnerValueFactory doubleSpinnerValueFactory = new SpinnerValueFactory.
                        DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, this.placement.getOrdered());
                doubleSpinnerValueFactory.setConverter(THE_DOUBLE_CONVERTER);
                orderedSpinner.setValueFactory(doubleSpinnerValueFactory);
            }
            orderedSpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                orderedSpinner.increment(0);
            });
            orderedSpinner.getValueFactory().setConverter(Utility.THE_DOUBLE_CONVERTER);
            orderedSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue!=null){
                    this.placement.setOrdered(newValue);
                }
            });

            designationsList.setItems(this.placement.designationsProperty());
            designationsList.setRowFactory(tv -> {
                TableRow<Designation> row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
                    if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY)
                        e.consume();
                });
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 && e.getButton()==MouseButton.SECONDARY && !row.isEmpty()) {
                        Designation rowData = row.getItem();
                        Utility.Window<DesignationEditorController> window=Utility.loadFXML(DesignationEditorController.class.getResource("DesignationEditor.fxml"),"Designation Editor",getStage());
                        window.controller.setMainController(mainController);
                        window.controller.setDesignation(rowData);
                        window.stage.show();
                        e.consume();
                    }
                });
                return row;
            });
            designationsNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
            designationsDetailsColumn.setCellValueFactory(param -> param.getValue().detailsProperty());
            designationSelect.setRegexPredicate();
            designationSelect.setNullString("Deselect Designation");
            designationSelect.setBackingList(Designation.COLLECTION.readableAndSortableList);
            designationAdd.disableProperty().bind(new BooleanBinding() {
                {
                    bind(designationSelect.nullableValueProperty());
                    bind(placement.designationsProperty());
                }
                @Override
                protected boolean computeValue() {
                    Designation designation=designationSelect.getNullableValue();
                    if(designation==null){
                        return true;
                    }
                    for (ObjectId id: placement.getDesignationsId()) {
                        if(designation.getId().equals(id)){
                            return true;
                        }
                    }
                    return false;
                }
            });
            designationClear.disableProperty().bind(new BooleanBinding() {
                {
                    bind(placement.designationsProperty());
                }
                @Override
                protected boolean computeValue() {
                    return placement.designationsProperty().size()==0;
                }
            });
            designationRemove.disableProperty().bind(new BooleanBinding() {
                {
                    bind(designationsList.getSelectionModel().selectedItemProperty());
                }
                @Override
                protected boolean computeValue() {
                    return designationsList.getSelectionModel().getSelectedItem()==null;
                }
            });
        }
        if(this.item!=null && this.placement!=null){
            imageView.imageProperty().bind(new ObjectBinding<Image>() {
                {
                    bind(PlacementEditorController.this.item.imageProperty());
                    bind(PlacementEditorController.this.placement.imageProperty());
                }
                @Override
                protected Image computeValue() {
                    return PlacementEditorController.this.placement.imageProperty().get()==null?
                            PlacementEditorController.this.item.imageProperty().get():
                            PlacementEditorController.this.placement.imageProperty().get();
                }
            });
        }
    }

    public void print(ActionEvent actionEvent) {
        PrintQR.print(parent.getDiscriminatedId());
    }

    public void show(ActionEvent actionEvent) {
        Utility.Window<ShowQRController> window=Utility.loadFXML(ShowQRController.class.getResource("ShowQR.fxml"),"QR View: "+parent.getDiscriminatedId().toString(),getStage());
        window.controller.setQrImage(parent.getDiscriminatedId());
        window.stage.show();
    }

    public void designationAdd(ActionEvent actionEvent) {
        Designation designation=designationSelect.getNullableValue();
        placement.designationsProperty().add(designation);
        designationSelect.setNullableValue(null);
    }

    public void designationClear(ActionEvent actionEvent) {
        placement.designationsProperty().clear();
    }

    public void removeDesignation(ActionEvent actionEvent) {
        placement.designationsProperty().remove(designationsList.getSelectionModel().getSelectedItem());
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
        designationRegexp.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                designationSelect.setRegexPredicate();
            }else {
                designationSelect.setPredicate();
            }
        });
        locationRegexp.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                locationSelect.setRegexPredicate();
            }else {
                locationSelect.setPredicate();
            }
        });
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void setImage(ActionEvent actionEvent) {
        imageInput.setText(Utility.selectImage(imageInput.getText()));
    }
}
