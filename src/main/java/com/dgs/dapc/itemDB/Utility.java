package com.dgs.dapc.itemDB;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.dgs.dapc.itemDB.javafx.IWindowInitialize;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.conversions.Bson;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

public class Utility {
    private Utility(){}

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    //region convert
    public static String bytesToHex(byte[] bytes,int bytesPerLine,boolean split8) {
        int lim=bytesPerLine-1;
        if (bytes == null) return null;
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = j % bytesPerLine == lim ? '\n' : (split8 && j%8==7?'\t':' ');
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes,int bytesPerLine) {
        int lim=bytesPerLine-1;
        if (bytes == null) return null;
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = j % bytesPerLine == lim ? '\n' : ' ';
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes) {
        if(bytes==null) return null;
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex){
        if(hex==null) return null;
        hex=hex.toUpperCase().replaceAll("0X","").replaceAll("[^0-9A-F]","");
        if(hex.length()%2==1) {
            return null;
        }
        byte[] bytes=new byte[hex.length()>>1];
        for (int i = 0, len=bytes.length; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return bytes;
    }

    public static String doublesToStr(double[] arr){
        if(arr==null || arr.length==0) return null;
        StringBuilder stringBuilder=new StringBuilder();
        for (double v : arr) {
            stringBuilder.append(v).append(' ');
        }
        stringBuilder.setLength(stringBuilder.length()-1);
        return stringBuilder.toString();
    }

    public static double[] strToDoubles(String str){
        if(str==null) return null;
        String[] split=str.split(" ");
        double[] doubles=new double[split.length];
        for(int i=0;i<doubles.length;i++){
            try{
                doubles[i]=Double.parseDouble(split[i]);
            }catch (Exception e){
                return null;
            }
        }
        return doubles;
    }
    //endregion

    public static String throwableToString(Throwable t){
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        PrintStream printStream=new PrintStream(outputStream);
        t.printStackTrace(printStream);
        t.printStackTrace();
        try {
            outputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        return new String(outputStream.toByteArray());
    }

    public static String getDiscriminatorId(Class clazz){
        BsonDiscriminator a=(BsonDiscriminator)clazz.getAnnotation(BsonDiscriminator.class);
        if(a!=null){
            return a.key();
        }
        return "_t";
    }

    public static String getDiscriminatorName(Class clazz){
        BsonDiscriminator a=(BsonDiscriminator)clazz.getAnnotation(BsonDiscriminator.class);
        if(a!=null){
            return a.value();
        }
        return clazz.getName();
    }

    public static BsonDocument filterCollection(MongoCollection collection){
        return filterClass(collection.getDocumentClass());
    }

    public static BsonDocument filterClass(Class clazz){
        return new BsonDocument(getDiscriminatorId(clazz),new BsonString(getDiscriminatorName(clazz)));
    }

    public static BasicDBObject queryForCollection(MongoCollection collection){
        return queryForClass(collection.getDocumentClass());
    }

    public static BasicDBObject queryForClass(Class clazz){
        return (BasicDBObject)QueryBuilder.start(getDiscriminatorId(clazz)).is(getDiscriminatorName(clazz)).get();
    }

    public static String selectImage(String defaultPath){
        FileChooser fileChooser=new FileChooser();
        if(defaultPath!=null){
            try{
                File f=new File(new URI(defaultPath));
                if(f.isFile()) fileChooser.setInitialDirectory(f.getAbsoluteFile().getParentFile());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        fileChooser.setTitle("Select image");
        File file=fileChooser.showOpenDialog(null);
        return file==null?null:file.toURI().toString();
    }

    public static final StringConverter<Double> THE_DOUBLE_CONVERTER = getSafeDoubleConverter(()->0D);

    public static final StringConverter<Long> THE_LONG_CONVERTER = getSafeLongConverter(()->0L);

    public static StringConverter<Double> getSafeDoubleConverter(Supplier<Double> defaultValue){
        return new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                if (object == null) {
                    return null;
                }
                if (object.longValue() == object) {
                    return Long.toString(object.longValue());
                }
                return Double.toString(object);
            }

            @Override
            public Double fromString(String string) {
                try {
                    return string == null ? null : Double.parseDouble(string);
                }catch (NumberFormatException e){
                    //e.printStackTrace();
                    return defaultValue.get();
                }
            }
        };
    }

    public static StringConverter<Integer> getSafeIntegerConverter(Supplier<Integer> defaultValue){
        return new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null) {
                    return null;
                }
                return Integer.toString(object);
            }

            @Override
            public Integer fromString(String string) {
                try {
                    return string == null ? null : Integer.parseInt(string);
                }catch (NumberFormatException e){
                    //e.printStackTrace();
                    return defaultValue.get();
                }
            }
        };
    }

    public static StringConverter<Long> getSafeLongConverter(Supplier<Long> defaultValue){
        return new StringConverter<Long>() {
            @Override
            public String toString(Long object) {
                if (object == null) {
                    return null;
                }
                return Long.toString(object);
            }

            @Override
            public Long fromString(String string) {
                try {
                    return string == null ? null : Long.parseLong(string);
                }catch (NumberFormatException e){
                    //e.printStackTrace();
                    return defaultValue.get();
                }
            }
        };
    }

    public static final String LIST_SEPARATOR_CHAR="/";
    public static final String LIST_SEPARATOR_SPACE=" ";
    public static final String LIST_SEPARATOR=LIST_SEPARATOR_CHAR+LIST_SEPARATOR_SPACE;
    public static final String LIST_SEPARATOR_REGEX=Pattern.quote(LIST_SEPARATOR_SPACE)+"*"+
            Pattern.quote(LIST_SEPARATOR_CHAR)+Pattern.quote(LIST_SEPARATOR_SPACE)+"*";


    public static <T> StringConverter<List<T>> getListConverter(StringConverter<T> converter){
        return new StringConverter<List<T>>() {
            private StringBuilder stringBuilder=new StringBuilder();

            @Override
            public String toString(List<T> object) {
                if(object==null) return null;
                stringBuilder.setLength(0);
                object.forEach(v -> {
                    String s=v==null?null:converter.toString(v);
                    if(s!=null && s.length()>0) {
                        stringBuilder.append(s).append(Utility.LIST_SEPARATOR);
                    }
                });
                if(stringBuilder.length()>0) {
                    stringBuilder.setLength(stringBuilder.length() - Utility.LIST_SEPARATOR.length());
                    return stringBuilder.toString();
                }
                return "";
            }

            @Override
            public List<T> fromString(String string) {
                if(string==null) return null;
                ArrayList<T> list=new ArrayList<>();
                boolean valid=false;
                for (String s:string.split(LIST_SEPARATOR_REGEX)) {
                    if (s.length()>0){
                        valid=true;
                    }
                    list.add(converter.fromString(s));
                }
                return valid?list:null;
            }
        };
    }

    public static StringConverter<List<Double>> DOUBLE_LIST_CONVERTER=getListConverter(THE_DOUBLE_CONVERTER);

    public static class Window<T>{
        public final T controller;
        public final Parent root;
        public final Stage stage;

        private Window(T controller, Parent root,Stage stage){
            this.controller=controller;
            this.root=root;
            this.stage=stage;
        }
    }

    public static <T> Window<T> loadFXML(URL fxml, String title, javafx.stage.Window parent) {
        return loadFXML(fxml,title,Modality.NONE,parent);
    }

    public static <T> Window<T> loadFXML(URL fxml,String title,Modality modality,javafx.stage.Window parent) {
        try {
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            Stage stage = new Stage(StageStyle.DECORATED);
            stage.initModality(modality);
            stage.setScene(new Scene(root));
            T controller=loader.getController();
            stage.setAlwaysOnTop(true);//added
            stage.setTitle(title);
            if(parent!=null){
                //stage.initOwner(parent);
                stage.setX(parent.getX()+parent.getWidth()/5+new Random().nextDouble()*16);
                stage.setY(parent.getY()+new Random().nextDouble()*16);
            }
            if(controller instanceof IWindowInitialize){
                ((IWindowInitialize) controller).initializeStage(stage);
            }
            return new Window<>(controller,root,stage);
        }catch (IOException e){
            throw new RuntimeException("Failed to load FXML: "+fxml,e);
        }
    }

    public static class Base128 {
        public static final Charset CHARSET = Charset.forName("ISO-8859-1");
        public static final String ENCODING_CHARACTERS =          "!#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~ÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßðñòóôõö÷øùúûüýþÿÈÉÊË";
        public static final String ENCODING_CHARACTERS_EXTENDED = "!#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~ĐŃŇÓÔŐÖ×ŘŮÚŰÜÝŢßđńňóôőö÷řůúűüýţ˙ČÉĘË";
        public static final String LAST_COUNT =          "æàáâãäå";
        public static final String LAST_COUNT_EXTENDED = "ćŕáâăäĺ";

        public static byte[] decodeStringBytes(byte[] string) {
            return decodeString(new String(string,CHARSET));
        }

        public static byte[] decodeString(String string) {
            if(string==null || string.length()==0){
                return null;
            }
            int lastCount= LAST_COUNT.indexOf(string.charAt(string.length()-1));
            if(lastCount<0){
                lastCount= LAST_COUNT_EXTENDED.indexOf(string.charAt(string.length()-1));
            }
            if(lastCount<0){
                return null;
            }
            int bitCount= string.length() * 7  - (lastCount==0?7: 14-lastCount);
            if(bitCount<=0 || bitCount%8!=0){
                return null;
            }
            BitSet bitSet=new BitSet(bitCount);
            char[] charArray = string.toCharArray();
            for (int charPtr = 0, charArrayLength = charArray.length-1; charPtr < charArrayLength; charPtr++) {
                char c = charArray[charPtr];
                int b = ENCODING_CHARACTERS.indexOf(c);
                if (b < 0) {
                    b = ENCODING_CHARACTERS_EXTENDED.indexOf(c);
                }
                if (b < 0) {
                    return null;
                }
                for (int i = 0; i < 7; i++) {
                    if ((b & (1 << i)) != 0) {
                        bitSet.set(charPtr*7 + i);
                    }
                }
                bitCount += 7;
            }
            int wholeBytes=(bitCount + 7) / 8;//rounded up
            byte[] setBytes=bitSet.toByteArray();
            if(setBytes.length==wholeBytes){
                return setBytes;
            }
            byte[] bytes=new byte[wholeBytes];
            System.arraycopy(setBytes,0,bytes,0,setBytes.length);
            return bytes;
        }

        public static String encodeToString(byte[] bytes){
            if(bytes==null){
                return null;
            }
            BitSet bitSet= BitSet.valueOf(bytes);
            int bitCount=bytes.length*8;
            int wholeSevens=(bitCount + 6) / 7;//rounded up
            StringBuilder sb=new StringBuilder();
            for (int i=0,c=0;i<wholeSevens;i++) {
                sb.append(ENCODING_CHARACTERS.charAt(bitSet.get(c,c+=7).toByteArray()[0]));
            }
            sb.append(LAST_COUNT.charAt(bitCount%7));
            return sb.toString();
        }

        public static byte[] encodeToBytes(byte[] bytes){
            return CHARSET.encode(encodeToString(bytes)).array();
        }
    }

    public static class Base10 {
        public static String encodeToString(byte[] bytes){
            return new BigInteger(bytes).toString();
        }

        public static byte[] decodeString(String string) {
            try {
                return new BigInteger(string).toByteArray();
            }catch (Exception e){
                return null;
            }
        }
    }

    public static Bson sort(TreeTableColumn col,String... fieldName){
        return Aggregates.sort(col.getSortType()== TreeTableColumn.SortType.ASCENDING?ascending(fieldName):descending(fieldName));
    }

    public static final String OPTIONS_CHAR="/";
    public static Pattern getPattern(String string){
        if(string.startsWith(OPTIONS_CHAR)){
            int last=string.lastIndexOf(OPTIONS_CHAR);
            if(last>0) {
                String options = string.substring(last + 1).toLowerCase();
                if(options.replaceAll("[a-z]","").length()==0) {
                    string = string.substring(1, last);
                    return Pattern.compile("(?" + options + ")" + string);
                }
            }
        }
        return Pattern.compile(string);
    }

    private static Method columnToFitMethod;

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public static void autoFitTable(TableView tableView) {
        tableView.getItems().addListener((ListChangeListener<Object>) c -> {
            for (Object column : tableView.getColumns()) {
                try {
                    columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static BufferedImage rotateCw(BufferedImage img )
    {
        int         width  = img.getWidth();
        int         height = img.getHeight();
        BufferedImage   newImage = new BufferedImage( height, width, img.getType() );

        for( int i=0 ; i < width ; i++ )
            for( int j=0 ; j < height ; j++ )
                newImage.setRGB( height-1-j, i, img.getRGB(i,j) );

        return newImage;
    }

    public static void setExpandRecursively(TreeItem<?> changeRoot, boolean expand){
        changeRoot.getChildren().forEach(treeItem -> {
            treeItem.setExpanded(expand);
            setExpandRecursively(treeItem,expand);
        });
    }

    public static void setExpandRecursivelyWithRoot(TreeItem<?> changeRoot, boolean expand){
        changeRoot.setExpanded(expand);
        setExpandRecursively(changeRoot,expand);
    }

    public static class ScanResult{
        public BufferedImage image;
        public List<String> code;

        public ScanResult(BufferedImage image, List<String> code) {
            this.image = image;
            this.code = code;
        }
    }

    public static ScanResult readQRCode(BufferedImage input) {
        GrayU8 gray = ConvertBufferedImage.convertFrom(input,(GrayU8)null);

        QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null,GrayU8.class);

        detector.process(gray);

        // Get's a list of all the qr codes it could successfully detect and decode
        List<QrCode> detections = detector.getDetections();

        Graphics2D g2 = input.createGraphics();
        int strokeWidth = Math.max(4,input.getWidth()/200); // in large images the line can be too thin
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.setColor(Color.GREEN);
        List<String> codes=new ArrayList<>();
        for( QrCode qr : detections ) {
            codes.add(qr.message);
            g2.drawPolygon(getPolygon(qr.bounds));
        }

        // List of objects it thinks might be a QR Code but failed for various reasons
        List<QrCode> failures = detector.getFailures();
        g2.setColor(Color.RED);
        for( QrCode qr : failures ) {
            // If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
            if( qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal() ){
                g2.setColor(Color.YELLOW);
            }else{
                g2.setColor(Color.RED);
            }
            g2.drawPolygon(getPolygon(qr.bounds));

        }
        g2.dispose();
        return new ScanResult(input,codes);
    }

    public static Polygon getPolygon(Polygon2D_F64 polygon2D_f64){
        FastQueue<Point2D_F64> vertexes = polygon2D_f64.vertexes;
        int[] x=new int[vertexes.size];
        int[] y=new int[vertexes.size];
        Point2D_F64[] data = vertexes.data;
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            Point2D_F64 point2D_f64 = data[i];
            x[i]=(int) point2D_f64.x;
            y[i]=(int) point2D_f64.y;
        }
        return new Polygon(x,y,vertexes.size);
    }

    public final static String unescape_perl_string(String oldstr) {

        /*
         * In contrast to fixing Java's broken regex charclasses,
         * this one need be no bigger, as unescaping shrinks the string
         * here, where in the other one, it grows it.
         */

        StringBuffer newstr = new StringBuffer(oldstr.length());

        boolean saw_backslash = false;

        for (int i = 0; i < oldstr.length(); i++) {
            int cp = oldstr.codePointAt(i);
            if (oldstr.codePointAt(i) > Character.MAX_VALUE) {
                i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
            }

            if (!saw_backslash) {
                if (cp == '\\') {
                    saw_backslash = true;
                } else {
                    newstr.append(Character.toChars(cp));
                }
                continue; /* switch */
            }

            if (cp == '\\') {
                saw_backslash = false;
                newstr.append('\\');
                newstr.append('\\');
                continue; /* switch */
            }

            switch (cp) {

                case 'r':  newstr.append('\r');
                    break; /* switch */

                case 'n':  newstr.append('\n');
                    break; /* switch */

                case 'f':  newstr.append('\f');
                    break; /* switch */

                /* PASS a \b THROUGH!! */
                case 'b':  newstr.append("\\b");
                    break; /* switch */

                case 't':  newstr.append('\t');
                    break; /* switch */

                case 'a':  newstr.append('\007');
                    break; /* switch */

                case 'e':  newstr.append('\033');
                    break; /* switch */

                /*
                 * A "control" character is what you get when you xor its
                 * codepoint with '@'==64.  This only makes sense for ASCII,
                 * and may not yield a "control" character after all.
                 *
                 * Strange but true: "\c{" is ";", "\c}" is "=", etc.
                 */
                case 'c':   {
                    if (++i == oldstr.length()) { die("trailing \\c"); }
                    cp = oldstr.codePointAt(i);
                    /*
                     * don't need to grok surrogates, as next line blows them up
                     */
                    if (cp > 0x7f) { die("expected ASCII after \\c"); }
                    newstr.append(Character.toChars(cp ^ 64));
                    break; /* switch */
                }

                case '8':
                case '9': die("illegal octal digit");
                    /* NOTREACHED */

                    /*
                     * may be 0 to 2 octal digits following this one
                     * so back up one for fallthrough to next case;
                     * unread this digit and fall through to next case.
                     */
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': --i;
                    /* FALLTHROUGH */

                    /*
                     * Can have 0, 1, or 2 octal digits following a 0
                     * this permits larger values than octal 377, up to
                     * octal 777.
                     */
                case '0': {
                    if (i+1 == oldstr.length()) {
                        /* found \0 at end of string */
                        newstr.append(Character.toChars(0));
                        break; /* switch */
                    }
                    i++;
                    int digits = 0;
                    int j;
                    for (j = 0; j <= 2; j++) {
                        if (i+j == oldstr.length()) {
                            break; /* for */
                        }
                        /* safe because will unread surrogate */
                        int ch = oldstr.charAt(i+j);
                        if (ch < '0' || ch > '7') {
                            break; /* for */
                        }
                        digits++;
                    }
                    if (digits == 0) {
                        --i;
                        newstr.append('\0');
                        break; /* switch */
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(
                                oldstr.substring(i, i+digits), 8);
                    } catch (NumberFormatException nfe) {
                        die("invalid octal value for \\0 escape");
                    }
                    newstr.append(Character.toChars(value));
                    i += digits-1;
                    break; /* switch */
                } /* end case '0' */

                case 'x':  {
                    if (i+2 > oldstr.length()) {
                        die("string too short for \\x escape");
                    }
                    i++;
                    boolean saw_brace = false;
                    if (oldstr.charAt(i) == '{') {
                        /* ^^^^^^ ok to ignore surrogates here */
                        i++;
                        saw_brace = true;
                    }
                    int j;
                    for (j = 0; j < 8; j++) {

                        if (!saw_brace && j == 2) {
                            break;  /* for */
                        }

                        /*
                         * ASCII test also catches surrogates
                         */
                        int ch = oldstr.charAt(i+j);
                        if (ch > 127) {
                            die("illegal non-ASCII hex digit in \\x escape");
                        }

                        if (saw_brace && ch == '}') { break; /* for */ }

                        if (! ( (ch >= '0' && ch <= '9')
                                ||
                                (ch >= 'a' && ch <= 'f')
                                ||
                                (ch >= 'A' && ch <= 'F')
                        )
                        )
                        {
                            die(String.format(
                                    "illegal hex digit #%d '%c' in \\x", ch, ch));
                        }

                    }
                    if (j == 0) { die("empty braces in \\x{} escape"); }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldstr.substring(i, i+j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\x escape");
                    }
                    newstr.append(Character.toChars(value));
                    if (saw_brace) { j++; }
                    i += j-1;
                    break; /* switch */
                }

                case 'u': {
                    if (i+4 > oldstr.length()) {
                        die("string too short for \\u escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 4; j++) {
                        /* this also handles the surrogate issue */
                        if (oldstr.charAt(i+j) > 127) {
                            die("illegal non-ASCII hex digit in \\u escape");
                        }
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt( oldstr.substring(i, i+j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\u escape");
                    }
                    newstr.append(Character.toChars(value));
                    i += j-1;
                    break; /* switch */
                }

                case 'U': {
                    if (i+8 > oldstr.length()) {
                        die("string too short for \\U escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 8; j++) {
                        /* this also handles the surrogate issue */
                        if (oldstr.charAt(i+j) > 127) {
                            die("illegal non-ASCII hex digit in \\U escape");
                        }
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldstr.substring(i, i+j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\U escape");
                    }
                    newstr.append(Character.toChars(value));
                    i += j-1;
                    break; /* switch */
                }

                default:   newstr.append('\\');
                    newstr.append(Character.toChars(cp));
                    /*
                     * say(String.format(
                     *       "DEFAULT unrecognized escape %c passed through",
                     *       cp));
                     */
                    break; /* switch */

            }
            saw_backslash = false;
        }

        /* weird to leave one at the end */
        if (saw_backslash) {
            newstr.append('\\');
        }

        return newstr.toString();
    }

    /*
     * Return a string "U+XX.XXX.XXXX" etc, where each XX set is the
     * xdigits of the logical Unicode code point. No bloody brain-damaged
     * UTF-16 surrogate crap, just true logical characters.
     */
    public final static String uniplus(String s) {
        if (s.length() == 0) {
            return "";
        }
        /* This is just the minimum; sb will grow as needed. */
        StringBuffer sb = new StringBuffer(2 + 3 * s.length());
        sb.append("U+");
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("%X", s.codePointAt(i)));
            if (s.codePointAt(i) > Character.MAX_VALUE) {
                i++; /****WE HATES UTF-16! WE HATES IT FOREVERSES!!!****/
            }
            if (i+1 < s.length()) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private static final void die(String foa) {
        throw new IllegalArgumentException(foa);
    }

    private static final void say(String what) {
        System.out.println(what);
    }
}
