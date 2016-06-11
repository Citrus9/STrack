package pl.pwr.citrus.strack.AHP;

/**
 * Created by Luiza on 2016-06-09.
 */
public class Store implements Comparable<Store> {

    private String name;
    private double rank;
    private String lokalization;

    public String getLokalization() {
        return lokalization;
    }

    public void setLokalization(String lokalization) {
        this.lokalization = lokalization;
    }

    public int getGrocery() {
        return grocery;
    }

    public void setGrocery(int grocery) {
        this.grocery = grocery;
    }

    public int getHousehold() {
        return household;
    }

    public void setHousehold(int household) {
        this.household = household;
    }

    public int getCosmetic() {
        return cosmetic;
    }

    public void setCosmetic(int cosmetic) {
        this.cosmetic = cosmetic;
    }

    private int grocery, household, cosmetic;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRank(double rank){
        this.rank = rank;
    }

    public double getRank(){
        return rank;
    }

    @Override
    public int compareTo(Store s) {
        return this.rank<s.getRank()?1:-1;
    }
}
