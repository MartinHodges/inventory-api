package com.requillion.solutions.inventory.service;

import com.requillion.solutions.inventory.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Service
public class ImageService {

    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;
    private static final int THUMBNAIL_SIZE = 200;
    private static final float COMPRESSION_QUALITY = 0.7f;

    public byte[] compressImage(byte[] originalImage, int referenceNumber) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImage));
        if (image == null) {
            throw new IOException("Invalid image data");
        }

        // Resize if necessary
        image = resizeIfNeeded(image, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);

        // Add reference number overlay
        image = addReferenceNumber(image, referenceNumber);

        // Compress
        return compressToJpeg(image, COMPRESSION_QUALITY);
    }

    private BufferedImage addReferenceNumber(BufferedImage image, int referenceNumber) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String text = "#" + referenceNumber;

        // Scale font size based on image dimensions
        int fontSize = Math.max(24, Math.min(image.getWidth(), image.getHeight()) / 20);
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int padding = fontSize / 3;
        int x = padding;
        int y = padding;

        // Draw white background
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, textWidth + padding * 2, textHeight + padding, padding, padding);

        // Draw black text
        g.setColor(Color.BLACK);
        g.drawString(text, x + padding, y + fm.getAscent() + padding / 2);

        g.dispose();
        return image;
    }

    public byte[] createThumbnail(byte[] originalImage) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImage));
        if (image == null) {
            throw new IOException("Invalid image data");
        }

        // Create square thumbnail
        BufferedImage thumbnail = createSquareThumbnail(image, THUMBNAIL_SIZE);

        return compressToJpeg(thumbnail, 0.8f);
    }

    private BufferedImage resizeIfNeeded(BufferedImage image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return image;
        }

        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        LoggerUtil.debug(log, "Resized image from %dx%d to %dx%d", width, height, newWidth, newHeight);
        return resized;
    }

    private BufferedImage createSquareThumbnail(BufferedImage image, int size) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Crop to square from center
        int cropSize = Math.min(width, height);
        int x = (width - cropSize) / 2;
        int y = (height - cropSize) / 2;

        BufferedImage cropped = image.getSubimage(x, y, cropSize, cropSize);

        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, size, size, null);
        g.dispose();

        return thumbnail;
    }

    private byte[] compressToJpeg(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.write(null, new IIOImage(image, null, null), param);

        ios.close();
        writer.dispose();

        return baos.toByteArray();
    }
}
