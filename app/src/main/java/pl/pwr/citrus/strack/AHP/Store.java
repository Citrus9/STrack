package pl.pwr.citrus.strack.AHP;

/**
 * Created by Luiza on 2016-06-09.
 */
public class Store {

    private String name;

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
}
