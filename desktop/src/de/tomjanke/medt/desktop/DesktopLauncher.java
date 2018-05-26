package de.tomjanke.medt.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import de.tomjanke.medt.NeuralFlappy;

public class DesktopLauncher {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 512 + 320;
        config.height = 512;
        config.samples = 4;
        config.title = "A Neural Network plays Flappy Bird";
        config.foregroundFPS = 60;
        config.backgroundFPS = 60;
        new LwjglApplication(new NeuralFlappy(), config);
    }
}
