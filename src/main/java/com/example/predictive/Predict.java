package com.example.predictive;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;

public class Predict {
//    private static final int STEP = 4;
    private static final int QSTEP = 8;
    private static final int NRANGES = 64;


    public static BufferedImage loadImage(Path imgPath) throws IOException {
        // Load the image
        return ImageIO.read(imgPath.toFile());
    }

    public static void saveImage(Path imgPath, BufferedImage img, String formatName) {
        try {
            ImageIO.write(img, formatName, imgPath.toFile());
            System.out.println("Compressed image saved successfully.");
        } catch (IOException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }

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

        public void setPixel(int x, int y, int pixel) {
            // Total
            arr[x][y][3] = pixel;
            // Red
            arr[x][y][0] = (pixel >> 16) & 0xFF;
            // Green
            arr[x][y][1] = (pixel >> 8) & 0xFF;
            // Blue
            arr[x][y][2] = pixel & 0xFF;
        }
    }

    // Get the Difference Between the Original Image and the Predicted One
    public static ImageArr3D getDifference(BufferedImage img) {
        BufferedImage predicted = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        int width = img.getWidth();
        int height = img.getHeight();
        int max = Integer.max(width, height);

        ImageArr3D difference = new ImageArr3D(width, height, img.getType());
        ImageArr3D quantized = new ImageArr3D(width, height, img.getType());
        // Total Diff (Pixel Real Value 24-bit)
        difference.setPixel(0, 0, img.getRGB(0, 0));
        predicted.setRGB(0, 0, img.getRGB(0, 0));
        //Copying the first Row and first Column into the predicted image
        for (int i = 1; i < max; ++i) {
            if (i < width) {
                predicted.setRGB(i, 0, img.getRGB(i, 0));
                difference.setPixel(i, 0, img.getRGB(i, 0));
            }

            if (i < height) {
                predicted.setRGB(0, i, img.getRGB(0, i));
                difference.setPixel(0, i, img.getRGB(0, i));
            }
        }
        quantized.arr = Arrays.copyOf(difference.arr, difference.arr.length);

        for (int x = 1; x < width; ++x) {
            for (int y = 1; y < height; ++y) {
                int diagPixel = img.getRGB(x - 1, y - 1);
                int beforePixel = img.getRGB(x - 1, y);
                int abovePixel = img.getRGB(x, y - 1);
                int _Max = 0;
                int _Min = 0;
                if (beforePixel >= abovePixel) {
                    _Max = beforePixel;
                    _Min = abovePixel;
                } else {
                    _Min = beforePixel;
                    _Max = abovePixel;
                }

                int[] imgColors = extractColors(img.getRGB(x, y));
                int[] predictedColors = null;
                if (diagPixel <= _Min) {
                    predicted.setRGB(x, y, _Max);
                    predictedColors = extractColors(_Max);
                } else if (diagPixel >= _Max) {
                    predicted.setRGB(x, y, _Min);
                    predictedColors = extractColors(_Min);
                } else {
                    predicted.setRGB(x, y, beforePixel + abovePixel - diagPixel);
                    predictedColors = extractColors((beforePixel + abovePixel - diagPixel));
                }
                assignDifference(difference, x, y, imgColors, predictedColors);
                for (int i = 0; i < 3; i++) {
                    // Custom Table
                    quantized.arr[x][y][i] = quantize(difference.arr[x][y][i]);
//                    quantized.arr[x][y][i] = (int) Math.round((double) difference.arr[x][y][i] / STEP) * STEP;

                    // Another equation
//                    quantized.arr[x][y][i] = (int) Math.round(((double) difference.arr[x][y][i] / STEP) + 0.5);
                }
            }
        }
        return quantized;
    }

    public static void assignDifference(ImageArr3D diff, int x, int y, int[] img1Colors, int[] img2Colors) {
        for (int i = 0; i < 3; ++i) {
            diff.arr[x][y][i] = img1Colors[i] - img2Colors[i];
        }
    }

    public static int[] extractColors(int pixel) {
        int[] colors = new int[3];
        colors[0] = (pixel >> 16) & 0xFF;
        colors[1] = (pixel >> 8) & 0xFF;
        colors[2] = pixel & 0xFF;
        return colors;
    }
    public static void compressToFile(ImageArr3D quantizedDiff) {
        try (DataOutputStream file = new DataOutputStream(new FileOutputStream("compressed.bin"))) {
            int width = quantizedDiff.width;
            int height = quantizedDiff.height;
            int nColors = 3;

            file.writeInt(width);
            file.writeInt(height);
            file.writeInt(nColors);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int i = 0; i < nColors; i++) {
                        file.writeByte(quantizedDiff.arr[x][y][i]);
                    }
                }
            }
            System.out.println("Image Compressed to: src/compressed.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static int getDecodedPixel(int predicted, int[] dequantizedDiff) {
        int[] colors = extractColors(predicted);

        int red = Math.min(Math.max(colors[0] + dequantizedDiff[0], 0), 255);
        int green = Math.min(Math.max(colors[1] + dequantizedDiff[1], 0), 255);
        int blue = Math.min(Math.max(colors[2] + dequantizedDiff[2], 0), 255);

        return ((red << 16) | (green << 8) | blue);
    }

    private static int quantize(int value) {
        // Ensure that the value is within the range [-255, 255]
        value = Math.max(-255, Math.min(255, value));

        // Map the value to the corresponding quantization level
        int quantizationLevel = (value + 255) / QSTEP;

        // Ensure that the quantization level is within the range [0, NRANGE - 1]
        quantizationLevel = Math.max(Math.min(NRANGES - 1, quantizationLevel),0);

        // Map the quantization level back to the quantized value
        //int quantizedValue = quantizationLevel * CLASS_RANGE - 255;
        return quantizationLevel;
    }
    private static int dequantize(int value){
        return value * QSTEP + (QSTEP / 2) - 255;
    }

    public static ImageArr3D loadQuantizedDiff(String filePath) {
        ImageArr3D quantizedDiff = null;
        try (DataInputStream file = new DataInputStream(new FileInputStream(filePath))) {
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
            System.out.println("Pixel array data loaded from: " + filePath);
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
                    dequantizedDiff[i] = dequantize(quantizedDiff.arr[x][y][i]);
                    // Another Way
                    //dequantizedDiff[i] = quantizedDiff.arr[x][y][i] * STEP;
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

}

    //    public static int quantizer(int pixelValue){
//        // Calculate the range key
//        return (pixelValue - MINVALUE) / QSTEP;
//    }
//
//    public static int dequantizer(int rangeKey){
//        // range Number => range Value
//        Map<Integer, Integer> quantizerTable = Map.of(
//                0, -4,
//                1 , -1,
//                2, 2,
//                3 , 5
//        );
//        return quantizerTable.get(rangeKey);
//    };
