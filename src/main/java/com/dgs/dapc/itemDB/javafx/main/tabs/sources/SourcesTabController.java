package com.dgs.dapc.itemDB.javafx.main.tabs.sources;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.DoubleSI;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.*;
import com.dgs.dapc.itemDB.javafx.main.MainController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.ItemEditorController;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.sourceEditor.SourceEditorController;
import com.dgs.dapc.itemDB.javafx.nullComboBox.NullCombo;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.UnwindOptions;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.bson.BsonDocument;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.conversions.Bson;

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

//todo actually sources tree?
public class SourcesTabController implements Initializable {
    public MainController mainController;

    public TreeTableView<Object> sourcesTree;
    public TreeTableColumn<Object,String> sourcesTagsParentColumn;
    public TreeTableColumn<Object,String> sourcesTagsColumn;
    public TreeTableColumn<INamed,String> sourcesNameColumn;
    public TreeTableColumn<Object,String> sourcesCountParentColumn;
    public TreeTableColumn<Object,String> sourcesDesignationColumn;
    public TreeTableColumn<Object,Double> sourcesCountColumn;
    public TreeTableColumn<Object,Double> sourcesMinColumn;
    public TreeTableColumn<Object,Double> sourcesOrderedColumn;
    public TreeTableColumn<Object,String> sourcesLinkColumn;
    public TreeTableColumn<Object,String> sourcesSupplierColumn;
    public TreeTableColumn<Object,String> sourcesSerialColumn;
    public TreeTableColumn<IDetailed,String> sourcesDetailsColumn;
    public TreeTableColumn<Object,String> sourcesManufacturerSupplierColumn;

    public Spinner<Integer> spinnerPage;
    public Spinner<Integer> spinnerPerPage;
    public Pagination pagination;

    public ToggleButton nameRegExp;
    public ToggleButton serialRegExp;
    public TextField serialQueryInput;
    public TextField nameQueryInput;
    public NullCombo<Tag> containsTagQueryInput;
    public NullCombo<Location> containedInLocationQueryInput;
    public NullCombo<Designation> containsDesignationQueryInput;
    public NullCombo<Contact> containsSourceQueryInput;
    public TextField qrLinkInput;
    public Button newBasedOnButton;
    public Button removeButton;
    public TextField genericQueryInput;
    public ToggleButton genericRegExp;
    public ToggleButton needsOrdering,stockLow;
    public Label countLabel;
    public ToggleButton expandToggle;

    private ObservableList<Bson> queryList = FXCollections.observableArrayList();
    private ObservableList<Bson> sortingList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        stockLow.setOnAction(event -> {
            if(stockLow.isSelected()) needsOrdering.setSelected(false);
        });
        needsOrdering.setOnAction(event -> {
            if(needsOrdering.isSelected()) stockLow.setSelected(false);
        });

        SpinnerValueFactory.IntegerSpinnerValueFactory integerSpinnerValueFactory1=
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1,Integer.MAX_VALUE,50);
        integerSpinnerValueFactory1.setConverter(Utility.getSafeIntegerConverter(()->50));
        spinnerPerPage.setValueFactory(integerSpinnerValueFactory1);
        spinnerPerPage.focusedProperty().addListener((observable, oldValue, newValue) -> spinnerPerPage.increment(0));

        SpinnerValueFactory.IntegerSpinnerValueFactory integerSpinnerValueFactory2=
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1,Integer.MAX_VALUE,1);
        integerSpinnerValueFactory2.setConverter(Utility.getSafeIntegerConverter(()->1));
        spinnerPage.setValueFactory(integerSpinnerValueFactory2);
        spinnerPage.focusedProperty().addListener((observable, oldValue, newValue) -> spinnerPage.increment(0));

        spinnerPage.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(!Objects.equals(newValue, oldValue)) {
                if (newValue <= 0) {
                    pagination.setCurrentPageIndex(0);
                }
                pagination.setCurrentPageIndex(newValue-1);
            }
        });
        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) ->{
            if(!Objects.equals(newValue,oldValue)){
                spinnerPage.getEditor().setText(Integer.toString(pagination.getCurrentPageIndex()+1));
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

        sourcesTree.rootProperty().set(Item.createSourcesPageRoot());

        expandToggle.selectedProperty().addListener((observable, oldValue, newValue) ->
                Utility.setExpandRecursively(sourcesTree.getRoot(),expandToggle.isSelected()));

        sourcesTree.setSortPolicy(param -> setSortFromColumns());//disable java sorting
        sourcesTree.rootProperty().get().valueProperty().bind(new ObjectBinding<Object>() {
            {
                bind(pagination.currentPageIndexProperty());
                bind(spinnerPerPage.valueProperty());
                bind(queryList);
                bind(sortingList);
                bind(sourcesTree.getSortOrder());
            }

            @SuppressWarnings("unchecked")
            @Override
            protected Object computeValue() {
                if (mainController != null) {
                    if (queryList.size() > 0) {
                        ArrayList<Bson> aggregation = new ArrayList<>();
                        aggregation.addAll(queryList);
                        aggregation.addAll(sortingList);
                        aggregation.add(Aggregates.skip(spinnerPerPage.getValue() * pagination.getCurrentPageIndex()));
                        aggregation.add(Aggregates.limit(spinnerPerPage.getValue()));
                        MongoCollection<Item> collection = mainController.model.logic.getItemsCollection();
                        List<TreeItem<Object>> list = new ArrayList<>();
                        for (Item item : collection.aggregate(aggregation).allowDiskUse(true).collation(mainController.model.logic.getCollation())) {
                            list.add(item.getTreeItemSources());
                        }
                        sourcesTree.rootProperty().get().getChildren().setAll(list);
                        Utility.setExpandRecursively(sourcesTree.getRoot(), expandToggle.isSelected());
                    }else{
                        sourcesTree.rootProperty().get().getChildren().clear();
                    }
                }
                return Item.createSourcesPageRoot().getValue();
            }
        });
        sourcesTree.setShowRoot(false);
        sourcesTree.setRowFactory(param -> {
            TreeTableRow<Object> row=new TreeTableRow<>();
            row.treeItemProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue!=null && newValue.getValue() instanceof Source) {
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
                if (event.getClickCount() == 2 && event.getButton()== MouseButton.PRIMARY && !row.isEmpty()) {
                    Object rowObj = row.getTreeItem().getValue();
                    if(rowObj instanceof Item){
                        Utility.Window<ItemEditorController> window=Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"),"Item Editor: "+((Item) rowObj).getId().toHexString());
                        window.controller.setMainController(mainController);
                        window.controller.setItem((Item)rowObj);
                        window.stage.show();
                        event.consume();
                    }else if(rowObj instanceof Source){
                        Utility.Window<SourceEditorController> window=Utility.loadFXML(SourceEditorController.class.getResource("SourceEditor.fxml"),"Source Editor");
                        window.controller.setMainController(mainController);
                        window.controller.setItemSource((Item) row.getTreeItem().getParent().getValue(),(Source) rowObj,false);
                        window.stage.show();
                        event.consume();
                    }
                }
            });
            return row;
        });

        sourcesNameColumn.setOnEditCommit(event -> {
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
        sourcesNameColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        sourcesNameColumn.setCellValueFactory(param-> param.getValue().getValue().nameProperty());

        sourcesCountColumn.setOnEditCommit(event -> {
            Object obj=event.getRowValue().getValue();
            Item item;
            if(obj instanceof Item){
                item=(Item) obj;
                if(event.getNewValue()==0D){
                    ((TreeItem<?>)item.getTreeItemPlacements()).getChildren().forEach(o -> {
                        if(o.getValue() instanceof Placement){
                            ((Placement) o.getValue()).setCount(0D);
                        }
                    });
                }else return;
            }else {
                item=(Item)event.getRowValue().getParent().getValue();
                ((Placement) obj).setCount(event.getNewValue());
            }
            sourcesTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        sourcesCountColumn.setCellValueFactory(param-> new ObjectBinding<Double>(){
            {
                Object o=param.getValue().getValue();
                if(o instanceof IStockState){
                    bind(((IStockState) o).countProperty());
                    bind(((IStockState) o).stockOptimalProperty());
                }
            }

            @Override
            protected Double computeValue() {
                Object o=param.getValue().getValue();
                if(o instanceof IStockState){
                    return ((IStockState) o).countProperty().get();
                }
                return null;
            }
        });
        sourcesCountColumn.setCellFactory(MAKE_COUNT_FORMATTED_TREE);

        sourcesMinColumn.setOnEditCommit(event -> {
            Object obj=event.getRowValue().getValue();
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
            sourcesTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        sourcesMinColumn.setCellValueFactory(param-> param.getValue().getValue() instanceof IStockState ? ((IStockState)param.getValue().getValue()).minCountProperty().asObject() : null);
        sourcesMinColumn.setCellFactory(MAKE_COUNT_TREE);

        sourcesOrderedColumn.setOnEditCommit(event -> {
            Object obj=event.getRowValue().getValue();
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
            sourcesTree.refresh();
            mainController.model.logic.getItemsCollection()
                    .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
        });
        sourcesOrderedColumn.setCellValueFactory(param -> param.getValue().getValue() instanceof IStockState ? ((IStockState)param.getValue().getValue()).orderedProperty().asObject() : null);
        sourcesOrderedColumn.setCellFactory(MAKE_COUNT_TREE);

        sourcesLinkColumn.setOnEditCommit(event -> {
            Object obj=event.getRowValue().getValue();
            if(!(obj instanceof Item)){
                Item item=(Item)event.getRowValue().getParent().getValue();
                ((Source) obj).setUrl(event.getNewValue());
                mainController.model.logic.getItemsCollection()
                        .replaceOne(new BsonDocument().append("_id", new BsonObjectId(item.getId())), item);
            }
            sourcesTree.refresh();
        });
        sourcesLinkColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof Source){
                return ((Source) param.getValue().getValue()).urlProperty().nameProperty();
            }
            return null;
        });
        sourcesLinkColumn.setCellFactory(param -> {
            TextFieldTreeTableCell<Object,String> cell=new TextFieldTreeTableCell<>();
            cell.setOnMouseClicked(event -> {
                if(cell.getTreeTableRow().getItem() instanceof ILinked){
                    ILinked link= (ILinked)cell.getTreeTableRow().getItem();
                    if(link.urlProperty().linkProperty().getValueSafe().length()>0){
                        mainController.hostServices.showDocument(link.urlProperty().getLink());
                    }
                }
            });
            cell.setStyle("-fx-text-fill:-fx-text-blue;");
            return cell;
        });
        sourcesSerialColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof ISerialState){
                return ((ISerialState) param.getValue().getValue()).serialProperty();
            }
            return null;
        });

        sourcesDetailsColumn.setOnEditCommit(event -> {
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
        sourcesDetailsColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        sourcesDetailsColumn.setCellValueFactory(param-> param.getValue().getValue().detailsProperty());

        sourcesTagsColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof ITagged){
                return ((ITagged) param.getValue().getValue()).tagsStringProperty();
            }
            return null;
        });
        sourcesDesignationColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof Item){
                return ((Item) param.getValue().getValue()).designationsStringProperty();
            }
            return null;
        });
        sourcesSupplierColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof Source){
                return ((Source) param.getValue().getValue()).supplierProperty().get().nameProperty();
            }else if(param.getValue().getValue() instanceof Item){
                return ((Item) param.getValue().getValue()).suppliersStringProperty();
            }
            return null;
        });
        sourcesManufacturerSupplierColumn.setCellValueFactory(param -> {
            if(param.getValue().getValue() instanceof Item){
                return ((Item) param.getValue().getValue()).manufacturersProperty().toStringProperty();
            }else if(param.getValue().getValue() instanceof Source){
                return ((Source) param.getValue().getValue()).supplierNameProperty();
            }
            return null;
        });
        containsTagQueryInput.setRegexPredicate();
        containsTagQueryInput.setNullString("Deselect Tag");
        containsTagQueryInput.setBackingList(Tag.COLLECTION.readableAndSortableList);
        containsTagQueryInput.nullableValueProperty().addListener(new ChangeListener<Tag>() {
            private TreeTableColumn column;

            @Override
            public void changed(ObservableValue<? extends Tag> observable, Tag oldValue, Tag newValue) {
                if (newValue==null) {
                    if(column!=null) {
                        column.setVisible(false);
                        column=null;
                    }
                } else {
                    for (TreeTableColumn c :sourcesTagsParentColumn.getColumns()) {
                        if(c.getUserData()==null) continue;
                        if(newValue==((Supplier<Tag>)c.getUserData()).get()){
                            column=c;
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
        containsSourceQueryInput.setRegexPredicate();
        containsSourceQueryInput.setNullString("Deselect Contact");
        containsSourceQueryInput.setBackingList(Contact.COLLECTION.readableAndSortableList);

        newBasedOnButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(sourcesTree.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected boolean computeValue() {
                TreeItem treeItem=sourcesTree.getSelectionModel().getSelectedItem();
                return treeItem==null || !(treeItem.getValue() instanceof Item);
            }
        });

        removeButton.disableProperty().bind(new BooleanBinding() {
            {
                bind(sourcesTree.getSelectionModel().selectedItemProperty());
            }
            @Override
            protected boolean computeValue() {
                TreeItem treeItem = sourcesTree.getSelectionModel().getSelectedItem();
                return treeItem == null || treeItem.getValue()==null;
            }
        });

        sourcesNameColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(sourcesNameColumn, "name")));
        sourcesCountParentColumn.setSortable(false);
        sourcesCountColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumCount", new Document("$sum", "$placements.count"))));
            sort.add(Utility.sort(sourcesCountColumn, "sumCount"));
            sort.add(project(exclude("sumCount")));
        });
        sourcesMinColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumMinCount", new Document("$sum", "$placements.minCount"))));
            sort.add(Utility.sort(sourcesMinColumn, "sumMinCount"));
            sort.add(project(exclude("sumMinCount")));
        });
        sourcesOrderedColumn.setUserData((IAggregateModifier) sort -> {
            sort.add(addFields(new Field<>("sumOrdered", new Document("$sum", "$placements.ordered"))));
            sort.add(Utility.sort(sourcesOrderedColumn, "sumOrdered"));
            sort.add(project(exclude("sumOrdered")));
        });
        sourcesSerialColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(sourcesSerialColumn, "placements.serial")));
        sourcesDetailsColumn.setUserData((IAggregateModifier) sort ->
                sort.add(Utility.sort(sourcesDetailsColumn, "details")));
        sourcesTagsParentColumn.setSortable(false);
        sourcesTagsColumn.setSortable(false);
        sourcesDesignationColumn.setUserData((IAggregateModifier) sort -> {
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
            sort.add(Utility.sort(sourcesDesignationColumn, "designations.name"));
            sort.add(project(exclude("designations")));
        });
        sourcesManufacturerSupplierColumn.setUserData((IAggregateModifier) sort -> {
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
            sort.add(Utility.sort(sourcesManufacturerSupplierColumn, "manufacturers.name"));
            sort.add(project(exclude("manufacturers")));
        });
        sourcesLinkColumn.setSortable(false);
    }

    public boolean setSortFromColumns(){
        List<Bson> sort=new ArrayList<>();
        for (TreeTableColumn col:sourcesTree.getSortOrder()) {
            if(col.getUserData() instanceof IAggregateModifier) {
                ((IAggregateModifier) col.getUserData()).modifyAggregate(sort);
            }
        }
        this.sortingList.setAll(sort);
        return true;
    }

    public void reloadAll() {
        queryList.setAll(Aggregates.match(Utility.queryForClass(Item.class)));
    }

    public void runSimpleQuery(){
        QueryBuilder queryBuilder=QueryBuilder.start();
        if(genericQueryInput.getText()!=null && genericQueryInput.getText().length()>0){
            QueryBuilder orQuery=QueryBuilder.start();
            Pattern pattern;
            if(genericRegExp.isSelected()){
                try{
                    pattern=Utility.getPattern(genericQueryInput.getText());
                }catch (PatternSyntaxException e){
                    pattern=Pattern.compile("(?i)"+ Pattern.quote(genericQueryInput.getText()));
                    genericRegExp.setSelected(false);
                }
            }else{
                pattern=Pattern.compile("(?i)"+ Pattern.quote(genericQueryInput.getText()));
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
                try{
                    queryBuilder.and(QueryBuilder.start().put("placements.serial").regex(Utility.getPattern(serialQueryInput.getText())).get());
                }catch (PatternSyntaxException e){
                    queryBuilder.and(QueryBuilder.start().put("placements.serial").regex(Pattern.compile("(?i)"+ Pattern.quote(serialQueryInput.getText()))).get());
                    serialRegExp.setSelected(false);
                }
                queryBuilder.and(QueryBuilder.start().put("placements.serial").regex(Utility.getPattern(serialQueryInput.getText())).get());
            }else{
                queryBuilder.and(QueryBuilder.start().put("placements.serial").regex(Pattern.compile("(?i)"+ Pattern.quote(serialQueryInput.getText()))).get());
            }
        }
        if(!containsTagQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("tags.tag").is(containsTagQueryInput.getNullableValue().getId()).get());
        }
        if(!containedInLocationQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("placements.locationId").in(containedInLocationQueryInput.getNullableValue().withAllChildren().stream().map(Location::getId).collect(Collectors.toList())).get());
        }
        if(!containsDesignationQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("placements.designationsId").is(containsDesignationQueryInput.getNullableValue().getId()).get());
        }
        if(!containsSourceQueryInput.isNullSelected()){
            queryBuilder.and(QueryBuilder.start().put("placements.sources.supplierId").is(containsSourceQueryInput.getNullableValue().getId()).get());
        }
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

    public void clearRecords(){
        queryList.clear();
    }

    public void reloadRecords(){
        queryList.setAll(queryList.toArray(new Bson[0]));
    }

    public void create(ActionEvent actionEvent) {
        Item item=new Item();
        Utility.Window<ItemEditorController> window=Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"),"Item Editor: "+item.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setItem(item);
        window.stage.show();
    }

    public void basedOn(ActionEvent actionEvent) {
        Item item=((Item)sourcesTree.getSelectionModel().getSelectedItem().getValue()).cloneObjectData();
        Utility.Window<ItemEditorController> window=Utility.loadFXML(ItemEditorController.class.getResource("ItemEditor.fxml"),"Item Editor: "+item.getId().toHexString());
        window.controller.setMainController(mainController);
        window.controller.setItem(item);
        window.stage.show();
    }

    public void removeSelected(ActionEvent actionEvent) {
        TreeItem o=sourcesTree.getSelectionModel().getSelectedItem();
        if(o.getValue() instanceof Source){
            if(o.getParent().getValue() instanceof Item) {
                Item item=((Item) o.getParent().getValue());
                if(ButtonType.OK==new Alert(Alert.AlertType.CONFIRMATION,"Remove "+o.getValue()+" ?").showAndWait().orElse(ButtonType.CANCEL)) {
                    item.sourcesProperty().remove(o);
                    mainController.model.logic.getItemsCollection().replaceOne(
                            new Document("_id", item.getId()), item);
                }
            }
        }else if(o.getValue() instanceof Item){
            if(ButtonType.OK==new Alert(Alert.AlertType.CONFIRMATION,"Remove "+o.getValue()+" ?").showAndWait().orElse(ButtonType.CANCEL)) {
                mainController.model.logic.getItemsCollection().deleteOne(new Document("_id", ((Item) o.getValue()).getId()));
                sourcesTree.rootProperty().get().getChildren().remove(o);
            }
        }
    }

    public void clearQuery(ActionEvent actionEvent) {
        containedInLocationQueryInput.setNullableValue(null);
        containsTagQueryInput.setNullableValue(null);
        containsDesignationQueryInput.setNullableValue(null);
        containsSourceQueryInput.setNullableValue(null);
        serialQueryInput.setText(null);
        nameQueryInput.setText(null);
        genericQueryInput.setText(null);
    }
}
