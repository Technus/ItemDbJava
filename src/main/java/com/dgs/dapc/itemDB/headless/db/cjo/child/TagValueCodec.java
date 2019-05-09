package com.dgs.dapc.itemDB.headless.db.cjo.child;

import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.Tag;
import com.mongodb.MongoClient;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class TagValueCodec implements Codec<TagValue> {
    public static final TagValueCodec INSTANCE=new TagValueCodec();
    public CodecRegistry codecRegistry= MongoClient.getDefaultCodecRegistry();

    @SuppressWarnings("unchecked")
    @Override
    public void encode(BsonWriter writer, TagValue value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeObjectId("tag",value.getId());
        if(value.getTag()!=null && value.getTag().getType()!=null && value.getTag().getType()!=Void.class  && value.getValue()!=null){
            writer.writeName("value");
            encoderContext.encodeWithChildContext(codecRegistry.get(value.getTag().getType()),writer,value.getValue());
        }
        writer.writeEndDocument();
    }

    @SuppressWarnings("unchecked")
    @Override
    public TagValue decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        Tag tag=Tag.COLLECTION.getAndMakeIfMissing(reader.readObjectId("tag"));
        if(reader.readBsonType()==BsonType.END_OF_DOCUMENT) {
            reader.readEndDocument();
        }else if("value".equals(reader.readName())){
            Object o=decoderContext.decodeWithChildContext(codecRegistry.get(tag.getType()), reader);
            reader.readEndDocument();
            TagValue tagValue=new TagValue(tag, o);
            tagValue.setExists(true);
            return tagValue;
        }
        TagValue tagValue=new TagValue(tag, null);
        tagValue.setExists(true);
        return tagValue;
    }

    @Override
    public Class<TagValue> getEncoderClass() {
        return TagValue.class;
    }
}
