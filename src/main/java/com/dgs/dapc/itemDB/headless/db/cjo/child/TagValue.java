package com.dgs.dapc.itemDB.headless.db.cjo.child;

import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import org.bson.types.ObjectId;

import static com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag.PREFIX;

public class TagValue<T> implements INamed, IDetailed, ITagged, ICloneable<TagValue<T>>, IIdentifiable,ISettable<TagValue<T>>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    private final SimpleObjectProperty<Tag> tag=new SimpleObjectProperty<>();
    private final SimpleObjectProperty<T> value=new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper valueString=new ReadOnlyStringWrapper();
    {
        valueString.bind(new ObjectBinding<String>() {
            {
                tag.addListener((observable, oldValue, newValue) -> {
                    if(newValue!=null){
                        bind(newValue.stringConverterProperty());
                        invalidate();
                    }
                    if(oldValue!=null){
                        unbind(oldValue.stringConverterProperty());
                        invalidate();
                    }
                });
                bind(value);
            }
            @Override
            protected String computeValue() {
                return valueToString();
            }
        });
    }
    private final ReadOnlyStringWrapper tagString =new ReadOnlyStringWrapper();
    {
        tagString.bind(new ObjectBinding<String>() {
            {
                tag.addListener((observable, oldValue, newValue) -> {
                    if(newValue!=null){
                        bind(newValue.nameProperty());
                        invalidate();
                    }
                    if(oldValue!=null){
                        unbind(oldValue.nameProperty());
                        invalidate();
                    }
                });
                bind(valueString);
            }
            @Override
            protected String computeValue() {
                if(TagValue.this.getValue()==null){
                    return (tag.get()!=null?tag.get().getName():null);
                }
                return (tag.get()!=null?tag.get().getName():null)+" = "+valueString.get();
            }
        });
    }
    private final ReadOnlyStringWrapper tagName=new ReadOnlyStringWrapper();
    {
        tagName.bind(new StringBinding() {
            {
                tag.addListener((observable, oldValue, newValue) -> {
                    if(oldValue!=null){
                        unbind(oldValue.nameProperty());
                        invalidate();
                    }
                    if(newValue!=null){
                        bind(newValue.nameProperty());
                        invalidate();
                    }
                });
            }
            @Override
            protected String computeValue() {
                return tag.get()==null?null:tag.get().getName();
            }
        });
    }
    private final ReadOnlyObjectWrapper<ObjectId> tagId=new ReadOnlyObjectWrapper<>();
    {
        tagId.bind(new ObjectBinding<ObjectId>() {
            {
                tag.addListener((observable, oldValue, newValue) -> {
                    if(oldValue!=null){
                        unbind(oldValue.idProperty());
                        invalidate();
                    }
                    if(newValue!=null){
                        bind(newValue.idProperty());
                        invalidate();
                    }
                });
            }
            @Override
            protected ObjectId computeValue() {
                return tag.get()==null?null:tag.get().getId();
            }
        });
    }
    private final ReadOnlyObjectWrapper<DiscriminatedObjectId> did=new ReadOnlyObjectWrapper<>();
    {
        did.bind(new ObjectBinding<DiscriminatedObjectId>() {
            {
                bind(tagId);
            }
            @Override
            protected DiscriminatedObjectId computeValue() {
                return new DiscriminatedObjectId(tagId.get(),PREFIX);
            }
        });
    }

    public TagValue(Tag tag, T value) {
        this.tag.set(tag);
        this.value.set(value);
    }

    public TagValue(){}

    @BsonRemove
    public Tag getTag() {
        return tag.get();
    }

    @BsonRemove
    public SimpleObjectProperty<Tag> tagProperty() {
        return tag;
    }

    @BsonRemove
    public void setTag(Tag tag) {
        this.tag.set(tag);
    }

    public T getValue() {
        return value.get();
    }

    public SimpleObjectProperty<T> valueProperty() {
        return value;
    }

    public void setValue(T value) {
        this.value.set(value);
    }

    @Override
    @BsonRemove
    public String getName() {
        return tagName.get();
    }

    @Override
    @BsonRemove
    public void setName(String name) {
        tagName.set(name);
    }

    @Override
    @BsonRemove
    public SimpleStringProperty nameProperty() {
        return tagName;
    }

    @BsonRemove
    @SuppressWarnings("unchecked")
    public String valueToString(){
        if(value.get()==null){
            if(tag.get()!=null && tag.get().getType()==Void.class){
                return "\u2713";
            }
            return null;
        }
        return tag.get()!=null?tag.get().getStringConverter().toString(value.get()):value.get().toString();
    }

    @BsonRemove
    @SuppressWarnings("unchecked")
    public T stringToValue(String value){
        try {
            return (T) tag.get().getStringConverter().fromString(value);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @BsonRemove
    public void setValueFromString(String s){
        this.value.set(stringToValue(s));
    }

    @BsonRemove
    public String getValueString(){
        return valueString.get();
    }

    @BsonRemove
    public ReadOnlyStringProperty valueStringProperty(){
        return valueString.getReadOnlyProperty();
    }

    @Override
    public String getDetails() {
        return tag.get().getDetails();
    }

    @Override
    public void setDetails(String details) {
        tag.get().setDetails(details);
    }

    @Override
    public SimpleStringProperty detailsProperty() {
        return tag.get().detailsProperty();
    }

    @Override
    @BsonRemove
    public String getTagsString() {
        return tagString.get();
    }

    @Override
    @BsonRemove
    public ReadOnlyStringProperty tagsStringProperty(){
        return tagString.getReadOnlyProperty();
    }

    @Override
    public String toString() {
        return getName()+" "+valueToString();
    }

    @Override
    @BsonRemove
    public ObjectId getId() {
        return tagId.get();
    }

    @Override
    @BsonRemove
    @Deprecated
    public void setId(ObjectId id) {
        tagId.set(id);
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
        return tagId;
    }


    @Override
    public TagValue<T> cloneObjectData() {
        return new TagValue<>(getTag(),getValue());
    }

    @Override
    public TagValue<T> cloneObjectFully() {
        TagValue<T> clone=cloneObjectData();
        clone.setExists(getExists());
        return clone;
    }


    @Override
    public void setData(TagValue<T> value) {
        setTag(value.getTag());
        setValue(value.getValue());
    }

    @Override
    public void setFully(TagValue<T> value) {
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
