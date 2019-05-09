package com.dgs.dapc.itemDB.headless.db.pojo.topLevel;

import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.properties.ObservableBoundMapList;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * Use Case group
 */
@BsonDiscriminator("Designation")
public class Designation implements INamed, IDetailed, IIdentifiable, IPictured, ICloneable<Designation>,ISettable<Designation>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='D';
    public static final ObservableBoundMapList<Designation> COLLECTION =new ObservableBoundMapList<>(Designation::make);

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
    private final SimpleStringProperty details=new SimpleStringProperty();

    public Designation(ObjectId id,
                       String name,
                       String picture,
                       String details) {
        this.id.set(id);
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
    }

    public Designation(String name, String picture, String details) {
        this.id.set(new ObjectId());
        this.name.set(name);
        this.picture.set(picture);
        this.details.set(details);
    }

    public Designation(){
        this.id.set(new ObjectId());
    }

    private static Designation make(ObjectId id){
        if(id==null){
            return new Designation(null,"UNKNOWN",null,"Unknown");
        }
        return new Designation(id,id.toHexString(),null,"Missing");
    }

    @BsonCreator
    public static Designation make(@BsonId ObjectId id,
                                   @BsonProperty("name") String name,
                                   @BsonProperty("picture") String picture,
                                   @BsonProperty("details") String details){
        Designation designation=new Designation(id,name,picture,details);
        designation.setExists(true);
        return designation;
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

    @Override
    @BsonRemove
    public String toString() {
        return name.get();
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

    @Override
    @BsonRemove
    public Designation cloneObjectData() {
        return new Designation(new ObjectId(),getName(),getPicture(),getDetails());
    }

    @Override
    @BsonRemove
    public Designation cloneObjectFully() {
        Designation clone=new Designation(getId(),getName(),getPicture(),getDetails());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Designation value) {
        setName(value.getName());
        setPicture(value.getPicture());
        setDetails(value.getDetails());
    }

    @Override
    @BsonRemove
    public void setFully(Designation value) {
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
}
