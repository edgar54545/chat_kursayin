package kursayin.team0.enums;

import javafx.scene.paint.Color;

public enum Colors {
    MYCOLOR(0, 230, 0),
    OTHERSCOLOR(179, 179, 179);

    private final Color color;

    Colors(int red, int green, int blue) {
        color = Color.rgb(red, green, blue);
    }

    public Color getColor() {
        return color;
    }
}