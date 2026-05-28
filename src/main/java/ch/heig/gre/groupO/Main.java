package ch.heig.gre.groupO;

import ch.heig.gre.gui.ChoiceItem;
import ch.heig.gre.gui.Config;
import ch.heig.gre.gui.MainViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainViewController.class.getResource("mainView.fxml"));
        Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent, 1100, 840);
        stage.setTitle("Wendy, I'm home.");
        stage.setScene(scene);

        MainViewController controller = fxmlLoader.getController();
        controller.init(
                new Config()
                        .setGeneratorRequiresWalls(true)
                        .setAllowsWeightsTuning(false)
                        .setSolverColorFn(Config.DISTANCE_BASED_SOLVER_COLOR_FN),
                new DfsGenerator(),
                List.of(new ChoiceItem<>("AStar", new AStar(AStar.Heuristic.EUCLIDEAN_NORM))));

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
