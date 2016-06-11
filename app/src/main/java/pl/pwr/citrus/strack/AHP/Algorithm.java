package pl.pwr.citrus.strack.AHP;

import java.util.ArrayList;

/**
 * Created by Luiza on 2016-06-10.
 */

import java.util.ArrayList;
import java.util.Collections;

public class Algorithm {

    private double[][] kriterionMatrix, groceryComparsionMatrix, householdComparsionMatrix, cosmeticComparsionMatrix;
    private double[] cKriterion, cGrocery, cHousehold, cCosmetics;
    private double[] sKriterion, sGrocery, sHousehold, sCosmetics;
    private boolean consistent;

    public Algorithm(double groceryToHousehold, double groceryToCosmetic, double householdToCosmetic) {
        kriterionMatrix = new double[3][3];
        kriterionMatrix[0][0] = 1;
        kriterionMatrix[0][1] = groceryToHousehold;
        kriterionMatrix[0][2] = groceryToCosmetic;

        kriterionMatrix[1][0] = 1 / groceryToHousehold;
        kriterionMatrix[1][1] = 1;
        kriterionMatrix[1][2] = householdToCosmetic;

        kriterionMatrix[2][0] = 1 / groceryToCosmetic;
        kriterionMatrix[2][1] = 1 / householdToCosmetic;
        kriterionMatrix[2][2] = 1;

        consistent = false;
    }

    public ArrayList<Store> Run(ArrayList<Store> stores) {
        initializeStores(stores);
        initalizeMatrixes(stores);
        normalizeMatrixes();
        checkConsistency();
        calculateRank(stores);
        sort(stores);
        return stores;
    }

    private void sort(ArrayList<Store> stores) {
        Collections.sort(stores);
    }

    private void calculateRank(ArrayList<Store> stores) {
        for (int i = 0; i < groceryComparsionMatrix.length; i++) {
            double rank = sKriterion[0]*sGrocery[i];
            rank +=sKriterion[1]*sHousehold[i];
            rank +=sKriterion[2]*sCosmetics[i];
            stores.get(i).setRank(rank);
        }
    }

    private void normalizeMatrixes() {
        cKriterion = sumColumns(kriterionMatrix);
        cGrocery = sumColumns(groceryComparsionMatrix);
        cHousehold = sumColumns(householdComparsionMatrix);
        cCosmetics = sumColumns(cosmeticComparsionMatrix);
        divideElements(kriterionMatrix, cKriterion);
        divideElements(groceryComparsionMatrix, cGrocery);
        divideElements(householdComparsionMatrix, cHousehold);
        divideElements(cosmeticComparsionMatrix, cCosmetics);
        sKriterion = rowsAverage(kriterionMatrix);
        sGrocery = rowsAverage(groceryComparsionMatrix);
        sHousehold = rowsAverage(householdComparsionMatrix);
        sCosmetics = rowsAverage(cosmeticComparsionMatrix);

    }

    private void checkConsistency() {
        consistent = check(cKriterion, sKriterion);
        if(consistent){
            consistent = check(cGrocery, sGrocery);
            if(consistent){
                consistent = check(cHousehold, sHousehold);
                if(consistent){
                    consistent = check(cCosmetics, sCosmetics);
                }
            }
        }
    }

    private boolean check(double[] c, double[] s) {
        double lambda = calculateLambda(c, s);
        double CI =(lambda - c.length)/(c.length-1);
        double CR = CI/getRI(c.length);
        return CR<=0.1;
    }

    private double getRI(int length) {
        double RI = 0;
        switch (length) {
            case 3:
                RI = 0.58;
                break;
            case 4:
                RI = 0.9;
                break;
            case 5:
                RI = 1.15;
                break;
            case 6:
                RI = 1.24;
                break;
            case 7:
                RI = 1.32;
                break;
            case 8:
                RI = 1.41;
                break;
            default:
                RI = 1;
        }
        return RI;
    }

    private double calculateLambda(double[] c, double[] s) {
        double sum = 0;
        for (int i = 0; i < s.length; i++) {
            sum+=(c[i]*s[i]);
        }
        return sum;
    }

    private double[] rowsAverage(double[][] matrix) {
        double[] avgRows =  new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            double result = 0;
            for (int j = 0; j < matrix.length; j++) {
                result+=matrix[i][j];
            }
            result/=matrix.length;
            avgRows[i]=result;
        }
        return avgRows;
    }

    private void divideElements(double[][] matrix, double[] columnSum) {
        for(int i=0; i<matrix.length; i++){
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j]/=columnSum[j];
            }
        }

    }

    private double[] sumColumns(double[][] matrix) {
        double [] columnSum = new double[matrix.length];
        for(int j =0; j<matrix.length; j++){
            double sum = 0;
            for(int i = 0; i< matrix.length; i++){
                sum+=matrix[i][j];
            }
            columnSum[j] = sum;
        }
        return columnSum;
    }

    private void initalizeMatrixes(ArrayList<Store> stores) {
        groceryComparsionMatrix = initaializeMatrix(1, groceryComparsionMatrix, stores);
        householdComparsionMatrix = initaializeMatrix(2, householdComparsionMatrix, stores);
        cosmeticComparsionMatrix = initaializeMatrix(3, cosmeticComparsionMatrix, stores);
    }

    private double[][] initaializeMatrix(int whichKriterion, double[][] matrix, ArrayList<Store> stores) {
        matrix = new double[stores.size()][stores.size()];
        for (int i = 0; i < stores.size(); i++) {
            for (int j = i; j < stores.size(); j++) {
                int valueA, valueB;
                switch (whichKriterion) {
                    case 1:
                        valueA = stores.get(i).getGrocery();
                        valueB = stores.get(j).getGrocery();
                        break;
                    case 2:
                        valueA = stores.get(i).getHousehold();
                        valueB = stores.get(j).getHousehold();
                        break;
                    default:
                        valueA = stores.get(i).getCosmetic();
                        valueB = stores.get(j).getCosmetic();
                        break;
                }
                if (valueA < valueB) {
                    matrix[i][j] = 1.0 / translateToOdd((int) valueB / valueA);
                } else {
                    matrix[i][j] = translateToOdd((int) valueA / valueB);
                }
            }
        }
        for (int i = 0; i < stores.size(); i++) {
            for (int j = 0; j < i; j++) {
                matrix[i][j] = 1 / matrix[j][i];
            }
        }
        return matrix;
    }

    private void initializeStores(ArrayList<Store> stores) {
        for (int i = 0; i < stores.size(); i++) {
            normalizeStore(stores.get(i));
        }
    }

    private void normalizeStore(Store store) {
        store.setGrocery(translateToOdd(store.getGrocery()));
        store.setCosmetic(translateToOdd(store.getCosmetic()));
        store.setHousehold(translateToOdd(store.getHousehold()));
    }

    private int translateToOdd(int grocery) {
        int result = 1;
        switch (grocery) {
            case 2:
                result = 3;
                break;
            case 3:
                result = 5;
                break;
            case 4:
                result = 7;
                break;
            case 5:
                result = 9;
                break;
        }
        return result;
    }

}
