package de.tomjanke.medt;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

public class NeuralFlappy extends ApplicationAdapter {

    private static final float TICK = 1 / 60f;
    private static final int BIRD_COUNT = 20;
    private static final int HALF_PIPE_HEIGHT = 60;
    private static final String[] HELP = new String[]{
            "CTRL + A", "Load Network",
            "CTRL + S", "Save Network",
            "CTRL + R", "Reset",
            "B", "Reset Speed",
            "N", "Decrease speed",
            "M", "Increase speed",
            "SPACE", "Pause"
    };

    private Viewport viewport;

    private SpriteBatch batch;
    private ShapeRenderer shape;

    private Random random;

    private Texture backgroundTexture,
            groundTexture,
            pipeTexture;

    private Texture[] birdTexture,
            birdTextureSelected;

    private BitmapFont font;
    private GlyphLayout glyph;

    private Rectangle birdRect, pipeRectU, pipeRectL;

    private float birdTextureTime,
            countdownTime,
            pipeX,
            groundX,
            backgroundX;

    private int iterationsPerTick = 1;

    private float[] heights,
            velocity,
            birds;

    private NeuralNet[] networks;

    private boolean[] dead;

    private boolean scoreLock;
    private int score = 0,
            generation = 0,
            bestScore = 0,
            bestScoreGen = 0;

    private boolean drawHelp = false,
            drawDebug = false,
            paused = true;

    {
        birds = new float[BIRD_COUNT];
        velocity = new float[BIRD_COUNT];
        networks = new NeuralNet[BIRD_COUNT];
        heights = new float[4];
        dead = new boolean[BIRD_COUNT];
        for (int i = 0; i < BIRD_COUNT; i++)
            networks[i] = new NeuralNet();
    }

    @Override
    public void create() {
        random = new Random();

        viewport = new FitViewport(512 + 320, 512);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        backgroundTexture = new Texture("background.png");
        groundTexture = new Texture("ground.png");
        birdTexture = new Texture[]{
                new Texture("bird1.png"),
                new Texture("bird2.png"),
                new Texture("bird3.png")
        };
        birdTextureSelected = new Texture[]{
                new Texture("bird-sel1.png"),
                new Texture("bird-sel2.png"),
                new Texture("bird-sel3.png")
        };
        pipeTexture = new Texture("pipe.png");

        batch = new SpriteBatch();
        shape = new ShapeRenderer();

        birdRect = new Rectangle();
        pipeRectU = new Rectangle();
        pipeRectL = new Rectangle();

        font = new BitmapFont(Gdx.files.internal("font.fnt"));
        font.getData().setScale(.5f);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        glyph = new GlyphLayout();

        viewport.apply();

        reset();
    }

    private void reset() {
        birdTextureTime = 0;
        pipeX = 200;
        groundX = 0;
        backgroundX = 0;
        score = 0;
        scoreLock = false;
        countdownTime = iterationsPerTick != 1 ? .1f : 1f;
        generation++;
        for (int i = 0; i < birds.length; i++) {
            birds[i] = 256;
            velocity[i] = 2f;
            dead[i] = false;
        }
        for (int i = 0; i < heights.length; i++)
            heights[i] = 200 + random.nextFloat() * 200;
    }

    private void breed() {
        ArrayList<NeuralNet> bestNetworks = new ArrayList<>();
        NeuralNet best = null;
        for (NeuralNet net : networks)
            if (best == null || best.getFitness() < net.getFitness()) best = net;

        if (best == null) {
            JOptionPane.showMessageDialog(null, "Well, there was no best bird found. So we'll just use the first one.", "Warning", JOptionPane.WARNING_MESSAGE);
            best = networks[0];
        }

        int threshold = (int) (best.getFitness() * 0.8);

        for (NeuralNet net : networks)
            if (net.getFitness() >= threshold && !best.equals(net)) bestNetworks.add(net);

        for (NeuralNet net : bestNetworks)
            best.breed(net);

        for (int i = 0; i < networks.length; i++)
            networks[i] = i == 0 ? best : new NeuralNet(best, .3f);

        for (NeuralNet net : networks)
            net.resetFitness();

        bestNetworks.clear();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (countdownTime > 0 && !paused) countdownTime -= TICK;

        int alive = 0;
        float currentHeight;

        boolean play = countdownTime <= 0;

        if (play && !paused) for (int iter = 0; iter < iterationsPerTick; iter++) {
            alive = 0;

            currentHeight = heights[0];
            if (pipeX < 50) {
                currentHeight = heights[1];
                if (!scoreLock) {
                    score++;
                    if (bestScore < score) {
                        bestScore = score;
                        bestScoreGen = generation;
                    }
                    scoreLock = true;
                }
            }

            groundX -= TICK * 80;
            if (groundX < -336) groundX += 336;
            backgroundX -= TICK * 40;
            if (backgroundX < -288) backgroundX += 288;

            pipeX -= TICK * 80;
            float pipePosition = pipeX;
            if (pipeX < 100) pipePosition += 200;
            pipePosition -= 100;

            for (int i = 0; i < birds.length; i++)
                if (!dead[i]) {
                    velocity[i] -= TICK * 10f;
                    if (networks[i].activate(new float[]{birds[i], currentHeight, pipePosition, velocity[i]}) > 0)
                        if (birds[i] <= 420 && velocity[i] < 1.2f)
                            velocity[i] = 4f;
                    birds[i] += velocity[i];
                }


            pipeRectU.set(pipeX, heights[0] + HALF_PIPE_HEIGHT, 52, 320);
            pipeRectL.set(pipeX, heights[0] - HALF_PIPE_HEIGHT - 320, 52, 320);
            for (int i = 0; i < birds.length; i++) {
                birdRect.set(100, birds[i] - 12, 34, 24);
                if (birdRect.overlaps(pipeRectL) || birdRect.overlaps(pipeRectU)) dead[i] = true;
                else if (birds[i] < 100) dead[i] = true;
                if (!dead[i]) networks[i].addFitness();
            }

            boolean allDead = true;
            for (boolean b : dead)
                if (!b) {
                    allDead = false;
                    alive++;
                }

            if (allDead) {
                breed();
                reset();
                break;
            }

            if (pipeX <= -54) {
                pipeX += 200;
                for (int i = 1; i < heights.length; i++)
                    heights[i - 1] = heights[i];
                heights[heights.length - 1] = 200 + random.nextFloat() * 200;
                scoreLock = false;
            }
        }

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        for (float x = backgroundX; x < 512; x += 288)
            batch.draw(backgroundTexture, x, 0, 288, 512);

        for (int i = 0; i < heights.length; i++) {
            batch.draw(pipeTexture, pipeX + 200 * i, heights[i] + HALF_PIPE_HEIGHT, 52, 320, 0, 0, 52, 320, false, true);
            batch.draw(pipeTexture, pipeX + 200 * i, heights[i] - HALF_PIPE_HEIGHT - 320, 52, 320);
        }

        for (float x = groundX; x < 512; x += 336)
            batch.draw(groundTexture, x, 0, 336, 112);

        birdTextureTime += 0.2;

        int birdTextureTimeI = (int) birdTextureTime;
        int textureIndex = 0;
        if (birdTextureTimeI == 1 || birdTextureTimeI == 3) textureIndex = 1;
        if (birdTextureTimeI == 2) textureIndex = 2;
        if (birdTextureTime >= 4) birdTextureTime -= 4;

        int first = -1;
        for (int i = 0; i < birds.length; i++)
            if (!dead[i])
                if (first == -1) first = i;
                else batch.draw(birdTexture[textureIndex], 100, birds[i] - 12);


        batch.draw(birdTextureSelected[textureIndex], 100, birds[first] - 12);

        batch.end();

        shape.setProjectionMatrix(viewport.getCamera().combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        shape.setColor(Color.DARK_GRAY);
        shape.rect(512, 0, 320, 512);

        NeuralNet net = null;
        for (int i = 0; i < dead.length; i++)
            if (!dead[i]) {
                net = networks[i];
                break;
            }

        if (net != null) {
            float[] input = net.getInputLayer(),
                    hidden = net.getHiddenLayer(),
                    weight1 = net.getWeightLayer1(),
                    weight2 = net.getWeightLayer2();
            float output = net.getOutputLayer();

            float _w = viewport.getWorldWidth(),
                    _h = viewport.getWorldHeight();

            float maxWeight = 0;
            for (float f : weight1)
                if (Math.abs(f) > maxWeight) maxWeight = Math.abs(f);
            for (float f : weight2)
                if (Math.abs(f) > maxWeight) maxWeight = Math.abs(f);

            for (int i = 0; i < input.length; i++)
                for (int h = 0; h < hidden.length; h++) {
                    color(shape, weight1[h * input.length + i]);
                    shape.rectLine(_w - 260, _h - (i + 1) * 60, _w - 160, _h - (h + 1) * 60, size(weight1[h * input.length + i], maxWeight, 1, 6));
                }

            for (int h = 0; h < hidden.length; h++) {
                color(shape, weight2[h]);
                shape.rectLine(_w - 160, _h - (h + 1) * 60, _w - 60, _h - 60, size(weight2[h], maxWeight, 1, 6));
            }

            for (int i = 0; i < input.length; i++) {
                shape.setColor(Color.WHITE);
                shape.circle(_w - 260, _h - (i + 1) * 60, 20);
                color(shape, input[i]);
                shape.circle(_w - 260, _h - (i + 1) * 60, size(input[i], 512, 6, 20));
            }

            for (int i = 0; i < hidden.length; i++) {
                shape.setColor(Color.WHITE);
                shape.circle(_w - 160, _h - (i + 1) * 60, 20);
                color(shape, hidden[i]);
                shape.circle(_w - 160, _h - (i + 1) * 60, size(hidden[i], 512, 6, 20));
            }

            shape.setColor(Color.WHITE);
            shape.circle(_w - 60, _h - 60, 20);
            color(shape, output);
            shape.circle(_w - 60, _h - 60, size(output, 512, 6, 20));
        }

        shape.end();

        batch.begin();

        int pad = 4;

        String txt = String.valueOf(score) + " P";
        glyph.setText(font, txt);
        font.draw(batch, glyph, 256 - glyph.width / 2, 45 - pad);

        txt = String.valueOf(bestScore) + " P";
        glyph.setText(font, txt);
        font.draw(batch, glyph, 384 - glyph.width / 2, 45 - pad);

        txt = String.valueOf(alive) + (alive == 1 ? " Bird" : " Birds");
        glyph.setText(font, txt);
        font.draw(batch, glyph, 128 - glyph.width / 2, 45 - pad);

        txt = String.valueOf(generation) + ". Gen";
        glyph.setText(font, txt);
        font.draw(batch, glyph, 256 - glyph.width / 2, 45 + glyph.height + pad);

        txt = String.valueOf(bestScoreGen) + ". Gen";
        glyph.setText(font, txt);
        font.draw(batch, glyph, 384 - glyph.width / 2, 45 + glyph.height + pad);

        txt = String.valueOf(first + 1) + ". Bird";
        glyph.setText(font, txt);
        font.draw(batch, glyph, 512 + 160 - glyph.width / 2, 45 - pad);

        if (paused) {
            txt = "– PAUSED –";
            glyph.setText(font, txt);
            font.draw(batch, glyph, 256 - glyph.width / 2, 256 + glyph.height + pad);

            txt = "SPACE to resume";
            glyph.setText(font, txt);
            font.draw(batch, glyph, 256 - glyph.width / 2, 256 - pad);
        }

        if (!drawHelp) {
            txt = "F1 / H – Help";
            glyph.setText(font, txt);
            font.draw(batch, glyph, 512 + 160 - glyph.width / 2, 45 + glyph.height + pad);
        } else {
            for (int i = 0; i < HELP.length / 2; i++) {
                txt = HELP[i * 2];
                glyph.setText(font, txt);
                font.draw(batch, glyph, 512 + pad, 45 + (glyph.height + pad) * (i + 1));
                font.draw(batch, HELP[i * 2 + 1], 620 + pad, 45 + (glyph.height + pad) * (i + 1));
            }
        }

        if (iterationsPerTick != 1) {
            txt = String.valueOf(iterationsPerTick) + "x";
            glyph.setText(font, txt);
            font.draw(batch, glyph, 128 - glyph.width / 2, 45 + glyph.height + pad);
        }

        if (drawDebug) {
            font.draw(batch, Gdx.graphics.getFramesPerSecond() + " fps", 10, 502);
        }

        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) iterationsPerTick = 1;

        if (Gdx.input.isKeyJustPressed(Input.Keys.M) && iterationsPerTick < 32) iterationsPerTick *= 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) && iterationsPerTick > 1) iterationsPerTick /= 2;

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.S))
            networks[first].save(generation);
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            NeuralNet network = new NeuralNet();
            int loaded = network.load();
            if (loaded != -1) {
                for (int i = 0; i < networks.length; i++)
                    if (i == 0) networks[i] = network;
                    else networks[i] = new NeuralNet(network, .3f);
                bestScoreGen = 0;
                bestScore = 0;
                generation = loaded - 1;
                reset();
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (int i = 0; i < networks.length; i++) networks[i] = new NeuralNet();
            bestScoreGen = 0;
            bestScore = 0;
            generation = 0;
            reset();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H) || Gdx.input.isKeyJustPressed(Input.Keys.F1)) drawHelp = !drawHelp;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) drawDebug = !drawDebug;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) paused = !paused;
    }

    private void color(ShapeRenderer shape, float val) {
        shape.setColor(val > 0 ? Color.GREEN : (val < 0 ? Color.FIREBRICK : Color.WHITE));
    }

    private float size(float val, float maxVal, float minSize, float maxSize) {
        if (Math.abs(val) > maxVal) return maxSize;
        float perc = Math.abs(val) / maxVal;
        return minSize + (maxSize - minSize) * perc;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewport.update(width, height, true);
        Gdx.graphics.requestRendering();
    }

    @Override
    public void dispose() {
        font.dispose();
        backgroundTexture.dispose();
        for (Texture t : birdTexture) t.dispose();
        for (Texture t : birdTextureSelected) t.dispose();
        pipeTexture.dispose();
        batch.dispose();
        shape.dispose();
    }
}
