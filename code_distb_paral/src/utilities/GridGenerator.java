package utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GridGenerator {

    
    
    public static void main(String [] args) throws IOException {
        String file = args[0];
        
        File imageFile = new File(file);
        BufferedImage image = ImageIO.read(imageFile);
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
        
        writer.write(image.getHeight()+","+image.getWidth()+"\n");
        
        for(int y = 0; y < image.getHeight(); y++) {
            for(int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                
                if(color.getBlue() < 240 && color.getRed() < 240 && color.getGreen() < 240) {
                    writer.write('1'); //Obstacle
                }else if(color.getRed() > 240 && color.getBlue() < 160 && color.getGreen() < 160) {
                    writer.write('3'); //Source
                }else if(color.getBlue() > 240 && color.getRed() < 160 && color.getGreen() < 160) {
                    writer.write('2'); //Water
                }else {
                    writer.write('0'); //Air
                }
                
                if(x + 1 != image.getWidth()) {
                    writer.write(',');
                }
            }

            writer.write("\n");
        }
        
        writer.close();
    }
    
}
