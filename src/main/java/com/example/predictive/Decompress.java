package com.example.predictive;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class Decompress {
    private static int QSTEP;

    static class ImageArr3D {
        public int[][][] arr;
        int width;
        int height;
        int imgType;

        ImageArr3D(int width, int height, int imgType) {
            this.arr = new int[width][height][4];
            this.height = height;
            this.width = width;
            this.imgType = imgType;
        }
    }

    private static int dequantize(int value){
        return value * QSTEP + (QSTEP / 2) - 255;
    }

    public static void saveImage(Path imgPath, BufferedImage img, String formatName) {
        try {
            ImageIO.write(img, formatName, imgPath.toFile());
            System.out.println("Decompressed image saved successfully.");
        } catch (IOException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }

    public static ImageArr3D loadQuantizedDiff(String filePath) {
        ImageArr3D quantizedDiff = null;
        try (DataInputStream file = new DataInputStream(new FileInputStream(filePath))) {
            QSTEP = file.readInt();
            int width = file.readInt();
            int height = file.readInt();
            int nColors = file.readInt();

            quantizedDiff = new ImageArr3D(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int i = 0; i < nColors; i++) {
                        quantizedDiff.arr[x][y][i] = file.readByte();
                    }
                }
            }
            System.out.println("Compressed Image loaded from: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return quantizedDiff;
    }

    public static BufferedImage decompressImage(ImageArr3D quantizedDiff) {
        BufferedImage predicted = new BufferedImage(quantizedDiff.width, quantizedDiff.height, quantizedDiff.imgType);
        predicted.setRGB(0, 0, quantizedDiff.arr[0][0][3]);
        int width = quantizedDiff.width;
        int height = quantizedDiff.height;
        int max = Integer.max(width, height);
        //Copying the first Row and first Column into the predicted image
        for (int i = 1; i < max; ++i) {
            if (i < width) {
                predicted.setRGB(i, 0, quantizedDiff.arr[i][0][3]);
            }

            if (i < height) {
                predicted.setRGB(0, i, quantizedDiff.arr[0][i][3]);
            }
        }

        for (int x = 1; x < width; ++x) {
            for (int y = 1; y < height; ++y) {
                int diagPixel = predicted.getRGB(x - 1, y - 1);
                int beforePixel = predicted.getRGB(x - 1, y);
                int abovePixel = predicted.getRGB(x, y - 1);
                int _Max = 0;
                int _Min = 0;
                if (beforePixel >= abovePixel) {
                    _Max = beforePixel;
                    _Min = abovePixel;
                } else {
                    _Min = beforePixel;
                    _Max = abovePixel;
                }

                int[] dequantizedDiff = new int[3];
                for (int i = 0; i < 3; i++) {
                    // Custom table
//                    dequantizedDiff[i] = dequantize(quantizedDiff.arr[x][y][i]);
                    // Another Way
                    dequantizedDiff[i] = quantizedDiff.arr[x][y][i] * QSTEP;
                }

                if (diagPixel <= _Min) {
                    int reconstructedPixel = getDecodedPixel(_Max, dequantizedDiff);
                    predicted.setRGB(x, y, reconstructedPixel);
                } else if (diagPixel >= _Max) {
                    int reconstructedPixel = getDecodedPixel(_Min, dequantizedDiff);
                    predicted.setRGB(x, y, reconstructedPixel);
                } else {
                    int reconstructedPixel = getDecodedPixel((beforePixel + abovePixel - diagPixel), dequantizedDiff);
                    predicted.setRGB(x, y, reconstructedPixel);
                }
            }
        }
        return predicted;
    }

    private static int getDecodedPixel(int predicted, int[] dequantizedDiff) {
        int[] colors = extractColors(predicted);

        int red = Math.min(Math.max(colors[0] + dequantizedDiff[0], 0), 255);
        int green = Math.min(Math.max(colors[1] + dequantizedDiff[1], 0), 255);
        int blue = Math.min(Math.max(colors[2] + dequantizedDiff[2], 0), 255);

        return ((red << 16) | (green << 8) | blue);
    }

    public static int[] extractColors(int pixel) {
        int[] colors = new int[3];
        colors[0] = (pixel >> 16) & 0xFF;
        colors[1] = (pixel >> 8) & 0xFF;
        colors[2] = pixel & 0xFF;
        return colors;
    }
}
