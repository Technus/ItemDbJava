package com.dgs.dapc.itemDB.headless.db.pojo.child;

import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Contact;
import com.dgs.dapc.itemDB.headless.properties.NamedUrlProperty;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * Exact source for a certain item
 */
public class Source implements INamed,IDetailed,ILinked, ICloneable<Source>, ISettable<Source>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    private final SimpleObjectProperty<Contact> supplier =new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper supplierName =new ReadOnlyStringWrapper();
    {
        supplierName.bind(new StringBinding() {
            {
                supplier.addListener((observable, oldValue, newValue) -> {
                    if(oldValue!=null){
                        unbind(oldValue.nameProperty());
                        invalidate();
                    }
                    if (newValue!=null){
                        bind(newValue.nameProperty());
                        invalidate();
                    }
                });
            }
            @Override
            protected String computeValue() {
                return supplier.get()==null?null:supplier.get().getName();
            }
        });
    }
    private final SimpleStringProperty name=new SimpleStringProperty();
    private final SimpleStringProperty details=new SimpleStringProperty();
    private final NamedUrlProperty url=new NamedUrlProperty();

    public Source(Contact supplier, String name, String details, String url) {
        this.supplier.set(supplier);
        this.name.set(name);
        this.details.set(details);
        this.url.set(url);
    }

    public Source(){}

    @BsonCreator
    public static Source make(@BsonProperty("supplierId") ObjectId supplierId,
                              @BsonProperty("name") String name,
                              @BsonProperty("details") String details,
                              @BsonProperty("url") String url) {
        Source source= new Source(Contact.COLLECTION.getAndMakeIfMissing(supplierId),name,details,url);
        source.setExists(true);
        return source;
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

    public ObjectId getSupplierId() {
        return supplier.get()!=null?supplier.get().getId():null;
    }

    public void setSupplierId(ObjectId supplierId){
        this.supplier.set(Contact.COLLECTION.getAndMakeIfMissing(supplierId));
    }

    @BsonRemove
    public SimpleObjectProperty<Contact> supplierProperty() {
        return supplier;
    }

    @BsonRemove
    public Contact getSupplier(){
        return supplier.get();
    }

    @BsonRemove
    public void setSupplier(Contact contact){
        supplier.set(contact);
    }

    @Override
    @BsonRemove
    public String toString() {
        return name.get();
    }

    @BsonRemove
    public String getSupplierName(){
        return supplierName.get();
    }

    @BsonRemove
    public ReadOnlyStringProperty supplierNameProperty(){
        return supplierName.getReadOnlyProperty();
    }

    @Override
    @BsonRemove
    public Source cloneObjectData() {
        return new Source(getSupplier(),getName(),getDetails(),getUrl());
    }

    @Override
    @BsonRemove
    public Source cloneObjectFully() {
        Source clone=cloneObjectData();
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Source value) {
        setSupplier(value.getSupplier());
        setName(value.getName());
        setDetails(value.getDetails());
        setUrl(value.getUrl());
    }

    @Override
    @BsonRemove
    public void setFully(Source value) {
        setData(value);
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
