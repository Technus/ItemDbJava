package com.dgs.dapc.itemDB.headless;

import com.dgs.dapc.itemDB.Utility;
import com.dgs.dapc.itemDB.headless.db.IIdentifiable;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValueCodec;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.*;
import com.github.technus.dbAdditions.mongoDB.MongoClientHandler;
import com.github.technus.dbAdditions.mongoDB.SafePOJO;
import com.github.technus.dbAdditions.mongoDB.codecs.ClassCodec;
import com.github.technus.dbAdditions.mongoDB.codecs.FilePathCodec;
import com.github.technus.dbAdditions.mongoDB.conventions.NullableConvention;
import com.github.technus.dbAdditions.mongoDB.conventions.OptionalConvention;
import com.github.technus.dbAdditions.mongoDB.fsBackend.FileSystemCollection;
import com.github.technus.dbAdditions.mongoDB.fsBackend.MongoFSBackendException;
import com.github.technus.dbAdditions.mongoDB.pojo.ConnectionConfiguration;
import com.github.technus.dbAdditions.mongoDB.pojo.ThrowableLog;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.result.DeleteResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.technus.dbAdditions.mongoDB.pojo.ThrowableLog.THROWABLE_LOG_COLLECTION_CODECS;

public class MainLogic implements AutoCloseable {
    /**
     * Requires additional Index!
     * db.itemsBlob.createIndex(
     *    {"placements._id": 1},
     *    {unique: true, partialFilterExpression: {"placements._id": {$type: "objectId"}}}
     * )
     */
    private String collation;
    private final String[] args;
    private MongoClientHandler remoteClient;
    private final Consumer<Throwable> throwableConsumer;
    private MongoCollection<ThrowableLog> throwableCollectionLocal;
    private MongoCollection<ThrowableLog> throwableCollectionRemote;

    private MongoCollection<Contact> contactCollection;
    private MongoCollection<Designation> designationCollection;
    private MongoCollection<Item> itemsCollection;
    private MongoCollection<Location> locationCollection;
    private MongoCollection<Tag> tagCollection;


    public MainLogic(List<String> parameters){
        this(parameters == null ? new String[0] : parameters.toArray(new String[0]));
    }

    public MainLogic(String... args) {
        this.args=args==null?new String[0]:args;
        ThrowableLog.currentApplicationName ="ItemDB";
        throwableConsumer = throwable -> {
            throwable.printStackTrace();
            if (throwableCollectionLocal != null) {
                try {
                    ThrowableLog log;
                    try {
                        log = new ThrowableLog(throwable);
                    } catch (Exception e) {
                        new Exception("Unable to create full throwable log", e).printStackTrace();
                        log = new ThrowableLog(throwable, 10);
                    }
                    try {
                        throwableCollectionLocal.insertOne(log);//todo fix?
                        //throwable.printStackTrace();//todo uncomment
                    } catch (Exception e) {
                        new MongoFSBackendException("Unable to insert throwable log", e).printStackTrace();
                    }
                } catch (Throwable t) {
                    new MongoFSBackendException("Failed to log error", t).printStackTrace();
                }
                if (throwableCollectionRemote != null) {
                    for (ThrowableLog log : throwableCollectionLocal.find()) {
                        ObjectId id = log.getId();
                        try {
                            log.setId(null);
                            throwableCollectionRemote.insertOne(log);
                        } catch (Exception e) {
                            new MongoException("Couldn't insert throwable log to remote",e).printStackTrace();
                            //just to be sure that there is no infinite work to do not log this error
                            break;
                        }
                        try{
                            DeleteResult result=throwableCollectionLocal.deleteOne(new Document().append("_id", id));
                            if(!result.wasAcknowledged() || result.getDeletedCount()!=1){
                                new MongoFSBackendException(
                                        "Invalid deletion detected "+result.wasAcknowledged()+" " +result.getDeletedCount(),null)
                                        .printStackTrace();
                            }
                        }catch (Exception e){
                            new MongoFSBackendException("Couldn't delete throwable log from local",e).printStackTrace();
                            //just to be sure that there is no infinite work to do do not log this error
                            break;
                        }
                    }
                }
            }
        };
        initialize();
    }

    private void initialize() {
        CodecRegistry initializerRegistry = SafePOJO.buildCodecRegistryWithOtherClassesOrCodecs(
                ApplicationInitializer.class, ApplicationInitializer.class, ConnectionConfiguration.class);

        ApplicationInitializer applicationInitializer;
        File initializerFile;
        if (args.length > 0 && args[0] != null && args[0].endsWith('.' + FileSystemCollection.EXTENSION)) {
            initializerFile = new File(args[0]).getAbsoluteFile();
        }else{
            initializerFile = new File("defaultInitializer." + FileSystemCollection.EXTENSION).getAbsoluteFile();
        }

        if (initializerFile.isFile()) {
            try {
                applicationInitializer = SafePOJO.decode(BsonDocument.parse(new String(
                        Files.readAllBytes(initializerFile.toPath()))), ApplicationInitializer.class, initializerRegistry);
            }catch (IOException e){
                throw new InitializationError("Unable to read initializer "+initializerFile.getAbsolutePath(),e);
            }catch (Exception e){
                throw new InitializationError("Unable to decode initializer",e);
            }
        } else {
            applicationInitializer = new ApplicationInitializer();
            BsonDocument initializerDocument = SafePOJO.encode(applicationInitializer, ApplicationInitializer.class, initializerRegistry);
            try {
                Files.write(initializerFile.toPath(), initializerDocument.toJson(JsonWriterSettings.builder().indent(true).build()).getBytes());
            }catch (IOException e){
                logError(new InitializationException("Couldn't write initializer to "+initializerFile.getAbsolutePath(),e));
            }catch (Exception e){
                logError(new InitializationException("Couldn't encode initializer",e));
            }
        }

        try {
            Locale.setDefault(Locale.forLanguageTag(applicationInitializer.getLanguageTag()));
        } catch (Exception e) {
            logError(new InitializationException("Couldn't set locale, using default en_US",e));
            Locale.setDefault(Locale.US);
        }

        try {
            collation=applicationInitializer.getCollation();
        } catch (Exception e) {
            collation=Locale.getDefault().getLanguage();
        }

        localPath=applicationInitializer.getLocalFilesPath();

        File localThrowableFolder=new File(localPath).getAbsoluteFile();
        throwableCollectionLocal = new FileSystemCollection<>(localThrowableFolder,
                new MongoNamespace("tecAppsLocal", ThrowableLog.class.getSimpleName()), ThrowableLog.class)
                .withCodecRegistry(THROWABLE_LOG_COLLECTION_CODECS);

        remoteClient = new MongoClientHandler(applicationInitializer.getRemote(),
                commandFailedEvent -> logError(commandFailedEvent.getThrowable()), () -> {});
        MongoDatabase remoteDatabase = remoteClient.getDatabase();

        try {
            remoteDatabase.runCommand(new Document().append("ping", ""));
        } catch (MongoTimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            logError(new MongoException("Couldn't ping database",e));
        }

        throwableCollectionRemote = remoteDatabase.getCollection("ERROR_LOGS", ThrowableLog.class)
                .withCodecRegistry(THROWABLE_LOG_COLLECTION_CODECS);

        NullableConvention nullableConvention=new NullableConvention();
        OptionalConvention optionalConvention=new OptionalConvention();

        ArrayList<Convention> conventions=new ArrayList<>(SafePOJO.CONVENTIONS);
        conventions.add(nullableConvention);
        conventions.add(optionalConvention);

        CodecRegistry codecRegistry=CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(TagValueCodec.INSTANCE, ClassCodec.INSTANCE, FilePathCodec.INSTANCE),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().conventions(conventions).register(
                        Contact.class,Designation.class,Item.class,Location.class,Tag.class//just to load discriminators
                ).automatic(true).build()));

        nullableConvention.codecRegistry=codecRegistry;
        optionalConvention.codecRegistry=codecRegistry;
        TagValueCodec.INSTANCE.codecRegistry=codecRegistry;

        MongoCollection<Document> collection=remoteDatabase.getCollection(applicationInitializer.getCollectionName()).withCodecRegistry(codecRegistry);
        itemsCollection=collection.withDocumentClass(Item.class);
        locationCollection=collection.withDocumentClass(Location.class);
        Location.COLLECTION.setQuery(objectId -> locationCollection.find(findWithMatchingClass(locationCollection).append("_id",objectId)).first());
        contactCollection =collection.withDocumentClass(Contact.class);
        Contact.COLLECTION.setQuery(objectId -> contactCollection.find(findWithMatchingClass(contactCollection).append("_id",objectId)).first());
        tagCollection=collection.withDocumentClass(Tag.class);
        Tag.COLLECTION.setQuery(objectId -> tagCollection.find(findWithMatchingClass(tagCollection).append("_id",objectId)).first());
        designationCollection=collection.withDocumentClass(Designation.class);
        Designation.COLLECTION.setQuery(objectId -> designationCollection.find(findWithMatchingClass(designationCollection).append("_id",objectId)).first());
    }

    @Override
    public void close() {
        remoteClient.close();
    }

    public void logError(Throwable t) {
        if (throwableConsumer == null) {
            t.printStackTrace();
        } else {
            throwableConsumer.accept(t);
        }
    }

    public MongoCollection<Item> getItemsCollection() {
        return itemsCollection;
    }

    public MongoCollection<Location> getLocationCollection() {
        return locationCollection;
    }

    public void reloadLocationCollection(){
        reloadCollection(Location.COLLECTION.map,getLocationCollection());
    }

    public MongoCollection<Contact> getContactCollection() {
        return contactCollection;
    }

    public void reloadContactCollection(){
        reloadCollection(Contact.COLLECTION.map,getContactCollection());
    }

    public MongoCollection<Tag> getTagCollection() {
        return tagCollection;
    }

    public void reloadTagCollection(){
        reloadCollection(Tag.COLLECTION.map,getTagCollection());
    }

    public MongoCollection<Designation> getDesignationCollection() {
        return designationCollection;
    }

    public void reloadDesignationCollection(){
        reloadCollection(Designation.COLLECTION.map,getDesignationCollection());
    }

    public void reload(){
        reloadLocationCollection();
        reloadContactCollection();
        reloadTagCollection();
        reloadDesignationCollection();
    }

    private <T extends IIdentifiable> void  reloadCollection(Map<ObjectId, T> map, MongoCollection<T> collection){
        map.clear();
        for(T o:collection.find(findWithMatchingClass(collection)).sort(new Document("name",1)).collation(getCollation())){
            map.putIfAbsent(o.getId(),o);
        }
    }

    private static <T extends IIdentifiable> Document findWithMatchingClass(MongoCollection<T> collection){
        return new Document(
                Utility.getDiscriminatorId(collection.getDocumentClass()),
                Utility.getDiscriminatorName(collection.getDocumentClass()));
    }

    public Collation getCollation(){
        return Collation.builder().locale(collation).collationStrength(CollationStrength.SECONDARY).build();
    }

    private static String localPath;
    public static String getLocalFilesPath(){
        return localPath;
    }
}
