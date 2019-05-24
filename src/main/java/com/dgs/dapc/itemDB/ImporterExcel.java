package com.dgs.dapc.itemDB;

import com.dgs.dapc.itemDB.headless.DoubleSI;
import com.dgs.dapc.itemDB.headless.MainLogic;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.*;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;

public class ImporterExcel {
    private static final String IMPORTED="Imported from Excel";
    private static MainLogic logic;
    private static final Charset charset=Charset.forName("UTF-8");//Charset.forName("windows-1250");

    public static void main(String[] args) throws IOException {
        logic=new MainLogic(args);
        //HashMap<Integer, Designation> designations=new HashMap<>();
        //for (String line : Files.readAllLines(new File("designations.tsv").toPath(), charset)) {
        //    if(line.length()==0)continue;
        //    line+="\tHECC";
        //    String[] fields=line.split("\\t+?");
        //    if(fields.length!=3){
        //        System.out.println("line = " + line);
        //        continue;
        //    }
        //    String name=fields[0];
        //    String details=fields[1];
        //    Designation designation=new Designation(name,null,details);
        //    logic.getDesignationCollection().insertOne(designation);
        //    //designations.put(Integer.parseInt(fields[0]),designation);
        //}

        Tag lenTag=new Tag("Overall length (m)","Including unusable parts",Double.class, DoubleSI.class);
        Tag diaTag=new Tag("Diameter (m)","Usable diameter",Double.class, DoubleSI.class);
        Tag profileTag=new Tag("Screw drive type","Ex. https://en.wikipedia.org/wiki/List_of_screw_drives",String.class, String.class);
        logic.getTagCollection().insertOne(lenTag);
        logic.getTagCollection().insertOne(diaTag);
        logic.getTagCollection().insertOne(profileTag);

        for (String line : Files.readAllLines(new File("items_pign.tsv").toPath(), charset)) {
            if (line.length() == 0) continue;
            //line += "\tHECC";//now
            String[] fields = line.split("\\t+?");
            if (fields.length != 13) {
                System.out.println("line = " + line);
            }
            String link=fields[0];
            String name=fields[1];
            String diameterT=fields[2];
            String profileT=fields[3];
            String overallLengthT=fields[4];
            String typeT=fields[5];
            String processD=fields[6];
            String locationL="Cabinet Yellow";//fields[7];
            String rowLD=fields[8];
            String colLD=fields[9];
            String manufacturerC=fields[10];
            String supplierC=fields[11];

            link=link.length()>0?link:null;
            //name
            Double diameter=diameterT.length()>0?Double.parseDouble(diameterT)/1000:null;
            profileT=profileT.length()>0?profileT:null;
            Double length=overallLengthT.length()>0?Double.parseDouble(overallLengthT)/1000:null;
            typeT=typeT.length()>0?typeT:null;
            Designation designation=logic.getDesignationCollection().find(
                    (BasicDBObject) QueryBuilder.start()
                            .and(Utility.queryForClass(Designation.class))
                            .and("name").is(processD).get()).first();
            if(designation==null && processD.length()>0){
                System.out.println("designation = " + processD);
            }
            Location location=logic.getLocationCollection().find(
                    (BasicDBObject)QueryBuilder.start()
                            .and(Utility.queryForClass(Location.class))
                            .and("name").is(locationL).get()).first();
            if(location==null && locationL.length()>0){
                System.out.println("location = " + locationL);
            }
            Integer col=colLD.length()>0?Integer.parseInt(colLD):null;
            Integer row=rowLD.length()>0?Integer.parseInt(rowLD):null;
            Contact manufacturer=logic.getContactCollection().find(
                    (BasicDBObject)QueryBuilder.start()
                            .and(Utility.queryForClass(Contact.class))
                            .and("name").is(manufacturerC).get()).first();
            if(manufacturer==null && manufacturerC.length()>0){
                System.out.println("manufacturer = " + manufacturerC);
            }
            Contact supplier=logic.getContactCollection().find(
                    (BasicDBObject)QueryBuilder.start()
                            .and(Utility.queryForClass(Contact.class))
                            .and("name").is(supplierC).get()).first();
            if(supplier==null && supplierC.length()>0){
                System.out.println("supplier = " + supplierC);
            }

            Source source=new Source(supplier,null,null,link);
            if(supplier==null && link==null){
                source=null;
            }

            String childName=col==null?(row==null?(null):("R"+row)):(row==null?("C"+col):("R"+row+" C"+col));

            Location locationChild=null;
            if(location!=null) {
                locationChild = logic.getLocationCollection().find(
                        (BasicDBObject) QueryBuilder.start()
                                .and(Utility.queryForClass(Location.class))
                                .and("name").is(childName)
                                .and("parentId").is(location.getId()).get()).first();
            }
            if(locationChild==null){
                System.out.println("locationChild = " + childName);
            }

            Placement placement=new Placement(locationChild,Collections.EMPTY_LIST,null,null);
            placement.setDesignations(designation==null?Collections.EMPTY_LIST:Collections.singletonList(designation));

            ArrayList<TagValue> tags=new ArrayList<>();
            if(diameter!=null){
                tags.add(new TagValue<>(diaTag,diameter));
            }
            if(length!=null){
                tags.add(new TagValue<>(lenTag,length));
            }
            if(profileT!=null){
                tags.add(new TagValue<>(profileTag,profileT));
            }

            Item item=new Item(typeT==null?name:name+' '+typeT,
                    null,
                    null,
                    manufacturer==null? Collections.EMPTY_LIST:Collections.singletonList(manufacturer),
                    source==null?Collections.EMPTY_LIST:Collections.singletonList(source),
                    placement==null?Collections.EMPTY_LIST:Collections.singletonList(placement),
                    tags);

            logic.getItemsCollection().insertOne(item);
        }
    }
}
