package com.dgs.dapc.itemDB.javafx.main;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.IAggregateModifier;
import com.dgs.dapc.itemDB.headless.db.IStockState;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Item;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.dgs.dapc.itemDB.javafx.main.editor.itemEditor.tagValueEditor.TagValueEditorController;
import com.dgs.dapc.itemDB.javafx.main.tabs.contacts.ContactsTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.designations.DesignationsTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.items.ItemsTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.locations.LocationsTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.sources.SourcesTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.tags.TagsTabController;
import com.dgs.dapc.itemDB.javafx.main.tabs.util.UtilTabController;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamListener;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.UnwindOptions;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.stage.Window;
import javafx.util.Callback;
import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Sorts.descending;

public class MainController implements Initializable,AutoCloseable {
    public HostServices hostServices;
    public MainModel model;
    public TabPane tabs;
    public Tab contactsTab;
    public ContactsTabController contactsController;
    public Tab designationsTab;
    public DesignationsTabController designationsController;
    public Tab itemsTab;
    public ItemsTabController itemsController;
    public Tab locationsTab;
    public LocationsTabController locationsController;
    public Tab sourcesTab;
    public SourcesTabController sourcesController;
    public Tab tagsTab;
    public TagsTabController tagsController;
    public Tab utilTab;
    public UtilTabController utilController;

    public final ObservableList<Object> editors= FXCollections.observableArrayList();

    public static final Callback MAKE_COUNT_TREE = param -> new TextFieldTreeTableCell<Object, Double>(){
        {
            setAlignment(Pos.CENTER_RIGHT);
            setConverter(Utility.THE_DOUBLE_CONVERTER);
        }
        @Override
        public void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else {
                setText(Utility.THE_DOUBLE_CONVERTER.toString(item));
            }
        }
    };
    public static final Callback MAKE_COUNT_FORMATTED_TREE = param -> new TextFieldTreeTableCell<Object, Double>(){
        {
            setAlignment(Pos.CENTER_RIGHT);
            setConverter(Utility.THE_DOUBLE_CONVERTER);
        }
        @Override
        public void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else {
                setText(Utility.THE_DOUBLE_CONVERTER.toString(item));
            }
            TreeItem o=getTreeTableRow().treeItemProperty().get();
            if(o==null) {
                return;
            }
            Object value=o.getValue();
            if(value instanceof IStockState && !((IStockState) value).isStockOptimal()){
                setStyle("-fx-light-text-color: derive(-fx-fg-deepRed,+25%);\n" +
                        "-fx-mid-text-color:   -fx-fg-deepRed;\n" +
                        "-fx-dark-text-color:  derive(-fx-fg-deepRed,-25%);");
            }else{
                setStyle("");
            }
        }
    };

    private static class TagColumnUserData implements IAggregateModifier, Supplier<Tag>{
        private final TreeTableColumn column;
        private final Tag obj;

        public TagColumnUserData(TreeTableColumn column, Tag obj){
            this.column=column;
            this.obj=obj;
        }

        @Override
        public void modifyAggregate(List<Bson> sort) {
            sort.add(unwind("$tags", new UnwindOptions().preserveNullAndEmptyArrays(true)));
            sort.add(addFields(new Field<>("hasTag",
                            new Document("$cond",
                                    new Document("if",
                                            new Document("$eq", Arrays.asList("$tags.tag",obj.getId())))
                                            .append("then", true)
                                            .append("else", false))),
                    new Field<>("tagValue",
                            new Document("$cond",
                                    new Document("if",
                                            new Document("$eq", Arrays.asList("$tags.tag",obj.getId())))
                                            .append("then", "$tags.value")
                                            .append("else",
                                                    new BsonNull())))));
            sort.add(sort(descending("hasTag")));
            sort.add(group("$_id",
                    first("name", "$name"),
                    first("picture", "$picture"),
                    first("details", "$details"),
                    first("manufacturersId", "$manufacturersId"),
                    first("sources", "$sources"),
                    first("placements", "$placements"),
                    push("tags", "$tags"),
                    first("tagValue", "$tagValue")));
            sort.add(Utility.sort(column, "tagValue"));
            sort.add(project(exclude("tagValue")));
        }

        @Override
        public Tag get() {
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    private TreeTableColumn<Object,String> getTagColumn(Tag me) {
        TreeTableColumn<Object, String> column = new TreeTableColumn<>();
        column.setEditable(false);
        column.setPrefWidth(150);
        column.textProperty().bind(me.nameProperty());
        column.setUserData(new TagColumnUserData(column,me));
        column.setCellValueFactory(param -> {
            if (param.getValue().getValue() instanceof Item) {
                TagValue tagValue = ((Item) param.getValue().getValue()).tagsProperty().map.get(me.getId());
                if (tagValue != null) {
                    return tagValue.valueStringProperty();
                }
            }
            return null;
        });
        column.setCellFactory(param -> {
            TextFieldTreeTableCell<Object,String> cell=new TextFieldTreeTableCell<>();
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    Object rowData = cell.getTreeTableRow().getTreeItem().getValue();
                    if (rowData instanceof Item) {
                        TagValue tagValue = ((Item) rowData).tagsProperty().map.get(me.getId());
                        if (tagValue!=null){
                            Utility.Window<TagValueEditorController> window = Utility.loadFXML(TagValueEditorController.class.getResource("TagValueEditor.fxml"), "TagValue Editor",getStage());
                            window.controller.setMainController(this);
                            window.controller.setItemTagValue((Item)rowData,tagValue,false);
                            event.consume();
                            window.stage.show();
                        }
                    }
                }
            });
            return cell;
        });
        return column;
    }

    private final Map<Tag, TreeTableColumn<Object,String>[]> columns=new HashMap<>();
    private final Consumer<Tag> tagColumnAdder = tag -> {
        @SuppressWarnings("unchecked")
        TreeTableColumn<Object,String>[] cols=new TreeTableColumn[2];
        cols[0]=getTagColumn(tag);
        cols[1]=getTagColumn(tag);
        for (TreeTableColumn column : cols){
            column.setVisible(false);
        }
        columns.put(tag,cols);
        Platform.runLater(()->{
            itemsController.itemsTagsParentColumn.getColumns().add(cols[0]);
            sourcesController.sourcesTagsParentColumn.getColumns().add(cols[1]);
        });
    };
    private final ListChangeListener<Tag> columnsChangeListener = c -> {
        ArrayList<Tag> additions=new ArrayList<>(),deletions=new ArrayList<>();
        while (c.next()) {
            if (c.wasRemoved()) {
                deletions.addAll(c.getRemoved());
            }
            if (c.wasAdded()) {
                additions.addAll(c.getAddedSubList());
            }
        }
        deletions.forEach(tag->{
            TreeTableColumn<Object,String>[] cols=columns.get(tag);
            if(cols!=null){
                Platform.runLater(()->{
                    itemsController.itemsTagsParentColumn.getColumns().remove(cols[0]);
                    sourcesController.sourcesTagsParentColumn.getColumns().remove(cols[1]);
                });
                columns.remove(tag);
            }
        });
        additions.forEach(tagColumnAdder);
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Tag.COLLECTION.readableAndSortableList.forEach(tagColumnAdder);
        Tag.COLLECTION.readableAndSortableList.addListener(columnsChangeListener);

        contactsController.mainController =
                designationsController.mainController =
                                locationsController.mainController =
                                                tagsController.mainController =
                                                        utilController.mainController = this;
        itemsController.setMainController(this);
        sourcesController.setMainController(this);

        itemsTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                Platform.runLater(()->{
                    itemsController.itemsTree.resizeColumn(itemsController.itemsTagsParentColumn,1);
                    itemsController.itemsTree.resizeColumn(itemsController.itemsTagsParentColumn,-1);
                    itemsController.itemsTree.refresh();
                });
            }
        });
        itemsTab.setUserData((Runnable) () -> {
            if (!editors.isEmpty()) {
                sourcesController.clearRecords();
                itemsController.reloadRecords();
            }
        });
        sourcesTab.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                Platform.runLater(()->{
                    sourcesController.sourcesTree.resizeColumn(sourcesController.sourcesTagsParentColumn,1);
                    sourcesController.sourcesTree.resizeColumn(sourcesController.sourcesTagsParentColumn,-1);
                    sourcesController.sourcesTree.refresh();
                });
            }
        });
        sourcesTab.setUserData((Runnable) () -> {
            if (!editors.isEmpty()) {
                itemsController.clearRecords();
                sourcesController.reloadRecords();
            }
        });

        tabs.selectionModelProperty().get().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.getUserData() instanceof Runnable) {
                ((Runnable) newValue.getUserData()).run();
            } else if (!editors.isEmpty()) {
                unloadRecords();
            }
        });
    }

    public void unloadRecords(){
        itemsController.clearRecords();
        sourcesController.clearRecords();
    }

    @Override
    public void close() {
        Tag.COLLECTION.readableAndSortableList.removeListener(columnsChangeListener);
    }

    /*
    public void findAndSwitch(String qrTag, Object source){
        if(qrTag!=null && qrTag.length()>2) {
            switch (qrTag.charAt(0)) {
                case Contact.PREFIX:
                    tabs.getSelectionModel().select(contactsTab);
                    break;
                case Designation.PREFIX:
                    tabs.getSelectionModel().select(designationsTab);
                    break;
                case Placement.PREFIX:
                case Item.PREFIX:
                    tabs.getSelectionModel().select(itemsTab);
                    itemsController.qrLinkInput.setText(qrTag);
                    itemsController.analyzeQR(new ActionEvent(source,itemsController.qrLinkInput));
                    break;
                case Location.PREFIX:
                    tabs.getSelectionModel().select(locationsTab);
                    break;
                case Tag.PREFIX:
                    tabs.getSelectionModel().select(tagsTab);
                    break;
            }
        }
    }
    */

    public Window getStage(){
        return tabs.getScene().getWindow();
    }

    private final SimpleBooleanProperty scanEnable=new SimpleBooleanProperty();

    public boolean isScanEnable() {
        return scanEnable.get();
    }

    public SimpleBooleanProperty scanEnableProperty() {
        return scanEnable;
    }

    public void setScanEnable(boolean scanEnable) {
        this.scanEnable.set(scanEnable);
    }

    private final WebcamListener webcamListener=new WebcamListener() {
        private volatile String lastCode=null;

        @Override
        public void webcamOpen(WebcamEvent we) {
            lastCode=null;
        }

        @Override
        public void webcamClosed(WebcamEvent we) {

        }

        @Override
        public void webcamDisposed(WebcamEvent we) {

        }

        @Override
        public void webcamImageObtained(WebcamEvent we) {
            if(lastCode!=null){
                return;
            }
            BufferedImage image=we.getImage();
            if(scanEnable.get()) {
                Utility.ScanResult codeResult = Utility.readQRCode(image);
                if (itemsTab.isSelected()) {
                    itemsController.setImage(image);
                } else if (sourcesTab.isSelected()) {
                    sourcesController.setImage(image);
                }
                if(codeResult.code.size()>0){
                    String code=codeResult.code.get(0);
                    if (lastCode == null && code != null) {
                        lastCode = code;
                        Toolkit.getDefaultToolkit().beep();
                        if (itemsTab.isSelected()) {
                            itemsController.setCode(code);
                        } else if (sourcesTab.isSelected()) {
                            sourcesController.setCode(code);
                        }
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                lastCode = null;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            }else{
                if (itemsTab.isSelected()) {
                    itemsController.setImage(image);
                } else if (sourcesTab.isSelected()) {
                    sourcesController.setImage(image);
                }
            }
        }
    };

    private Webcam webcam;

    {
        setDefaultWebCamAsync();
    }

    private void setDefaultWebCamAsync(){
        try {
            webcam=Webcam.getDefault(5000);
            webcam.addWebcamListener(webcamListener);
            if(!webcam.open(true)){
                webcam.removeWebcamListener(webcamListener);
                webcam=null;
            }
        } catch (TimeoutException| WebcamException e) {
            webcam=null;
        }
    }

    private void setWebCamAsync(Webcam webCam){
        try{
            webcam=webCam;
            webcam.addWebcamListener(webcamListener);
            if(!webcam.open(true)){
                webcam.removeWebcamListener(webcamListener);
                webcam=null;
            }
        }catch (WebcamException e){
            webcam=null;
        }
    }

    public void configureCameraAsync(ActionEvent actionEvent) {
        if(webcam==null){
            setDefaultWebCamAsync();
        }else{
            String name=webcam.getName();
            webcam.removeWebcamListener(webcamListener);
            webcam.close();
            try {
                List<Webcam> cams= Webcam.getWebcams(5000).stream()
                        .sorted(Comparator.comparing(Webcam::getName)).collect(Collectors.toList());
                boolean foundMe=false;
                for (Webcam cam:cams) {
                    if(foundMe){
                        setWebCamAsync(cam);
                        break;
                    }
                    if(name.equals(cam.getName())){
                        foundMe=true;
                    }
                }
                if(!foundMe || !webcam.isOpen()){
                    setWebCamAsync(cams.get(0));
                }
            } catch (TimeoutException|WebcamException e) {
                webcam=null;
                e.printStackTrace();
            }
        }
    }
}
