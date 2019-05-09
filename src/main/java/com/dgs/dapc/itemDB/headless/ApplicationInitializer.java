package com.dgs.dapc.itemDB.headless;

import com.github.technus.dbAdditions.functionalInterfaces.ITimedModification;
import com.github.technus.dbAdditions.mongoDB.pojo.ConnectionConfiguration;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

public class ApplicationInitializer implements ITimedModification {
    @BsonId
    private final String id;
    private final String localFilesPath;
    private final String collectionName;
    private final ConnectionConfiguration remote;
    private final String languageTag;
    private final Instant timestamp;
    private final String collation;

    public ApplicationInitializer(){
        this(false);
    }

    public ApplicationInitializer(boolean usingOldTimestamp){
        id="defaultInitializer";
        localFilesPath=".";
        remote=new ConnectionConfiguration("localhost","tecAppsRemote");
        languageTag ="en-US";
        collectionName ="itemsBlob";
        timestamp=usingOldTimestamp?Instant.ofEpochMilli(0):Instant.now();
        collation="pl";
    }

    @BsonCreator
    public ApplicationInitializer(
            @BsonId String id,
            @BsonProperty("localFilesPath") String localFilesPath,
            @BsonProperty("collectionName") String collectionName,
            @BsonProperty("remote") ConnectionConfiguration remote,
            @BsonProperty("languageTag") String languageTag,
            @BsonProperty("timestamp") Instant timestamp,
            @BsonProperty("collation") String collation) {
        this.id = id;
        this.localFilesPath = localFilesPath;
        this.collectionName = collectionName;
        this.remote = remote;
        this.languageTag = languageTag;
        this.timestamp=timestamp;
        this.collation=collation;
    }

    public String getId() {
        return id;
    }

    public String getLocalFilesPath() {
        return localFilesPath;
    }

    public ConnectionConfiguration getRemote() {
        return remote;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getCollation(){
        return collation;
    }
}
