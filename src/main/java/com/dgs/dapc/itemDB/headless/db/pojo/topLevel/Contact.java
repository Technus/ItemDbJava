package com.dgs.dapc.itemDB.headless.db.pojo.topLevel;

import com.dgs.dapc.itemDB.headless.MainLogic;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.properties.NamedUrlProperty;
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

import java.io.File;
import java.nio.file.Files;

@BsonDiscriminator("Contact")
public class Contact implements IDetailed, INamed, ILinked, IIdentifiable, IPictured,ICloneable<Contact>,ISettable<Contact>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='C';
    public static final ObservableBoundMapList<Contact> COLLECTION =new ObservableBoundMapList<>(Contact::make);

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
    private final NamedUrlProperty url=new NamedUrlProperty();
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

    public Contact(ObjectId id,
                   String name,
                   String details,
                   String picture,
                   String url) {
        this.id.set(id);
        this.name.set(name);
        this.details.set(details);
        this.url.set(url);
        this.picture.set(picture);
    }

    public Contact(){
        this.id.set(new ObjectId());
    }

    public Contact(String name, String details, String url,String picture) {
        this.id.set(new ObjectId());
        this.name.set(name);
        this.details.set(details);
        this.url.set(url);
        this.picture.set(picture);
    }

    private static Contact make(ObjectId id){

        if(id==null){
            return new Contact(null,"UNKNOWN","Unknown",null,null);
        }
        return new Contact(id,id.toHexString(),"Missing",null,null);
    }

    @BsonCreator
    public static Contact make(@BsonId ObjectId id,
                               @BsonProperty("name") String name,
                               @BsonProperty("details") String details,
                               @BsonProperty("picture") String picture,
                               @BsonProperty("url") String url){
        Contact contact=new Contact(id,name,details,picture,url);
        contact.setExists(true);
        return contact;
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
    public String getUrl() {
        return url.get();
    }

    @Override
    public void setUrl(String url) {
        this.url.set(url);
    }

    @Override
    @BsonRemove
    public NamedUrlProperty urlProperty() {
        return url;
    }

    @Override
    @BsonId
    public ObjectId getId() {
        return id.get();
    }

    @Override
    @BsonId
    public void setId(ObjectId id){
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
    @BsonRemove
    public Contact cloneObjectData() {
        return new Contact(new ObjectId(),getName(),getDetails(),getPicture(),getUrl());
    }

    @Override
    @BsonRemove
    public Contact cloneObjectFully() {
        Contact clone=new Contact(getId(),getName(),getDetails(),getPicture(),getUrl());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Contact value) {
        setName(value.getName());
        setDetails(value.getDetails());
        setPicture(value.getPicture());
        setUrl(value.getUrl());
    }
    @Override
    @BsonRemove
    public void setFully(Contact value) {
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
