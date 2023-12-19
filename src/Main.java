import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedImage img = Predict.loadImage(Paths.get("src/img_2.png"));
        Predict.ImageArr3D quantized = Predict.getDifference(img);
        Predict.compressToFile(quantized);


        Predict.ImageArr3D _QuantizedFromFile = Predict.loadQuantizedDiff("src/compressed.bin");
        BufferedImage predicted = Predict.decompressImage(_QuantizedFromFile);
        Predict.saveImage(Paths.get("src/decode.bmp"), predicted, "bmp");
    }
}


