package com.dgs.dapc.itemDB.headless.db.pojo.topLevel;

import com.dgs.dapc.itemDB.headless.MainLogic;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.properties.ObservableBoundMapList;
import com.dgs.dapc.itemDB.headless.properties.ObservableToStringList;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
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
import java.util.stream.Collectors;

import static com.dgs.dapc.itemDB.Utility.THE_DOUBLE_CONVERTER;

/**
 * Container definition
 */
@BsonDiscriminator("Location")
public class Location implements INamed, IDetailed, IIdentifiable, IPictured, ICloneable<Location>,ISettable<Location>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='L';

    public static final ObservableBoundMapList<Location> COLLECTION =new ObservableBoundMapList<>(Location::make);
    @SuppressWarnings("unchecked")
    public static final TreeItem<Location> ROOT=new TreeItem<>(new Location("ROOT",null,null,Collections.EMPTY_LIST));
    static {
        COLLECTION.map.addListener((MapChangeListener<ObjectId, Location>) change -> {
            if(change.wasRemoved()){
                change.getValueRemoved().removeFrom(ROOT);
            }
            if(change.wasAdded()){
                change.getValueAdded().addTo(ROOT);
            }
        });
    }

    @BsonRemove
    public boolean removeFrom(TreeItem<Location> root){
        if(getId()==null){
            return recursiveRemoveNullId(root);
        }
        return recursiveRemove(root);
    }

    @BsonRemove
    private boolean recursiveRemoveNullId(TreeItem<Location> branch) {
        for (TreeItem<Location> leaf : branch.getChildren()) {
            if (leaf==getTreeItem()) {
                return branch.getChildren().remove(treeItem);
            }else{
                if(recursiveRemoveNullId(leaf)) return true;
            }
        }
        return false;
    }

    @BsonRemove
    private boolean recursiveRemove(TreeItem<Location> branch) {
        for (TreeItem<Location> leaf : branch.getChildren()) {
            if (getId().equals(leaf.getValue().getId())) {
                return branch.getChildren().remove(treeItem);
            }else{
                if(recursiveRemove(leaf)) return true;
            }
        }
        return false;
    }

    @BsonRemove
    public boolean addTo(TreeItem<Location> root){
        if(getParent()==null || getParent().getId()==null){
            return root.getChildren().add(treeItem);
        }
        return recursiveAdd(root);
    }

    @BsonRemove
    private boolean recursiveAdd(TreeItem<Location> branch) {
        for (TreeItem<Location> leaf : branch.getChildren()) {
            if (getParent().getId().equals(leaf.getValue().getId())) {
                return leaf.getChildren().add(treeItem);
            }else{
                if(recursiveAdd(leaf)) return true;
            }
        }
        return false;
    }

    @BsonRemove
    public List<Location> withAllChildren(){
        List<Location> children= FXCollections.observableArrayList(allChildren());
        children.add(this);
        return children;
    }

    @BsonRemove
    public List<Location> allChildren(){
        List<Location> children=Location.COLLECTION.readableAndSortableList.filtered(
                location -> getId().equals(location.getParentId()));
        children.forEach(child-> children.addAll(child.allChildren()));
        return children;
    }

    private final TreeItem<Location> treeItem=new TreeItem<>();
    {
        treeItem.setValue(this);
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
    private final ReadOnlyStringWrapper toString=new ReadOnlyStringWrapper();
    private final SimpleStringProperty picture=new SimpleStringProperty();
    private final ReadOnlyObjectWrapper<Image> image=new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper containsImage=new ReadOnlyBooleanWrapper();
    private final SimpleObjectProperty<Location> parent=new SimpleObjectProperty<>();
    private final SimpleStringProperty details=new SimpleStringProperty();
    private final ObservableToStringList<SimpleDoubleProperty> coordinateLimits=new ObservableToStringList<>(THE_DOUBLE_CONVERTER);

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
        parent.addListener((observable, oldValue, newValue) -> {
            if(removeFrom(ROOT)){
                addTo(ROOT);
            }
        });
        toString.bind(new StringBinding() {
            {
                parent.addListener((observable, oldValue, newValue) -> {
                    if(oldValue!=null){
                        unbind(oldValue.toStringProperty());
                        invalidate();
                    }
                    if(newValue!=null){
                        bind(newValue.toStringProperty());
                        invalidate();
                    }
                });
                bind(nameProperty());
                bind(coordinateLimitsProperty());
            }
            @Override
            protected String computeValue() {
                if(getParent()!=null){
                    return getParent().toString()+">"+name.get()+" "+coordinateLimitsProperty().toStringProperty().get();
                }
                return name.get()+" "+coordinateLimitsProperty().toStringProperty().get();
            }
        });
    }

    public Location(String name,String picture, String details) {
        this.id.set(new ObjectId());
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
    }

    public Location(String name,String picture, String details, List<Double> coordinateLimits) {
        this(name, picture, details);
        coordinateLimits.forEach(aDouble -> this.coordinateLimits.add(new SimpleDoubleProperty(aDouble)));
    }

    public Location(){
        this.id.set(new ObjectId());
    }

    public Location(ObjectId id,
                    String name,
                    String picture,
                    String details,
                    List<Double> coordinateLimits,
                    ObjectId parentId) {
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
        coordinateLimits.forEach(aDouble -> this.coordinateLimits.add(new SimpleDoubleProperty(aDouble)));
        if(parentId!=null){
            this.parent.set(Location.COLLECTION.getAndMakeIfMissing(parentId));
        }
        this.id.set(id);
    }

    @SuppressWarnings("unchecked")
    private static Location make(ObjectId id){
        if(id==null){
            return new Location(null,"UNKNOWN",null,"Unknown", Collections.EMPTY_LIST,null);
        }
        return new Location(id,id.toHexString(),null,"Missing",new ArrayList<>(),null);
    }

    @BsonCreator
    public static Location make(@BsonId ObjectId id,
                                @BsonProperty("name") String name,
                                @BsonProperty("picture") String picture,
                                @BsonProperty("details") String details,
                                @BsonProperty("coordinateLimits") List<Double> coordinateLimits,
                                @BsonProperty("parentId") ObjectId parentId){
        if(coordinateLimits==null){
            coordinateLimits=Collections.EMPTY_LIST;
        }
        Location location=new Location(id,name,picture,details,coordinateLimits,parentId);
        location.setExists(true);
        return location;
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


    public List<Double> getCoordinateLimits() {
        return coordinateLimits.stream().map(SimpleDoubleProperty::get).collect(Collectors.toList());
    }

    public void setCoordinateLimits(List<Double> coordinateLimits){
        this.coordinateLimits.clear();
        coordinateLimits.forEach(aDouble -> this.coordinateLimits.add(new SimpleDoubleProperty(aDouble)));
    }

    @BsonRemove
    public ObservableToStringList<SimpleDoubleProperty> coordinateLimitsProperty() {
        return coordinateLimits;
    }

    @Override
    @BsonRemove
    public String toString() {
        return toString.get();
    }

    @BsonRemove
    public ReadOnlyStringProperty toStringProperty(){
        return toString.getReadOnlyProperty();
    }

    @Override
    public String getPicture() {
        return picture.get();
    }

    @Override
    public void setPicture(String pictureLink) {
        picture.set(pictureLink);
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

    @BsonRemove
    @Override
    public Location cloneObjectData() {
        return new Location(new ObjectId(),getName(),getPicture(),getDetails(),getCoordinateLimits(),getParentId());
    }

    @BsonRemove
    @Override
    public Location cloneObjectFully() {
        Location clone=new Location(getId(),getName(),getPicture(),getDetails(),getCoordinateLimits(),getParentId());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Location value) {
        setName(value.getName());
        setPicture(value.getPicture());
        setDetails(value.getDetails());
        setCoordinateLimits(value.getCoordinateLimits());
        setParent(value.getParent());
    }

    @Override
    @BsonRemove
    public void setFully(Location value) {
        setData(value);
        setId(value.getId());
        setExists(value.getExists());
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

    @BsonRemove
    public Location getParent() {
        return parent.get();
    }

    @BsonRemove
    public SimpleObjectProperty<Location> parentProperty() {
        return parent;
    }

    @BsonRemove
    public void setParent(Location parent) {
        this.parent.set(parent);
    }

    public ObjectId getParentId(){
        return parent.get()==null?null:parent.get().getId();
    }

    public void setParentId(ObjectId id){
        parent.set(id == null ? null : Location.COLLECTION.getAndMakeIfMissing(id));
    }

    @BsonRemove
    public TreeItem<Location> getTreeItem(){
        return treeItem;
    }
}
