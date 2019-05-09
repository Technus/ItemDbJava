package com.dgs.dapc.itemDB;

import com.dgs.dapc.itemDB.headless.MainLogic;
import com.dgs.dapc.itemDB.headless.db.cjo.child.TagValue;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Placement;
import com.dgs.dapc.itemDB.headless.db.pojo.child.Source;
import com.dgs.dapc.itemDB.headless.db.pojo.topLevel.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.util.Collections.EMPTY_LIST;

public class Importer {
    private static final String IMPORTED="Imported from Access";
    private static final MainLogic logic=new MainLogic();
    private static final Charset charset=Charset.forName("windows-1250");

    public static void main(String[] args) throws IOException {
        HashMap<Integer,Contact> contacts=new HashMap<>();
        for (String line : Files.readAllLines(new File("Suppliers.txt").toPath(), charset)) {
            if(line.length()==0)continue;
            line+="\tHECC";
            String[] fields=line.split("\\t+?");
            if(fields.length!=5){
                System.out.println("line = " + line);
            }
            String name=fields[1];
            String details=fields[2];
            String url=fields[3];
            Contact contact=new Contact(name,url,details,null);
            logic.getContactCollection().insertOne(contact);
            contacts.put(Integer.parseInt(fields[0]),contact);
        }
        HashMap<Integer, Designation> designations=new HashMap<>();
        for (String line : Files.readAllLines(new File("Processes.txt").toPath(), charset)) {
            if(line.length()==0)continue;
            line+="\tHECC";
            String[] fields=line.split("\\t+?");
            if(fields.length!=4){
                System.out.println("line = " + line);
            }
            String name=fields[1];
            String details=fields[2];
            Designation designation=new Designation(name,null,details);
            logic.getDesignationCollection().insertOne(designation);
            designations.put(Integer.parseInt(fields[0]),designation);
        }
        HashMap<Integer, Location> locations=new HashMap<>();
        for (String line : Files.readAllLines(new File("Locations.txt").toPath(), charset)) {
            if(line.length()==0) continue;
            line+="\tHECC";
            String[] fields=line.split("\\t+?");
            if(fields.length!=6){
                System.out.println("line = " + line);
            }
            String name=fields[4];
            String x=fields[2];
            String y=fields[3];

            ArrayList<Double> limits=new ArrayList<>();
            if(x.length()>0){
                limits.add(Double.parseDouble(x));
            }
            if(y.length()>0){
                limits.add(Double.parseDouble(y));
            }
            Location location=new Location(name,null,null,limits);
            logic.getLocationCollection().insertOne(location);
            locations.put(Integer.parseInt(fields[0]),location);
        }
        HashMap<String, Tag> types=new HashMap<>();
        HashMap<String, Integer> usages=new HashMap<>();
        HashMap<Item, TagValue<String>> itemsV=new HashMap<>();
        HashMap<Item,String> lines=new HashMap<>();
        for (String line : Files.readAllLines(new File("Items.txt").toPath(), charset)) {
            if (line.length() == 0) continue;
            String l = line;
            line += "\tHECC";
            String[] fields = line.split("\\t+?");
            if (fields.length != 12) {
                System.out.println("line = " + line);
            }
            //ID	ItemName(type)	Type(name)	Process	Qty	MinQty	Location	Row	Col	Supplier	Link
            Tag tag = null;
            String type = fields[1];
            if (type.length() > 0) {
                tag = types.get(type);
                if (tag == null) {
                    usages.put(type, 1);
                    tag = new Tag(type, IMPORTED, String.class);
                    types.put(tag.getName(), tag);
                } else {
                    usages.replace(type, usages.get(type) + 1);
                }
            }
            String name = fields[2];
            TagValue<String> tagValue = tag == null ? null : new TagValue<>(tag, name);

            Designation designation = null;
            if (fields[3].length() > 0) {
                designation = designations.get(Integer.parseInt(fields[3]));
            }
            Double qty = Double.parseDouble(fields[4]);
            Double minQty = Double.parseDouble(fields[5]);
            Location location = null;
            if (fields[6].length() > 0) {
                location = locations.get(Integer.parseInt(fields[6]));
            }
            String x = fields[7];
            String y = fields[8];
            ArrayList<Double> coordinates = new ArrayList<>();
            if (x.length() > 0) {
                coordinates.add(Double.parseDouble(x));
            }
            if (y.length() > 0) {
                coordinates.add(Double.parseDouble(y));
            }
            Contact supplier = null;
            if (fields[9].length() > 0) {
                supplier = contacts.get(Integer.parseInt(fields[9]));
            }
            String url = fields[10];

            //placement/source/type/Item
            //TagValue tagValue=tag!=null?new TagValue(tag,name):;

            Source source = supplier == null ? null : new Source(supplier, null, null, url);
            @SuppressWarnings("unchecked")
            Placement placement = new Placement(qty, minQty, 0D, location, coordinates, null, null, designation == null ? EMPTY_LIST : Collections.singletonList(designation), null);

            Item item = new Item(tag == null ? name : tag.getName(), null, null,
                    supplier == null ? EMPTY_LIST : Collections.singletonList(supplier),
                    source == null ? EMPTY_LIST : Collections.singletonList(source),
                    Collections.singletonList(placement),
                    EMPTY_LIST);

            itemsV.put(item, tagValue);
            lines.put(item, l);
        }
        usages.forEach((k,v)->{
            if (v > 1) {
                logic.getTagCollection().insertOne(types.get(k));
            }else{
                types.remove(k);
            }
        });
        itemsV.forEach((item, tagValue) -> {
            if(tagValue!=null) {
                if (types.containsKey(tagValue.getName())) {
                    item.tagsProperty().map.put(tagValue.getId(), tagValue);
                    System.out.println(tagValue.getValue()+" "+tagValue.getName());
                }else{
                    if(tagValue.getValue()==null || tagValue.getValue().length()==0){
                        item.setName(tagValue.getName());
                    }else {
                        item.setName(tagValue.getValue());
                        item.setDetails(tagValue.getName());
                    }
                    System.out.println(item.getName()+" "+item.getDetails());
                }
                logic.getItemsCollection().insertOne(item);
            }else{
                System.out.println("Possible invalid item? = " + lines.get(item));
            }
        });
    }
}
