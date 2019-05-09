package com.dgs.dapc.itemDB;

import com.dgs.dapc.itemDB.headless.db.DiscriminatedObjectId;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.beans.property.SimpleStringProperty;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.PrinterResolution;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.util.HashMap;
import java.util.Map;

public class BrotherPrintQR {
    private static final SimpleStringProperty printerName=new SimpleStringProperty("Brother PT-P750W USB");
    //private static final SimpleStringProperty printerName=new SimpleStringProperty("LABEL");
    static {
        printerName.addListener((observable, oldValue, newValue) -> printer=null);
    }
    private static final QRCodeWriter qrCodeWriter=new QRCodeWriter();
    private static PrintService printer;
    private static final Map<EncodeHintType, Object> hints = new HashMap<>();
    static{
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, Utility.Base128.CHARSET.name());
        hints.put(EncodeHintType.MARGIN, 1);
    }

    private BrotherPrintQR(){}

    public static BufferedImage get(DiscriminatedObjectId objectId) {
        try {
            BitMatrix bm = qrCodeWriter.encode(objectId.toString(), BarcodeFormat.QR_CODE, 1, 1, hints);
            BufferedImage image=MatrixToImageWriter.toBufferedImage(bm);
            //Image imageL = image.getScaledInstance(image.getWidth() * 2, image.getHeight() * 2, Image.SCALE_FAST);
            BufferedImage target=new BufferedImage(image.getWidth(null)*5/3,image.getHeight(null),image.getType());
            Graphics2D g=(Graphics2D) target.getGraphics();
            g.setFont(new Font("Consolas", Font.BOLD, 26));
            g.setColor(Color.WHITE);
            g.fillRect(0,0,target.getWidth(),target.getHeight());
            g.drawImage(image,2,0,image.getWidth(null),image.getWidth(null),null);
            g.setColor(Color.BLACK);
            g.drawString(""+objectId.discriminator,
                    image.getWidth(null)+((target.getWidth()-image.getWidth(null))-g.getFontMetrics().stringWidth(""+objectId.discriminator))/2,
                    (target.getHeight()-g.getFontMetrics().getHeight()+g.getFontMetrics().getDescent())/2+g.getFontMetrics().getAscent());
            g.dispose();
            return Utility.rotateCw(target);
        }catch (WriterException e){
            throw new RuntimeException("Failed encoding QR Code!",e);
        }
    }

    public static void print(DiscriminatedObjectId objectId) {
        try {
            PrinterJob printJob = PrinterJob.getPrinterJob();
            if (printer == null) {
                for (PrintService printer : PrinterJob.lookupPrintServices()) {
                    if (printer.getName().contains(printerName.get())) {
                        System.out.println("Printer: " + printer.getName());
                        printJob.setPrintService(BrotherPrintQR.printer = printer);
                    }
                }
            } else {
                printJob.setPrintService(printer);
            }

            PageFormat pageFormat = printJob.defaultPage();
            Paper paper = pageFormat.getPaper();
            paper.setSize(mmToInch72nd(25), mmToInch72nd(17));
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pageFormat.setOrientation(PageFormat.PORTRAIT);
            pageFormat.setPaper(paper);
            pageFormat = printJob.validatePage(pageFormat);
            BufferedImage image = get(objectId);
            printJob.setPrintable((graphics, pf, pageIndex) -> {
                if (pageIndex != 0) {
                    return Printable.NO_SUCH_PAGE;
                }
                graphics.drawImage(image, 0, 0, image.getWidth(null), image.getHeight(null), null);
                return Printable.PAGE_EXISTS;
            }, pageFormat);
            printJob.setJobName("LABEL PRINT");
            PrintRequestAttributeSet attributesSet = new HashPrintRequestAttributeSet(new PrintRequestAttribute[]{
                    new PrinterResolution(180, 180, PrinterResolution.DPI),
                    new MediaPrintableArea(
                            0,
                            0,
                            25,
                            17,
                            MediaPrintableArea.MM
                    )
            });
            printJob.print(attributesSet);
        } catch (PrinterException e) {
            e.printStackTrace();
            printer = null;
        }
    }

    public static void main(String[] args) throws Exception {
        printString("æàáâãäå");
        //String s="stuvwxyz{|}~ÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßðñòóôõö÷øùúûüýþÿÈÉÊË";
        //for (int i = 0; i < s.length(); i+=16) {
        //    printString(s.substring(i,i+16));
        //}
    }

    private static void printString(String args) throws Exception {
        QRCodeWriter codeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, Utility.Base128.CHARSET.name());
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bm = codeWriter.encode(args, BarcodeFormat.QR_CODE, 1, 1, hints);
        System.out.println(bm.toString());
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bm);
        PrinterJob printJob = PrinterJob.getPrinterJob();
        final Image temp=image.getScaledInstance(image.getWidth(),image.getHeight(),Image.SCALE_FAST);
        final BufferedImage imageL=new BufferedImage(temp.getWidth(null)+8,temp.getHeight(null)+6,image.getType());
        imageL.getGraphics().drawImage(temp,8,6,temp.getWidth(null), temp.getHeight(null),null);


        for (PrintService printer : PrinterJob.lookupPrintServices()) {
            if (printer.getName().contains(printerName.get())) {
                System.out.println("Printer: " + printer.getName());
                printJob.setPrintService(printer);

                PageFormat pageFormat = printJob.defaultPage();
                Paper paper = pageFormat.getPaper();
                paper.setSize(mmToInch72nd(12), mmToInch72nd(25));
                paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
                pageFormat.setOrientation(PageFormat.PORTRAIT);
                pageFormat.setPaper(paper);
                pageFormat = printJob.validatePage(pageFormat);
                printJob.setPrintable((graphics, pf, pageIndex) -> {
                    if (pageIndex != 0) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    graphics.drawImage(imageL, 0, 0, imageL.getWidth(null), imageL.getHeight(null), null);
                    return Printable.PAGE_EXISTS;
                }, pageFormat);
                PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet(new PrintRequestAttribute[]{
                        new PrinterResolution(180, 180, PrinterResolution.DPI),
                        new MediaPrintableArea(
                                0,
                                0,
                                25,
                                17,
                                MediaPrintableArea.MM
                        )
                });
                printJob.setJobName("LABEL PRINT");
                //printJob.printDialog();
                printJob.print(aset);
            }
        }
        System.out.println("WAT");
    }

    private static double mmToInch72nd(double mm) {
        return mm / 25.4 * 72;
    }
}
