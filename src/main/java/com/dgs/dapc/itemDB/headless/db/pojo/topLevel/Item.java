package com.dgs.dapc.itemDB.headless.db.pojo.topLevel;

import com.dgs.dapc.itemDB.headless.MainLogic;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.properties.ObservableBoundMapList;
import com.dgs.dapc.itemDB.headless.properties.ObservableNamedObjectsList;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Item definition
 */
@BsonDiscriminator("Item")
public class Item implements INamed, IDetailed, IStockState, IIdentifiable, IPictured,ITagged, ISerialState,ICloneable<Item>,ISettable<Item>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='I';
    @SuppressWarnings("unchecked")
    public static TreeItem<Object> createPlacementsPageRoot(){
        return new TreeItem<>(new Item("PlacementsPageRoot",null,"ROOT", Collections.EMPTY_LIST,Collections.EMPTY_LIST,Collections.EMPTY_LIST,Collections.EMPTY_LIST));
    }

    @SuppressWarnings("unchecked")
    public static TreeItem<Object> createSourcesPageRoot(){
        return new TreeItem<>(new Item("SourcesPageRoot",null,"ROOT", Collections.EMPTY_LIST,Collections.EMPTY_LIST,Collections.EMPTY_LIST,Collections.EMPTY_LIST));
    }

    @SuppressWarnings("unchecked")
    private final TreeItem treeItemPlacements =new TreeItem(this);
    private final ReadOnlyStringWrapper designationsString =new ReadOnlyStringWrapper();
    @BsonRemove
    public ReadOnlyStringProperty designationsStringProperty() {
        return designationsString.getReadOnlyProperty();
    }
    private final ReadOnlyStringWrapper placementsString =new ReadOnlyStringWrapper();
    @BsonRemove
    public ReadOnlyStringProperty placementsStringProperty() {
        return placementsString.getReadOnlyProperty();
    }
    private final ReadOnlyStringWrapper coordinatesString =new ReadOnlyStringWrapper();
    @BsonRemove
    public ReadOnlyStringProperty coordinatesStringProperty() {
        return coordinatesString.getReadOnlyProperty();
    }
    @SuppressWarnings("unchecked")
    private final ObservableList<TreeItem<Placement>> placements= treeItemPlacements.getChildren();
    private final ReadOnlyStringWrapper serials=new ReadOnlyStringWrapper();
    {
        designationsString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) c -> {
                    while(c.next()){
                        if(c.wasRemoved()){
                            c.getRemoved().forEach(treeItem->unbind(treeItem.getValue().designationsProperty().toStringProperty()));
                            invalidate();
                        }
                        if(c.wasAdded()){
                            c.getAddedSubList().forEach(treeItem->bind(treeItem.getValue().designationsProperty().toStringProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected String computeValue() {
                //todo remove duplicates?
                stringBuilder.setLength(0);
                placements.forEach(v -> {
                    String s=v.getValue().designationsProperty().toStringProperty().get();
                    if(s!=null && s.length()>0) {
                        stringBuilder.append(s).append(", ");
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder.toString();
                }
                return "";
            }
        });
        placementsString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) c -> {
                    while(c.next()){
                        if(c.wasRemoved()){
                            c.getRemoved().forEach(treeItem->unbind(treeItem.getValue().locationNameProperty()));
                            invalidate();
                        }
                        if(c.wasAdded()){
                            c.getAddedSubList().forEach(treeItem -> bind(treeItem.getValue().locationNameProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected String computeValue() {
                ArrayList<Location> locations=new ArrayList<>();
                stringBuilder.setLength(0);
                placements.forEach(v -> {
                    if(v.getValue().getLocation()!=null) {
                        Location location=v.getValue().getLocation();
                        if(!locations.contains(location)) {
                            locations.add(location);
                            String s = location.toString();
                            if (s != null && s.length() > 0) {
                                stringBuilder.append(s).append(", ");
                            }
                        }
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder.toString();
                }
                return "";
            }
        });
        coordinatesString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();

            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) c -> {
                    while(c.next()){
                        if(c.wasRemoved()){
                            c.getRemoved().forEach(treeItem->unbind(treeItem.getValue().coordinatesProperty().toStringProperty()));
                            invalidate();
                        }
                        if(c.wasAdded()){
                            c.getAddedSubList().forEach(treeItem->bind(treeItem.getValue().coordinatesProperty().toStringProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                placements.forEach(v -> {
                    String s=v.getValue().coordinatesProperty().toStringProperty().get();
                    if(s!=null && s.length()>0) {
                        stringBuilder.append(s).append("; ");
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder.toString();
                }
                return "";
            }
        });
        serials.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) c -> {
                    while(c.next()){
                        if(c.wasRemoved()){
                            c.getRemoved().forEach(treeItem->unbind(treeItem.getValue().serialProperty()));
                            invalidate();
                        }
                        if(c.wasAdded()){
                            c.getAddedSubList().forEach(treeItem->bind(treeItem.getValue().serialProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                placements.forEach(v -> {
                    String s=v.getValue().serialProperty().get();
                    if(s!=null && s.length()>0) {
                        stringBuilder.append(s).append("; ");
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder.toString();
                }
                return "";
            }
        });
    }

    @SuppressWarnings("unchecked")
    private final TreeItem treeItemSources =new TreeItem(this);
    @SuppressWarnings("unchecked")
    private final ObservableList<TreeItem<Source>> sources= treeItemSources.getChildren();
    private final ReadOnlyStringWrapper suppliersString =new ReadOnlyStringWrapper();
    @BsonRemove
    public ReadOnlyStringProperty suppliersStringProperty() {
        return suppliersString.getReadOnlyProperty();
    }
    {
        suppliersString.bind(new StringBinding() {
            final StringBuilder stringBuilder=new StringBuilder();
            {
                sources.addListener((ListChangeListener<TreeItem<Source>>) c -> {
                    while(c.next()){
                        if(c.wasRemoved()){
                            c.getRemoved().forEach(treeItem->unbind(treeItem.getValue().supplierNameProperty()));
                            invalidate();
                        }
                        if(c.wasAdded()){
                            c.getAddedSubList().forEach(treeItem-> bind(treeItem.getValue().supplierNameProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                sources.forEach(v -> {
                    String s=v.getValue().supplierNameProperty().get();
                    if(s!=null && s.length()>0){
                        stringBuilder.append(s).append(", ");
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder.toString();
                }
                return "";
            }
        });
    }

    private final SimpleObjectProperty<ObjectId> id=new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<DiscriminatedObjectId> did=new ReadOnlyObjectWrapper<>();
    {
        did.bind(new ObjectBinding<DiscriminatedObjectId>() {
            {
                bind(id);
            }
            @Override
            protected DiscriminatedObjectId computeValue() {
                return new DiscriminatedObjectId(id.get(),PREFIX);
            }
        });
    }
    private final SimpleStringProperty name=new SimpleStringProperty();
    private final SimpleStringProperty picture=new SimpleStringProperty();
    private final ReadOnlyObjectWrapper<Image> image=new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper containsImage=new ReadOnlyBooleanWrapper();
    {
        image.bind(new ObjectBinding<Image>() {
            {
                bind(picture);
            }
            @Override
            protected Image computeValue() {
                if(picture.getValueSafe().length()>0) {
                    try {
                        File file=new File(MainLogic.getLocalFilesPath() + File.separator + picture.get());
                        if (Files.exists(file.toPath())) {
                            return new Image(file.toURI().toString());
                        } else {
                            file=new File(picture.get());
                            if(Files.exists(file.toPath())){
                                return new Image(file.toURI().toString());
                            }
                        }
                        return new Image(picture.get());
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            }
        });
        containsImage.bind(new BooleanBinding() {
            {
                bind(image);
            }
            @Override
            protected boolean computeValue() {
                return image.get()!=null;
            }
        });
    }
    private final SimpleStringProperty details=new SimpleStringProperty();
    private final ObservableNamedObjectsList<Contact> manufacturers = new ObservableNamedObjectsList<>();

    private final ReadOnlyDoubleWrapper count=new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper minCount=new ReadOnlyDoubleWrapper();
    private final ReadOnlyDoubleWrapper ordered=new ReadOnlyDoubleWrapper();
    {
        count.bind(new DoubleBinding() {
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) observable -> {
                    while (observable.next()) {
                        if (observable.wasRemoved()) {
                            observable.getRemoved().forEach(item -> unbind(item.getValue().countProperty()));
                            invalidate();
                        }
                        if (observable.wasAdded()) {
                            observable.getAddedSubList().forEach(item -> bind(item.getValue().countProperty()));
                            invalidate();
                        }
                    }
                });
            }

            @Override
            protected double computeValue() {
                return placements.stream().mapToDouble(item->item.getValue().getCount()).sum();
            }
        });
        minCount.bind(new DoubleBinding() {
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) observable->{
                    while (observable.next()) {
                        if (observable.wasRemoved()) {
                            observable.getRemoved().forEach(item -> unbind(item.getValue().minCountProperty()));
                            invalidate();
                        }
                        if (observable.wasAdded()) {
                            observable.getAddedSubList().forEach(item -> bind(item.getValue().minCountProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected double computeValue() {
                return placements.stream().mapToDouble(item->item.getValue().getMinCount()).sum();
            }
        });
        ordered.bind(new DoubleBinding() {
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) observable->{
                    while (observable.next()) {
                        if (observable.wasRemoved()) {
                            observable.getRemoved().forEach(item -> unbind(item.getValue().orderedProperty()));
                            invalidate();
                        }
                        if (observable.wasAdded()) {
                            observable.getAddedSubList().forEach(item -> bind(item.getValue().orderedProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected double computeValue() {
                return placements.stream().mapToDouble(item->item.getValue().getOrdered()).sum();
            }
        });
    }

    private final ObservableBoundMapList<TagValue> tags=new ObservableBoundMapList<>(id -> null);
    private final ReadOnlyStringWrapper tagsString =new ReadOnlyStringWrapper();
    {
        tagsString.bind(new StringBinding() {
            final StringBuilder stringBuilder =new StringBuilder();
            {
                tags.map.addListener((MapChangeListener<ObjectId, TagValue>) change -> {
                    if(change.wasRemoved()){
                        unbind(change.getValueRemoved().tagsStringProperty());
                        invalidate();
                    }
                    if(change.wasAdded() && !getDependencies().contains(change.getValueRemoved())){
                        bind(change.getValueAdded().tagsStringProperty());
                        invalidate();
                    }
                });
            }
            @Override
            protected String computeValue() {
                stringBuilder.setLength(0);
                tags.map.forEach((objectId, tagValue) -> {
                    String s=tagValue.tagsStringProperty().get();
                    if(s!=null && s.length()>0){
                        stringBuilder.append(s).append("; ");
                    }
                });
                if(stringBuilder.length()>2){
                    stringBuilder.setLength(stringBuilder.length()-2);
                }
                return stringBuilder.toString();
            }
        });
    }


    private final ReadOnlyBooleanWrapper stockOptimalProperty =new ReadOnlyBooleanWrapper();
    @BsonRemove
    public boolean isStockOptimal(){
        return stockOptimalProperty.get();
    }
    {
        stockOptimalProperty.bind(new BooleanBinding() {
            {
                placements.addListener((ListChangeListener<TreeItem<Placement>>) observable->{
                    while (observable.next()) {
                        if (observable.wasRemoved()) {
                            observable.getRemoved().forEach(item -> unbind(item.getValue().stockOptimalProperty()));
                            invalidate();
                        }
                        if (observable.wasAdded()) {
                            observable.getAddedSubList().forEach(item -> bind(item.getValue().stockOptimalProperty()));
                            invalidate();
                        }
                    }
                });
            }
            @Override
            protected boolean computeValue() {
                return placements.stream().allMatch(placementTreeItem ->
                        placementTreeItem.getValue() == null || placementTreeItem.getValue().isStockOptimal());
            }
        });
    }
    @Override
    public ReadOnlyBooleanProperty stockOptimalProperty() {
        return stockOptimalProperty.getReadOnlyProperty();
    }

    public Item(String name,String picture, String details, List<Contact> manufacturers,
                List<Source> sources, List<Placement> placements, List<TagValue> tags) {
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
        this.manufacturers.addAll(manufacturers);
        sources.forEach(source -> this.sources.add(new TreeItem<>(source)));
        placements.forEach(placement -> this.placements.add(new TreeItem<>(placement)));
        tags.forEach(tagValue -> this.tags.map.put(tagValue.getId(),tagValue));
        this.id.set(new ObjectId());
    }

    private Item(ObjectId id,String picture, String name, String details, List<Contact> manufacturers,
                 List<Source> sources, List<Placement> placements, List<TagValue> tags) {
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
        this.manufacturers.addAll(manufacturers);
        sources.forEach(source -> this.sources.add(new TreeItem<>(source)));
        placements.forEach(placement -> this.placements.add(new TreeItem<>(placement)));
        tags.forEach(tagValue -> this.tags.map.put(tagValue.getId(),tagValue));
        this.id.set(id);
    }

    public Item(){
        this.id.set(new ObjectId());
    }

    @BsonCreator
    public static Item make(@BsonId ObjectId id,
                            @BsonProperty("name") String name,
                            @BsonProperty("picture") String picture,
                            @BsonProperty("details") String details,
                            @BsonProperty("manufacturersId") List<ObjectId> manufacturersId,
                            @BsonProperty("sources") List<Source> sources,
                            @BsonProperty("placements") List<Placement> placements,
                            @BsonProperty("tags") List<TagValue> tagValues) {
        if(manufacturersId==null){
            manufacturersId=Collections.EMPTY_LIST;
        }
        if(sources==null){
            sources=Collections.EMPTY_LIST;
        }
        if(placements==null){
            placements=Collections.EMPTY_LIST;
        }
        if(tagValues==null){
            tagValues=Collections.EMPTY_LIST;
        }
        Item item = new Item(id,picture,name,details,
                manufacturersId.stream().map(Contact.COLLECTION::getAndMakeIfMissing).collect(Collectors.toList()),
                sources,placements,tagValues);
        item.setExists(true);
        return item;
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public void setName(String name) {
        this.name.set(name);
    }

    @Override
    @BsonRemove
    public SimpleStringProperty nameProperty() {
        return name;
    }

    @Override
    public String getPicture() {
        return picture.get();
    }

    @Override
    public void setPicture(String pictureLink) {
        this.picture.set(pictureLink);
    }

    @Override
    @BsonRemove
    public SimpleStringProperty pictureProperty() {
        return picture;
    }

    @Override
    @BsonRemove
    public ReadOnlyObjectProperty<Image> imageProperty() {
        return image.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public ReadOnlyBooleanProperty containingImageProperty() {
        return containsImage.getReadOnlyProperty();
    }

    @Override
    public String getDetails() {
        return details.get();
    }

    @Override
    public void setDetails(String details) {
        this.details.set(details);
    }

    @Override
    @BsonRemove
    public SimpleStringProperty detailsProperty() {
        return details;
    }

    @Override
    @BsonRemove
    public Double getOrdered() {
        return ordered.get();
    }

    @Override
    @BsonRemove
    public Double getCount() {
        return count.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyDoubleProperty countProperty() {
        return count.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public Double getMinCount() {
        return minCount.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyDoubleProperty orderedProperty() {
        return ordered.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public ReadOnlyDoubleProperty minCountProperty() {
        return minCount.getReadOnlyProperty();
    }

    @Override
    @BsonId
    public ObjectId getId() {
        return id.get();
    }

    @Override
    @BsonId
    public void setId(ObjectId id) {
        this.id.set(id);
    }

    @Override
    @BsonRemove
    public DiscriminatedObjectId getDiscriminatedId() {
        return did.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyObjectProperty<DiscriminatedObjectId> discriminatedIdProperty() {
        return did.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public SimpleObjectProperty<ObjectId> idProperty() {
        return id;
    }

    public List<Source> getSources() {
        return sources.stream().map(TreeItem::getValue).collect(Collectors.toList());
    }

    public void setSources(List<Source> sources) {
        this.sources.clear();
        sources.forEach(source -> this.sources.add(new TreeItem<>(source)));
    }

    @BsonRemove
    public ObservableList<TreeItem<Source>> sourcesProperty(){
        return sources;
    }

    public List<Placement> getPlacements() {
        return placements.stream().map(TreeItem::getValue).collect(Collectors.toList());
    }

    public void setPlacements(List<Placement> placements) {
        this.placements.clear();
        placements.forEach(placement -> this.placements.add(new TreeItem<>(placement)));
    }

    @BsonRemove
    public ObservableList<TreeItem<Placement>> placementsProperty(){
        return placements;
    }


    public List<ObjectId> getManufacturersId() {
        return manufacturers.stream().map(Contact::getId).collect(Collectors.toList());
    }

    public void setManufacturersId(List<ObjectId> manufacturersID){
        this.manufacturers.clear();
        manufacturersID.forEach(id->this.manufacturers.add(Contact.COLLECTION.getAndMakeIfMissing(id)));
    }

    @BsonRemove
    public ObservableNamedObjectsList<Contact> manufacturersProperty(){
        return manufacturers;
    }

    @BsonRemove
    public void setManufacturers(List<Contact> manufacturers){
        this.manufacturers.clear();
        this.manufacturers.addAll(manufacturers);
    }

    @BsonRemove
    public List<Contact> getManufacturers(){
        return new ArrayList<>(manufacturers);
    }

    @BsonRemove
    public ObservableBoundMapList<TagValue> tagsProperty(){
        return tags;
    }

    public void setTags(List<TagValue> tags){
        this.tags.map.clear();
        tags.forEach(tagValue -> this.tags.map.put(tagValue.getId(),tagValue));
    }

    public List<TagValue> getTags(){
        return new ArrayList<>(tags.map.values());
    }

    @BsonRemove
    public List<ObjectId> getTagsId(){
        return tags.map.values().stream().map(TagValue::getId).collect(Collectors.toList());
    }

    @BsonRemove
    public TreeItem getTreeItemPlacements(){
        return treeItemPlacements;
    }

    @BsonRemove
    public TreeItem getTreeItemSources(){
        return treeItemSources;
    }

    @Override
    @BsonRemove
    public String toString() {
        return name.get();
    }

    @Override
    @BsonRemove
    public String getTagsString() {
        return tagsString.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyStringProperty tagsStringProperty() {
        return tagsString;
    }

    @Override
    @BsonRemove
    public String getSerial() {
        return serials.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyStringProperty serialProperty() {
        return serials.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public Item cloneObjectData() {
        return new Item(new ObjectId(), getPicture(), getName(), getDetails(), getManufacturers(), cloneSourcesData(), clonePlacementsData(), cloneTagValuesData());
    }

    @Override
    @BsonRemove
    public Item cloneObjectFully() {
        Item clone=new Item(getId(),getPicture(),getName(),getDetails(),getManufacturers(),cloneSourcesFully(),clonePlacementsFully(),cloneTagValuesFully());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Item value) {
        setPicture(value.getPicture());
        setName(value.getName());
        setDetails(value.getDetails());
        setManufacturers(value.getManufacturers());
        setSources(value.cloneSourcesData());//decouple!
        setPlacements(value.clonePlacementsData());//decouple!
        setTags(value.cloneTagValuesData());//decouple!
    }

    @Override
    @BsonRemove
    public void setFully(Item value) {
        setPicture(value.getPicture());
        setName(value.getName());
        setDetails(value.getDetails());
        setManufacturers(value.getManufacturers());
        setSources(value.cloneSourcesFully());//decouple!
        setPlacements(value.clonePlacementsFully());//decouple!
        setTags(value.cloneTagValuesFully());//decouple!
        setId(value.getId());
        setExists(value.getExists());
    }

    private List<Source> cloneSourcesFully(){
        return sources.stream().map(sourceTreeItem -> sourceTreeItem.getValue().cloneObjectFully()).collect(Collectors.toList());
    }

    private List<Placement> clonePlacementsFully(){
        return placements.stream().map(placementTreeItem -> placementTreeItem.getValue().cloneObjectFully()).collect(Collectors.toList());
    }

    private List<TagValue> cloneTagValuesFully(){
        return tags.readableAndSortableList.stream().map((Function<TagValue, TagValue>) TagValue::cloneObjectFully).collect(Collectors.toList());
    }

    private List<Source> cloneSourcesData(){
        return sources.stream().map(sourceTreeItem -> sourceTreeItem.getValue().cloneObjectData()).collect(Collectors.toList());
    }

    private List<Placement> clonePlacementsData(){
        return placements.stream().map(placementTreeItem -> placementTreeItem.getValue().cloneObjectData()).collect(Collectors.toList());
    }

    private List<TagValue> cloneTagValuesData(){
        return tags.readableAndSortableList.stream().map((Function<TagValue, TagValue>) TagValue::cloneObjectData).collect(Collectors.toList());
    }

    @Override
    @BsonRemove
    public SimpleBooleanProperty existsProperty() {
        return exists;
    }

    @Override
    @BsonRemove
    public boolean getExists() {
        return exists.get();
    }

    @Override
    @BsonRemove
    public void setExists(boolean exists) {
        this.exists.set(exists);
    }
}
