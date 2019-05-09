package com.dgs.dapc.itemDB.headless.db;


import com.dgs.dapc.itemDB.Utility;
import org.bson.types.ObjectId;

public class DiscriminatedObjectId{
    public static final int decSize=34;
    public ObjectId id;
    public char discriminator;

    public DiscriminatedObjectId(char discriminator){
        this.id=new ObjectId(new byte[12]);
        this.discriminator=discriminator;
    }

    public DiscriminatedObjectId(ObjectId id,char discriminator){
        this.id=id;
        this.discriminator=discriminator;
    }

    public DiscriminatedObjectId(String string){
        this(Utility.Base10.decodeString(string));
    }

    private DiscriminatedObjectId(byte[] bytes){
        if(bytes!=null && bytes.length>0){
            if(bytes.length!=15){
                byte[] temp=new byte[15];
                System.arraycopy(bytes,0,temp,Math.max(0,15-bytes.length),Math.min(bytes.length, 15));
                bytes=temp;
            }
            discriminator=(char)( (((short)bytes[1])<<8)|bytes[2] );
            byte[] oid=new byte[12];
            System.arraycopy(bytes,3,oid,0,12);
            id=new ObjectId(oid);
        }
    }

    private byte[] getBytes(){
        byte[] bytes=new byte[15];
        System.arraycopy(id.toByteArray(),0,bytes,3,12);
        bytes[1]=(byte)(discriminator>>>8);
        bytes[2]=(byte)(discriminator&0xff);
        return bytes;
    }

    @Override
    public String toString() {
        String val= Utility.Base10.encodeToString(getBytes());
        StringBuilder sb=new StringBuilder();
        while(val.length()+sb.length()<decSize){
            sb.append('0');
        }
        sb.append(val);
        return sb.toString();
    }
}
