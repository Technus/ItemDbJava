package com.dgs.dapc.itemDB.headless.db.pojo.child;

import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Designation;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Location;
import com.dgs.dapc.itemDB.headless.properties.ObservableNamedObjectsList;
import com.dgs.dapc.itemDB.headless.properties.ObservableToStringList;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.dgs.dapc.itemDB.Utility.THE_DOUBLE_CONVERTER;

/**
 * Where this group is placed and count requirements
 */
public class Placement implements INamed, IDetailed, IStock, ISerial, IPictured, ICloneable<Placement>,ISettable<Placement>,IIdentifiable,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='P';
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
    private final SimpleStringProperty details=new SimpleStringProperty();
    private final SimpleDoubleProperty count=new SimpleDoubleProperty(0D);
    private final SimpleDoubleProperty minCount=new SimpleDoubleProperty(0D);
    private final SimpleDoubleProperty ordered=new SimpleDoubleProperty(0D);
    private final SimpleObjectProperty<Location> location =new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper locationName=new ReadOnlyStringWrapper();
    private final ObservableToStringList<SimpleDoubleProperty> coordinates= new ObservableToStringList<>(THE_DOUBLE_CONVERTER);
    {
        locationName.bind(new StringBinding() {
            {
                location.addListener((observable, oldValue, newValue) -> {
                    if (oldValue!=null){
                        unbind(oldValue.toStringProperty());
                        invalidate();
                    }
                    if(newValue!=null){
                        bind(newValue.toStringProperty());
                        invalidate();
                    }
                });
            }
            @Override
            protected String computeValue() {
                return location.get()==null?null:location.get().toString();
            }
        });
    }
    private final ObservableNamedObjectsList<Designation> designations =new ObservableNamedObjectsList<>();
    private final SimpleStringProperty serial =new SimpleStringProperty();

    private final ReadOnlyBooleanWrapper stockOptimalProperty =new ReadOnlyBooleanWrapper();
    @BsonRemove
    public boolean isStockOptimal(){
        return stockOptimalProperty.get();
    }
    {
        stockOptimalProperty.bind(new BooleanBinding() {
            {
                bind(countProperty());
                bind(minCountProperty());
            }
            @Override
            protected boolean computeValue() {
                return getCount()>0 && getCount()>=getMinCount();
            }
        });
    }
    @BsonRemove
    @Override
    public ReadOnlyBooleanProperty stockOptimalProperty() {
        return stockOptimalProperty.getReadOnlyProperty();
    }


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
                try {
                    return new Image(picture.get());
                }catch (Exception e){
                    return null;
                }
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

    public Placement(Location location, List<Double> coordinates, String name, String details){
        this.location.set(location);
        coordinates.forEach(aDouble -> this.coordinates.add(new SimpleDoubleProperty(aDouble)));
        this.name.set(name);
        this.details.set(details);
        this.id.set(new ObjectId());
    }

    public Placement(Double count, Double minCount, Double ordered, Location location, List<Double> coordinates, String name, String details, List<Designation> designations,String serial) {
        this(location,coordinates,name,details);
        this.count.set(count);
        this.minCount.set(minCount);
        this.ordered.set(ordered);
        this.designations.addAll(designations);
        this.serial.set(serial);
    }
    public Placement(ObjectId id,Location location, List<Double> coordinates, String name, String details){
        this.location.set(location);
        coordinates.forEach(aDouble -> this.coordinates.add(new SimpleDoubleProperty(aDouble)));
        this.name.set(name);
        this.details.set(details);
        this.id.set(id);
    }

    public Placement(ObjectId id,Double count, Double minCount, Double ordered, Location location, List<Double> coordinates, String name, String details, List<Designation> designations,String serial,String picture) {
        this(id,location,coordinates,name,details);
        this.count.set(count);
        this.minCount.set(minCount);
        this.ordered.set(ordered);
        this.designations.addAll(designations);
        this.serial.set(serial);
        this.picture.set(picture);
    }

    public Placement(){
        id.set(new ObjectId());
    }

    @BsonCreator
    public static Placement make(@BsonId ObjectId id,
                    @BsonProperty("count") Double count,
                     @BsonProperty("minCount") Double minCount,
                     @BsonProperty("ordered") Double ordered,
                     @BsonProperty("locationId") ObjectId locationId,
                     @BsonProperty("coordinates") List<Double> coordinates,
                     @BsonProperty("name") String name,
                     @BsonProperty("details") String details,
                     @BsonProperty("designationsId") List<ObjectId> designationsID,
                     @BsonProperty("serial") String serial,
                     @BsonProperty("picture") String picture) {
        if(designationsID==null){
            designationsID= Collections.EMPTY_LIST;
        }
        Placement placement=new Placement(id,
                count,minCount,ordered,
                Location.COLLECTION.getAndMakeIfMissing(locationId),
                coordinates,name,details,
                designationsID.stream().map(Designation.COLLECTION::getAndMakeIfMissing).collect(Collectors.toList()),
                serial,picture);
        placement.setExists(true);
        return placement;
    }

    //@BsonRemove
    //public String getToStringLocation() {
    //    return toStringLocation.get();
    //}

    //@BsonRemove
    //public ReadOnlyStringProperty getToStringLocationProperty(){
    //    return toStringLocation.getReadOnlyProperty();
    //}

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
    public Double getOrdered() {
        return ordered.get();
    }

    @Override
    public Double getCount() {
        return count.get();
    }

    @Override
    public void setOrdered(Double ordered) {
        this.ordered.set(ordered);
    }

    @Override
    public void setCount(Double count) {
        this.count.set(count);
    }

    @Override
    @BsonRemove
    public SimpleDoubleProperty countProperty() {
        return count;
    }


    @Override
    public Double getMinCount() {
        return minCount.get();
    }

    @Override
    @BsonRemove
    public SimpleDoubleProperty orderedProperty() {
        return ordered;
    }

    @Override
    public void setMinCount(Double minCount) {
        this.minCount.set(minCount);
    }

    @Override
    @BsonRemove
    public SimpleDoubleProperty minCountProperty() {
        return minCount;
    }


    public ObjectId getLocationId() {
        return location.get()!=null?location.get().getId():null;
    }

    public void setLocationId(ObjectId locationId){
        location.set(Location.COLLECTION.getAndMakeIfMissing(locationId));
    }

    @BsonRemove
    public SimpleObjectProperty<Location> locationProperty() {
        return location;
    }

    @BsonRemove
    public Location getLocation() {
        return location.get();
    }

    @BsonRemove
    public void setLocation(Location location) {
        this.location.set(location);
    }


    public List<Double> getCoordinates() {
        return coordinates.stream().map(SimpleDoubleProperty::get).collect(Collectors.toList());
    }

    public void setCoordinates(List<Double> coordinates){
        this.coordinates.clear();
        coordinates.forEach(aDouble -> this.coordinates.add(new SimpleDoubleProperty(aDouble)));
    }

    @BsonRemove
    public ObservableToStringList<SimpleDoubleProperty> coordinatesProperty() {
        return coordinates;
    }


    public List<ObjectId> getDesignationsId() {
        return designations.stream().map(Designation::getId).collect(Collectors.toList());
    }

    public void setDesignationsId(List<ObjectId> designationsId){
        this.designations.clear();
        designationsId.forEach(id -> this.designations.add(Designation.COLLECTION.getAndMakeIfMissing(id)));
    }

    @BsonRemove
    public ObservableNamedObjectsList<Designation> designationsProperty() {
        return designations;
    }

    @BsonRemove
    public List<Designation> getDesignations(){
        return new ArrayList<>(designations);
    }

    @BsonRemove
    public void setDesignations(List<Designation> designations){
        this.designations.clear();
        this.designations.addAll(designations);
    }

    @Override
    @BsonRemove
    public String toString() {
        return name.get();
    }

    public String getSerial(){
        return serial.get();
    }

    public void setSerial(String serial){
        this.serial.set(serial);
    }

    @BsonRemove
    public SimpleStringProperty serialProperty(){
        return serial;
    }

    @BsonRemove
    public String getLocationName(){
        return locationName.get();
    }

    @BsonRemove
    public ReadOnlyStringProperty locationNameProperty(){
        return locationName.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public Placement cloneObjectFully() {
        Placement clone=new Placement(getId(),getCount(),getMinCount(),getOrdered(),getLocation(),getCoordinates(),getName(),getDetails(),getDesignations(),getSerial(),getPicture());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public Placement cloneObjectData() {
        return new Placement(new ObjectId(),getCount(),getMinCount(),getOrdered(),getLocation(),getCoordinates(),getName(),getDetails(),getDesignations(),getSerial(),getPicture());
    }

    @Override
    @BsonRemove
    public void setData(Placement value) {
        setCount(value.getCount());
        setMinCount(value.getMinCount());
        setOrdered(value.getOrdered());
        setLocation(value.getLocation());
        setCoordinates(value.getCoordinates());
        setName(value.getName());
        setDetails(value.getDetails());
        setDesignations(value.getDesignations());
        setSerial(value.getSerial());
        setPicture(value.getPicture());
    }

    @Override
    @BsonRemove
    public void setFully(Placement value) {
        setData(value);
        setId(value.getId());
        setExists(value.getExists());
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
    public SimpleObjectProperty<ObjectId> idProperty() {
        return id;
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
}
