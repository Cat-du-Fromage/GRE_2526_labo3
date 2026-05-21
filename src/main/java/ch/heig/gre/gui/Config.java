package ch.heig.gre.gui;

import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.gui.impl.ObservableMaze;
import ch.heig.gre.maze.MazeSolver;
import ch.heig.gre.maze.impl.ShenaniganWeightFunction;
import javafx.scene.paint.Color;

public final class Config {
  @FunctionalInterface
  public interface SolverColorFn {
    Color getColor(Config config,
                   int source,
                   int destination,
                   int[] weights,
                   VertexLabelling<Integer> distances,
                   int[] pathPredecessors,
                   PositiveWeightFunction wf,
                   int v);
  }

  public static final SolverColorFn WEIGHT_BASED_SOLVER_COLOR_FN = Config::weightBasedSolverColorFn;
  public static final SolverColorFn DISTANCE_BASED_SOLVER_COLOR_FN = Config::distanceBasedSolverColorFn;

  private boolean allowsWeightsTuning;
  private boolean generatorRequiresWalls;
  private SolverColorFn solverColorFn = WEIGHT_BASED_SOLVER_COLOR_FN;

  public int startPoint(ObservableMaze graph) {
    return 0;
  }

  public int wallThickness(int cellSide) {
    return Math.max(cellSide / 20, 1);
  }

  public Color wallColor() {
    return Color.BLACK;
  }

  public int minWeight() {
    return 5;
  }

  public int maxWeight() {
    return 25;
  }

  public boolean allowsWallsTuning() {
    return true;
  }

  public boolean allowsWeightsTuning() {
    return allowsWeightsTuning;
  }

  public boolean generatorRequiresWalls() {
    return generatorRequiresWalls;
  }

  public PositiveWeightFunction weightFunction(int[] vertexWeights) {
    return new ShenaniganWeightFunction(vertexWeights, minWeight());
  }

  public Color generatorColor(ObservableMaze maze, int v) {
    return switch (maze.getLabel(v)) {
      case PROCESSED -> Color.WHITE;
      case PENDING -> Color.BLACK;
      case PROCESSING -> Color.RED;
    };
  }

  public Color tuningColor(int[] weights, int v) {
    double value = (weights[v] - minWeight()) / (double) (maxWeight() - minWeight());
    return Color.WHITE.interpolate(Color.DARKGREEN, value);
  }

  public Color solverCellColor(int source,
                               int destination,
                               int[] weights,
                               VertexLabelling<Integer> distances,
                               int[] pathPredecessors,
                               PositiveWeightFunction wf,
                               int v) {
    return solverColorFn.getColor(this, source, destination, weights, distances, pathPredecessors, wf, v);
  }

  // Fluent setters

  public Config setGeneratorRequiresWalls(boolean value) {
    generatorRequiresWalls = value;
    return this;
  }

  public Config setAllowsWeightsTuning(boolean value) {
    allowsWeightsTuning = value;
    return this;
  }

  public Config setSolverColorFn(SolverColorFn fn) {
    solverColorFn = fn;
    return this;
  }

  // Static helpers

  private static Color weightBasedSolverColorFn(Config config,
                                               int from,
                                               int to,
                                               int[] weights,
                                               VertexLabelling<Integer> distances,
                                               int[] pathPredecessors,
                                               PositiveWeightFunction wf,
                                               int v) {
    if (pathPredecessors[v] != -1) {
      double value = (wf.get(pathPredecessors[v], v) - config.minWeight())
          / (double) (config.maxWeight() - config.minWeight());
      return Color.NAVAJOWHITE.interpolate(Color.DARKRED, value);
    }

    if (distances.getLabel(v) < Integer.MAX_VALUE) {
      double value = (weights[v] - config.minWeight()) / (double) (config.maxWeight() - config.minWeight());
      return Color.PALETURQUOISE.interpolate(Color.DARKSLATEGRAY, value);
    }

    return config.tuningColor(weights, v);
  }

  private static Color distanceBasedSolverColorFn(Config config,
                                               int from,
                                               int to,
                                               int[] weights,
                                               VertexLabelling<Integer> distances,
                                               int[] pathPredecessors,
                                               PositiveWeightFunction wf,
                                               int v) {
    double n = weights.length / 2.5;
    if (pathPredecessors[v] != -1) {
      return Color.NAVAJOWHITE.interpolate(Color.DARKRED, distances.getLabel(v) / n);
    }

    if (distances.getLabel(v) < Integer.MAX_VALUE) {
      return Color.PALETURQUOISE.interpolate(Color.DARKSLATEGRAY, distances.getLabel(v) / n);
    }

    return config.tuningColor(weights, v);
  }
}
