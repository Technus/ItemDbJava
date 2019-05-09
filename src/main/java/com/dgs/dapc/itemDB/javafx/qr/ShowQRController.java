package com.dgs.dapc.itemDB.javafx.qr;

import com.dgs.dapc.itemDB.PrintQR;
import com.dgs.dapc.itemDB.headless.db.DiscriminatedObjectId;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.*;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

public class ShowQRController {
    public ImageView qrImage;
    public Button printButton;
    public TextField discriminatorText;
    public TextField objectIdText;
    public TextField qrValueText;

    public void setQrImage(DiscriminatedObjectId objectId){
        qrValueText.setText(objectId.toString());
        discriminatorText.setText(""+objectId.discriminator);
        objectIdText.setText(objectId.id.toHexString());

        BufferedImage bufferedImage= PrintQR.get(objectId);
        if(bufferedImage!=null){
            Image image= SwingFXUtils.toFXImage(bufferedImage,null);

            int width = (int)image.getWidth();
            int height = (int)image.getHeight();

            int zoom = 10; // 2, 4, 8, 16 (I only tested for powers of two)
            IntBuffer src = IntBuffer.allocate(width * height);
            WritablePixelFormat<IntBuffer> pf = PixelFormat.getIntArgbInstance();
            image.getPixelReader().getPixels(0, 0, width, height, pf, src, width);
            int newWidth = width * zoom;
            int newHeight = height * zoom;
            int[] dst = new int[newWidth * newHeight];
            int index = 0;
            for (int y = 0; y < height; y++) {
                index = y * newWidth * zoom;
                for (int x = 0; x < width; x++) {
                    int pixel = src.get();
                    for (int i = 0; i < zoom; i++) {
                        for (int j = 0; j < zoom; j++) {
                            dst[index + i + (newWidth * j)] = pixel;
                        }
                    }
                    index += zoom;
                }
            }
            WritableImage bigImage = new WritableImage(newWidth, newHeight);
            bigImage.getPixelWriter().setPixels(0, 0, newWidth, newHeight, pf, dst, 0, newWidth);
            qrImage.setImage(bigImage);
            qrImage.setFitWidth(newWidth);
            qrImage.setFitHeight(newHeight);

            printButton.setOnAction(event -> PrintQR.print(objectId));
            printButton.setDisable(false);
        }
    }
}
