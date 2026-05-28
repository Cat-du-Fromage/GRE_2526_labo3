package ch.heig.gre.groupO;

import ch.heig.gre.Keys;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.MazeSolver;
import ch.heig.gre.maze.Metadata;

import java.util.*;

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
    if (source < 0 || source >= grid.nbVertices()
        || destination < 0 || destination >= grid.nbVertices()) {
      throw new IllegalArgumentException("Source or destination vertex id is out of bounds");
    }

    // Nombre de sommets dans la grille
    int n = grid.nbVertices();
    // Largeur de la grille (nombre de colonnes)
    int width = grid.width();
    // Poids minimum d'un arc dans la grille
    int cmin = weights.minWeight();
    // Infini pour les calculs de distance
    int INFINITY = Integer.MAX_VALUE / 2;

    // lambda[v] = meilleure distance connue de la source jusqu'à v
    int[] lambda = new int[n];
    Arrays.fill(lambda, INFINITY); // Initialisation à l'infini pour tous les sommets
    lambda[source] = 0; // La distance de la source à elle-même est 0

    // Précécesseurs afin de reconstruire le chemin à la fin
    int[] predecessors = new int[n];
    Arrays.fill(predecessors, -1); // -1 UNREACHABLE

    // Sommet retiré de PriorityQueue
    boolean[] removed = new boolean[n];

    // f[v] = lambda[v] + h(v)
    // h(v) est l'heuristique de v
    PriorityQueue<int[]> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    priorityQueue.offer(new int[]{searchHeuristique(source, destination, width, cmin), source});
    // La source est à distance 0
    distances.setLabel(source, 0);

    int nbrProcessed = 0;

    while(!priorityQueue.isEmpty()) {
      int[] top = priorityQueue.poll();
      int u = top[1];

      // Si le sommet a déjà été traité, on l'ignore
      if(removed[u]) continue;
      removed[u] = true;
      nbrProcessed++;

      // On a trouvé la destination, on peut arrêter l'algorithme
      if(u == destination) break;

      for(int v : grid.neighbors(u)) {
        // Ignorer le voisin déhà traité
        if (removed[v]) continue;

        int newDistance = lambda[u] + weights.get(u, v);
        // Si on a trouvé une meilleure distance pour v, on met à jour lambda[v],
        // le prédécesseur de v, et la distance dans distances
        if (newDistance < lambda[v]) {
          lambda[v] = newDistance;
          predecessors[v] = u;
          distances.setLabel(v, newDistance);
          // Calcul de f(v) = lambda[v] + h(v) et ajout de v dans la PriorityQueue
          int f = newDistance + searchHeuristique(v, destination, width, cmin);
          priorityQueue.offer(new int[]{f, v});
        }
      }
    }

    // Reconstuction du chemin
    List<Integer> path = new ArrayList<>();
    int current = destination;
    while(current != -1) {
      path.add(current);
      current = predecessors[current];
    }
    // On a construit le chemin à l'envers, on doit le retourner
    Collections.reverse(path);

    // Calcul de la longueur réelle en remontant le chemin
    int length = 0;
    for (int i = 1; i < path.size(); i++) {
      // path.get(i) est le sommet acutel
      length += weights.get(path.get(i - 1), path.get(i));
    }

    // Création des métadonnées
    Metadata metadata = new Metadata();
    metadata.put(Keys.LENGTH, length);
    metadata.put(Keys.NB_PROCESSED_VERTICES, nbrProcessed);

    return new Result(path, metadata);
  }

  /**
   * Calcule l'heuristique h(v) pour un sommet v donné, en fonction de la destination,
   * de la largeur de la grille et du poids minimum des arcs.
   *
   * @param v           Le sommet pour lequel calculer l'heuristique.
   * @param destination Le sommet de destination.
   * @param width       La largeur de la grille (nombre de colonnes).
   * @param cmin        Le poids minimum d'un arc dans la grille.
   * @return L'heuristique h(v) calculée selon le type d'heuristique choisi.
   */
  private int searchHeuristique(int v, int destination, int width, int cmin) {
    // coordonées de v
    int vx = v % width;
    int vy = v / width;
    // coordonées de la destination
    int tx = destination % width;
    int ty = destination / width;
    // distance de Manhattan
    int dx = Math.abs(vx - tx);
    int dy = Math.abs(vy - ty);

    return switch(heuristic) {
        case DIJKSTRA -> 0;
        case INFINITY_NORM -> cmin * Math.max(dx, dy);
        case EUCLIDEAN_NORM -> (int) (cmin * Math.sqrt(dx * dx + dy * dy));
        case MANHATTAN -> cmin * (dx + dy);
        case K_MANHATTAN -> (int) (kManhattan * cmin * (dx + dy));
    };
  }
}