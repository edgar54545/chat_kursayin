package kursayin.team0.enums;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;

public enum Backgrounds {
    MYBACKGROUND(Colors.MYCOLOR),
    OTHERSBACKGROUND(Colors.OTHERSCOLOR);

    private final Background background;

    Backgrounds(Colors color) {
        background = new Background(new BackgroundFill(color.getColor(), new CornerRadii(8), Insets.EMPTY));
    }

    public Background getBackground() {
        return background;
    }
}