package de.tomjanke.medt;

import com.badlogic.gdx.Gdx;

import javax.swing.*;
import java.io.*;
import java.util.Random;

/**
 * This class represents a neural network
 */
class NeuralNet {

    /**
     * <tt>Input Layer</tt>
     */
    private float[] inputLayer;

    /**
     * <tt>Input Layer</tt> <> <tt>Hidden Layer</tt>>
     */
    private float[] weightLayer1;

    /**
     * <tt>Hidden Layer</tt>
     */
    private float[] hiddenLayer;

    /**
     * <tt>Hidden Layer</tt> <> <tt>Output Layer</tt>
     */
    private float[] weightLayer2;

    /**
     * <tt>Output Layer</tt>
     */
    private float outputLayer;

    /**
     * Current <tt>Fitness</tt> of this network
     */
    private int fitness;

    {
        // there are 4 Inputs
        final int input = 4;
        // there are 5 Hiddens
        final int hidden = 5;

        // Initialize the layers
        inputLayer = new float[input];
        hiddenLayer = new float[hidden];
        outputLayer = 0;

        // Initialize the weights
        weightLayer1 = new float[input * hidden];
        weightLayer2 = new float[hidden];
    }

    NeuralNet() {
        // Calculate the first weights totally random
        Random random = new Random();
        for (int i = 0; i < weightLayer1.length; i++)
            weightLayer1[i] = random.nextFloat() * 2 - 1f;
        for (int i = 0; i < weightLayer2.length; i++)
            weightLayer2[i] = random.nextFloat() * 2 - 1f;
    }

    NeuralNet(NeuralNet parent, float mutation) {
        // Get the base from another neural network and
        System.arraycopy(parent.getWeightLayer1(), 0, weightLayer1, 0, weightLayer1.length);
        System.arraycopy(parent.getWeightLayer2(), 0, weightLayer2, 0, weightLayer2.length);
        // Mutate the weights
        Random random = new Random();
        for (int i = 0; i < weightLayer1.length; i++)
            weightLayer1[i] += random.nextFloat() * mutation * 2 - mutation;
        for (int i = 0; i < weightLayer2.length; i++)
            weightLayer2[i] += random.nextFloat() * mutation * 2 - mutation;
    }

    /**
     * Activates the neural network with the given inputs and returns the output value
     *
     * @param input <tt>Input-Layer</tt>
     * @return Result <tt>Output-Layer</tt>
     * @throws IllegalArgumentException If the <tt>input.length</tt> and <tt>inputLayer.length</tt> do not match
     */
    float activate(float[] input) {
        if (inputLayer.length != input.length) throw new IllegalArgumentException("input.length and inputLayer.length do not match");

        System.arraycopy(input, 0, inputLayer, 0, inputLayer.length);

        // Calculate the output
        process();

        return outputLayer;
    }

    /**
     * Calculates the average between two networks
     *
     * @param net The other network
     * @return <tt>this</tt> network with modified weights
     */
    NeuralNet breed(NeuralNet net) {
        // Average Weight-Layer 1
        for (int i = 0; i < weightLayer1.length; i++)
            weightLayer1[i] = (weightLayer1[i] + net.getWeightLayer1()[i]) / 2;

        // Average Weight-Layer 2
        for (int i = 0; i < weightLayer2.length; i++)
            weightLayer2[i] = (weightLayer2[i] + net.getWeightLayer2()[i]) / 2;

        return this;
    }

    float[] getInputLayer() {
        return inputLayer;
    }

    float[] getHiddenLayer() {
        return hiddenLayer;
    }

    float getOutputLayer() {
        return outputLayer;
    }

    float[] getWeightLayer1() {
        return weightLayer1;
    }

    float[] getWeightLayer2() {
        return weightLayer2;
    }

    /**
     * Calculates the output
     */
    private void process() {
        // Hidden-Layer
        for (int hidden = 0; hidden < hiddenLayer.length; hidden++)
            hiddenLayer[hidden] = 0;

        // Calculate Hidden-Layer
        for (int hidden = 0; hidden < hiddenLayer.length; hidden++)
            for (int input = 0; input < inputLayer.length; input++)
                hiddenLayer[hidden] += inputLayer[input] * weightLayer1[hidden * inputLayer.length + input];

        // Output-Layer
        outputLayer = 0;

        // Calculate Output-Layer
        for (int hidden = 0; hidden < hiddenLayer.length; hidden++)
            outputLayer += hiddenLayer[hidden] * weightLayer2[hidden];
    }

    void resetFitness() {
        fitness = 0;
    }

    int getFitness() {
        return fitness;
    }

    void addFitness() {
        fitness++;
    }

    /**
     * Saves the network to <tt>network.dat</tt>
     *
     * @param generation Current generation
     */
    void save(int generation) {
        File file = new File("network.dat");
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            out.write(String.valueOf(generation) + "\n");
            for (float v : weightLayer1)
                out.write(String.valueOf(v) + "\n");
            for (float v : weightLayer2)
                out.write(String.valueOf(v) + "\n");
            out.flush();
            out.close();
            JOptionPane.showMessageDialog(null, "The Neural Network was saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The Neural Network could not be saved:\n" + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads a neural network from <tt>network.dat</tt> or an internal file
     *
     * @return Generation of the loaded network or -1 on error
     */
    int load() {
        File file = new File("network.dat");
        try (BufferedReader in = file.exists() ? new BufferedReader(new InputStreamReader(new FileInputStream(file))) : Gdx.files.internal("network.dat").reader(1024)) {
            int generation = Integer.parseInt(in.readLine());
            for (int i = 0; i < weightLayer1.length; i++)
                weightLayer1[i] = Float.valueOf(in.readLine().trim());
            for (int i = 0; i < weightLayer2.length; i++)
                weightLayer2[i] = Float.valueOf(in.readLine().trim());
            return generation;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The Neural Network could not be loaded:\n" + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return -1;
    }

}
