package ch.heig.gre.groupO;

import ch.heig.gre.Keys;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.MazeSolver;
import ch.heig.gre.maze.Metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class AStar implements MazeSolver {
  public enum Heuristic {
    DIJKSTRA, INFINITY_NORM, EUCLIDEAN_NORM, MANHATTAN, K_MANHATTAN
  }

  /** Heuristique utilisée pour l'algorithme A*. */
  private final Heuristic heuristic;

  /** Facteur multiplicatif de la distance de Manhattan utilisé par l'heuristique K-Manhattan. */
  private final double kManhattan;

  public AStar(Heuristic heuristic) {
    this(heuristic, 1);
  }

  public AStar(Heuristic heuristic, double kManhattan) {
    this.heuristic = heuristic;
    this.kManhattan = kManhattan;
  }

  @Override
  public Result solve(final GridGraph2D grid,
                      final PositiveWeightFunction weights,
                      final int source,
                      final int destination,
                      final VertexLabelling<Integer> distances) {

    if (source < 0 || source >= grid.nbVertices() || destination < 0 || destination >= grid.nbVertices()) {
      throw new IllegalArgumentException("Source or destination vertex id is out of bounds");
    }

    final int verticesCount = grid.nbVertices();
    final int width = grid.width();
    final int cmin = weights.minWeight();
    final int xt = destination % width;
    final int yt = destination / width;

    // lambda[i] : meilleure distance connue de source à i
    final int[] lambda = new int[verticesCount];
    Arrays.fill(lambda, Integer.MAX_VALUE);

    // predecessors[i] : prédécesseur de i sur le meilleur chemin connu
    final int[] predecessors = new int[verticesCount];
    Arrays.fill(predecessors, -1);

    // closed[i] : i a déjà été traité (peut être ré-ouvert si l'heuristique est non consistante)
    final boolean[] closed = new boolean[verticesCount];

    // File de priorité ordonnée par f(i) = lambda(i) + h(i).
    // Entrées : [priorité, sommet]
    PriorityQueue<int[]> opened = new PriorityQueue<>(Comparator.comparingLong(a -> a[0]));

    lambda[source] = 0;
    distances.setLabel(source, 0);
    opened.add(new int[]{ heuristicDst(source, xt, yt, width, cmin), source });

    int processed = 0;

    while (!opened.isEmpty()) {
      int[] top = opened.poll();
      int priority = top[0];
      int current = top[1];

      if (closed[current]) continue;
      if (priority > lambda[current] + heuristicDst(current, xt, yt, width, cmin)) continue;

      closed[current] = true;
      processed++;

      if (current == destination) break;

      for (int neighbor : grid.neighbors(current)) {
        int weight = weights.get(current, neighbor);
        int newDist = lambda[current] + weight;
        if (newDist < lambda[neighbor]) {
          lambda[neighbor] = newDist;
          predecessors[neighbor] = current;
          distances.setLabel(neighbor, newDist);
          // Si neighbor était fermé, on autorise sa ré-ouverture (cas heuristique non consistante)
          closed[neighbor] = false;
          int f = newDist + heuristicDst(neighbor, xt, yt, width, cmin);
          opened.add(new int[]{ f, neighbor });
        }
      }
    }

    // Reconstruction du chemin en remontant les marques predecessors de t vers s
    List<Integer> path = new ArrayList<>();
    if (destination == source || predecessors[destination] != -1) {
      int current = destination;
      while (current != -1) {
        path.add(current);
        if (current == source) break;
        current = predecessors[current];
      }
      Collections.reverse(path);
    }

    // Avec une heuristique non consistante, lamda[destination] peut différer de la longueur réelle
    // du chemin codé par predecessors. On recalcule donc la longueur en sommant les poids le long du chemin.
    int pathLength = 0;
    for (int i = 1; i < path.size(); i++) {
      pathLength += weights.get(path.get(i - 1), path.get(i));
    }

    Metadata meta = new Metadata();
    meta.put(Keys.LENGTH, pathLength);
    meta.put(Keys.NB_PROCESSED_VERTICES, processed);

    return new Result(path, meta);
  }

  /** Estimation heuristique de la distance restant à parcourir du sommet i à la destination. */
  private int heuristicDst(int i, int xt, int yt, int width, int cmin) {
    int xi = i % width;
    int yi = i / width;
    int dx = Math.abs(xt - xi);
    int dy = Math.abs(yt - yi);

    return switch (heuristic) {
      case DIJKSTRA -> 0;
      case INFINITY_NORM -> cmin * Math.max(dx, dy);
      case EUCLIDEAN_NORM -> (int)(cmin * Math.floor(Math.sqrt((double) dx * dx + dy * dy)));
      case MANHATTAN -> cmin * (dx + dy);
      case K_MANHATTAN -> (int)(kManhattan * cmin * (dx + dy));
    };
  }
}