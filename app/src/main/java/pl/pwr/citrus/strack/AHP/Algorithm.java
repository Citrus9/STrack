package pl.pwr.citrus.strack.AHP;

import java.util.ArrayList;

/**
 * Created by Luiza on 2016-06-10.
 */
public class Algorithm {
    private double[][] kriterionMatrix;

    public Algorithm(double groceryToHousehold, double groceryToCosmetic, double householdToCosmetic){
        kriterionMatrix = new double[3][3];
        kriterionMatrix[0][0]=1;
        kriterionMatrix[0][1]=groceryToHousehold;
        kriterionMatrix[0][2]=groceryToCosmetic;

        kriterionMatrix[1][0]=1/groceryToHousehold;
        kriterionMatrix[1][1]=1;
        kriterionMatrix[1][2]=householdToCosmetic;

        kriterionMatrix[2][0]=1/groceryToCosmetic;
        kriterionMatrix[2][1]=1/householdToCosmetic;
        kriterionMatrix[2][2]=1;
    }

    public ArrayList<Store> Run(ArrayList<Store> stores){
        return stores;
    }
}
