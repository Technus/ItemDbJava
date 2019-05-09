package com.dgs.dapc.itemDB.headless.db.pojo.topLevel;

import com.dgs.dapc.itemDB.headless.DoubleSI;
import com.dgs.dapc.itemDB.headless.db.*;
import com.dgs.dapc.itemDB.headless.properties.ObservableBoundMapList;
import com.github.technus.dbAdditions.mongoDB.conventions.BsonRemove;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import javafx.util.converter.*;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;

@BsonDiscriminator("Tag")
public class Tag implements INamed, IDetailed, IIdentifiable, ICloneable<Tag>,ISettable<Tag>,IExists {
    private final SimpleBooleanProperty exists=new SimpleBooleanProperty();
    public static final char PREFIX='T';
    public static final ObservableBoundMapList<Tag> COLLECTION =new ObservableBoundMapList<>(Tag::make);
    public static final ObservableList<Class> TYPES_LIST = FXCollections.observableArrayList();
    public static final ObservableList<Class> CONVERTERS_LIST = FXCollections.observableArrayList();
    public static final HashMap<Class, StringConverter> CONVERTERS= new HashMap<Class, StringConverter>(){
        @Override
        public StringConverter put(Class key, StringConverter value) {
            CONVERTERS_LIST.add(key);
            return super.put(key, value);
        }
    };
    public static final StringConverter NULL_CONVERTER=new StringConverter() {
        @Override
        public String toString(Object object) {
            return object==null?null:object.toString();
        }

        @Override
        public Object fromString(String string) {
            throw new UnsupportedOperationException("Cannot convert string to object!");
        }
    };
    static {
        CONVERTERS.put(Void.class, new StringConverter<Void>() {
            @Override
            public String toString(Void object) {
                return null;
            }

            @Override
            public Void fromString(String string) {
                return null;
            }
        });
        TYPES_LIST.add(Void.class);

        CONVERTERS.put(BigDecimal.class,new BigDecimalStringConverter());
        TYPES_LIST.add(BigDecimal.class);
        CONVERTERS.put(Decimal128.class, new StringConverter<Decimal128>() {
            final BigDecimalStringConverter decimalStringConverter=(BigDecimalStringConverter)CONVERTERS.get(BigDecimal.class);
            @Override
            public String toString(Decimal128 object) {
                return decimalStringConverter.toString(object.bigDecimalValue());
            }

            @Override
            public Decimal128 fromString(String string) {
                return new Decimal128(decimalStringConverter.fromString(string));
            }
        });
        TYPES_LIST.add(Decimal128.class);
        CONVERTERS.put(BigInteger.class,new BigIntegerStringConverter());
        TYPES_LIST.add(BigInteger.class);
        CONVERTERS.put(Boolean.class,new BooleanStringConverter());
        TYPES_LIST.add(Boolean.class);
        //CONVERTERS.put(boolean.class,new BooleanStringConverter());
        CONVERTERS.put(Byte.class,new ByteStringConverter());
        TYPES_LIST.add(Byte.class);
        //CONVERTERS.put(byte.class,new ByteStringConverter());
        CONVERTERS.put(Character.class,new CharacterStringConverter());
        TYPES_LIST.add(Character.class);
        //CONVERTERS.put(char.class,new CharacterStringConverter());
        CONVERTERS.put(Currency.class,new CharacterStringConverter());
        TYPES_LIST.add(Currency.class);
        CONVERTERS.put(Date.class,new DateStringConverter());
        TYPES_LIST.add(Date.class);
        CONVERTERS.put(LocalDate.class,new LocalDateStringConverter());
        TYPES_LIST.add(LocalDate.class);
        CONVERTERS.put(LocalDateTime.class,new LocalDateTimeStringConverter());
        TYPES_LIST.add(LocalDateTime.class);
        CONVERTERS.put(LocalTime.class,new LocalTimeStringConverter());
        TYPES_LIST.add(LocalTime.class);
        CONVERTERS.put(Instant.class, new StringConverter<Instant>() {
            final DateTimeStringConverter converter=new DateTimeStringConverter();
            @Override
            public String toString(Instant object) {
                return converter.toString(Date.from(object));
            }

            @Override
            public Instant fromString(String string) {
                return converter.fromString(string).toInstant();
            }
        });
        TYPES_LIST.add(Instant.class);
        CONVERTERS.put(String.class,new DefaultStringConverter());
        TYPES_LIST.add(String.class);
        CONVERTERS.put(Double.class,new DoubleStringConverter());
        TYPES_LIST.add(Double.class);
        //CONVERTERS.put(double.class,new DoubleStringConverter());
        CONVERTERS.put(Float.class,new FloatStringConverter());
        TYPES_LIST.add(Float.class);
        //CONVERTERS.put(float.class,new FloatStringConverter());
        CONVERTERS.put(Integer.class, new IntegerStringConverter());
        TYPES_LIST.add(Integer.class);
        //CONVERTERS.put(int.class, new IntegerStringConverter());
        CONVERTERS.put(Long.class,new LongStringConverter());
        TYPES_LIST.add(Long.class);
        //CONVERTERS.put(long.class,new LongStringConverter());
        CONVERTERS.put(Short.class,new ShortStringConverter());
        TYPES_LIST.add(Short.class);
        //CONVERTERS.put(short.class,new ShortStringConverter());
        CONVERTERS.put(ObjectId.class, new StringConverter<ObjectId>() {
            @Override
            public String toString(ObjectId object) {
                return object.toHexString();
            }

            @Override
            public ObjectId fromString(String string) {
                return new ObjectId(string);
            }
        });
        TYPES_LIST.add(ObjectId.class);
        CONVERTERS.put(Class.class, new StringConverter<Class>() {
            @Override
            public String toString(Class object) {
                return object.getName();
            }

            @Override
            public Class fromString(String string) {
                try{
                    return Class.forName(string);
                }catch (Exception e){
                    e.printStackTrace();
                    return null;
                }
            }
        });
        TYPES_LIST.add(Class.class);
        CONVERTERS.put(File.class, new StringConverter<File>() {
            @Override
            public String toString(File object) {
                return object.getPath();
            }

            @Override
            public File fromString(String string) {
                return new File(string);
            }
        });
        TYPES_LIST.add(File.class);
        CONVERTERS.put(DoubleSI.class,DoubleSI.INSTANCE);
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
    private final SimpleStringProperty details=new SimpleStringProperty();
    private final SimpleObjectProperty<Class> type =new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Class> converter =new SimpleObjectProperty<>();
    private final ReadOnlyObjectWrapper<StringConverter> stringConverter =new ReadOnlyObjectWrapper<>();
    {
        stringConverter.bind(new ObjectBinding<StringConverter>() {
            {
                bind(converter);
            }

            @Override
            protected StringConverter computeValue() {
                return CONVERTERS.getOrDefault(converter.get(),NULL_CONVERTER);
            }
        });
    }

    public Tag(ObjectId id,
               String name,
               String details,
               Class type,
               Class stringConverter) {
        this.id.set(id);
        this.name.set(name);
        this.details.set(details);
        this.type.set(type);
        this.converter.set(stringConverter);
    }

    public Tag(){
        this.id.set(new ObjectId());
    }

    public Tag(String name, String details, Class type) {
        this(name,details,type,type);
    }

    public Tag(String name, String details, Class type,Class converter) {
        this.id.set(new ObjectId());
        this.name.set(name);
        this.details.set(details);
        this.type.set(type);
        this.converter.set(converter);
    }

    private static Tag make(ObjectId id){
        if(id==null){
            return new Tag(null,"UNKNOWN","Unknown",null,null);
        }
        return new Tag(id,id.toHexString(),"Missing",null,null);
    }

    @BsonCreator
    public static Tag make(@BsonId ObjectId id,
                           @BsonProperty("name") String name,
                           @BsonProperty("details") String details,
                           @BsonProperty("type") Class type,
                           @BsonProperty("converter") Class stringConverter){
        Tag tag=new Tag(id,name,details,type,stringConverter);
        tag.setExists(true);
        return tag;
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

    public Class getType(){
        return type.get();
    }

    public void setType(Class type){
        this.type.set(type);
    }

    @BsonRemove
    public SimpleObjectProperty<Class> typeProperty(){
        return type;
    }

    @BsonRemove
    public ReadOnlyObjectProperty<StringConverter> stringConverterProperty(){
        return stringConverter.getReadOnlyProperty();
    }

    @BsonRemove
    public StringConverter getStringConverter(){
        return stringConverter.get();
    }

    public Class getConverter() {
        return converter.get();
    }

    @BsonRemove
    public SimpleObjectProperty<Class> converterProperty() {
        return converter;
    }

    public void setConverter(Class converter) {
        this.converter.set(converter);
    }

    @Override
    @BsonRemove
    public Tag cloneObjectData() {
        return new Tag(new ObjectId(),getName(),getDetails(),getType(),getConverter());
    }

    @Override
    @BsonRemove
    public Tag cloneObjectFully() {
        Tag clone=new Tag(getId(),getName(),getDetails(),getType(),getConverter());
        clone.setExists(getExists());
        return clone;
    }

    @Override
    @BsonRemove
    public void setData(Tag value) {
        setName(value.getName());
        setDetails(value.getDetails());
        setType(value.getType());
        setConverter(value.getConverter());
    }

    @Override
    @BsonRemove
    public void setFully(Tag value) {
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
