package ch.heig.gre.groupO;

import ch.heig.gre.Keys;
import ch.heig.gre.graph.GridGraph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.maze.DistanceLabelling;
import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.MazeSolver;
import ch.heig.gre.maze.impl.GridMazeBuilder;
import ch.heig.gre.maze.impl.MazeTuner;
import ch.heig.gre.maze.impl.ShenaniganWeightFunction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.random.RandomGenerator;

public final class Experiment {
  /** Dimension de la grille (carrée) */
  private static final int SIDE = 1100;

  /** Sommets source et destination pour les expériences */
  private static final int SRC = 550500;
  private static final int DST = 660600;

  /** Nombre de grilles à générer pour chaque expérience */
  private static final int N = 100;

  /** Graine du générateur aléatoire (reproductibilité du relief d'une exécution à l'autre). */
  private static final long SEED = 2026;

  /** Valeurs du facteur K évaluées pour l'heuristique H4 (K-Manhattan). K=0 -> H0, K=1 -> H3. */
  private static final double[] K_VALUES = {0.0, 0.5, 1.0, 2.0, 4.0, 6.0, 8.0};

  /** Topologie de la grille */
  private static final GridGraph2D TOPOLOGY;

  /** Expériences à réaliser */
  private static final Params[] EXPERIMENTS = {
      new Params(
          "Relief très peu dense, labyrinthe très ouvert",
          0, 0.15, 20, 1, 20),
      new Params(
          "Relief très peu dense, labyrinthe assez ouvert",
          0, 0.1, 20, 1, 20),
      new Params(
          "Relief très peu dense, labyrinthe peu ouvert",
          0, 0.01, 20, 1, 20),
      new Params(
          "Relief dense, labyrinthe moyennement ouvert",
          0.25, 0.05, 25, 5, 20),
      new Params(
          "Relief très dense, labyrinthe moyennement ouvert",
          0.5, 0.05, 25, 5, 20),
      new Params(
          "Relief très dense et fortement pondéré, labyrinthe moyennement ouvert",
          0.5, 0.05, 25, 5, 100)
  };

  /**
   * <p>Paramètres d'une expérience, avec une description approximative de leurs effets sur la génération.</p>
   *
   * <p>À passer en paramètre de la méthode {@link #generateGrid} pour générer un labyrinthe.</p>
   *
   * @param description            Description de l'expérience
   * @param reliefDensityFactor    Facteur de densité du relief
   * @param wallRemovalProbability Probabilité de suppression d'un mur lors de la génération du relief
   * @param reliefRadiusRatio      Ratio du rayon de la zone de relief par rapport à la taille du labyrinthe
   * @param reliefSummitsPerRange  Nombre de sommets de relief générés par chaîne de montagnes
   * @param reliefMaxSummitWeight  Poids maximal d'un sommet de relief
   */
  record Params(String description,
                double reliefDensityFactor,
                double wallRemovalProbability,
                double reliefRadiusRatio,
                int reliefSummitsPerRange,
                int reliefMaxSummitWeight
  ) {}

  static {
    var g = new GridGraph(SIDE);
    GridGraph.bindAll(g);
    TOPOLOGY = g;
  }

  // Indices des heuristiques admissibles servant de références : H0 (Dijkstra, K=0) et H3 (Manhattan, K=1).
  private static final int H0_INDEX = 0;
  private static final int H3_INDEX = 3;

  public static void main(String[] args) {
    MazeGenerator mazeGenerator = new DfsGenerator();

    // Les quatre heuristiques optimistes (admissibles), dans l'ordre de dominance attendu.
    AStar[] optimisticSolvers = {
        new AStar(AStar.Heuristic.DIJKSTRA),
        new AStar(AStar.Heuristic.INFINITY_NORM),
        new AStar(AStar.Heuristic.EUCLIDEAN_NORM),
        new AStar(AStar.Heuristic.MANHATTAN),
    };
    String[] optimisticLabels = {"H0 (Dijkstra)", "H1 (L-inf)", "H2 (L2)", "H3 (Manhattan)"};
    int nbOptimisticHeuristics = optimisticSolvers.length;
    int nbKValues = K_VALUES.length;

    System.out.printf(Locale.US, "Grille %dx%d, source=%d, destination=%d, N=%d%n",
        SIDE, SIDE, SRC, DST, N);

    for (int experimentIndex = 0; experimentIndex < EXPERIMENTS.length; experimentIndex++) {
      Params params = EXPERIMENTS[experimentIndex];

      // Accumulateurs pour les heuristiques optimistes (4 algorithmes)
      double[] sumLength = new double[nbOptimisticHeuristics];
      double[] sumProcessedVertices = new double[nbOptimisticHeuristics];
      double[] sumReductionVsH0 = new double[nbOptimisticHeuristics];       // réduction % du nb de sommets traités vs H0
      double[] sumUsefulExpansionRate = new double[nbOptimisticHeuristics]; // tau = |chemin| / |sommets traites|

      // Accumulateurs de l'étude de H4 (un par valeur de K).
      int[] optimalSolutionCount = new int[nbKValues];
      double[] sumLengthH4 = new double[nbKValues];
      double[] minRelativeError = new double[nbKValues];
      double[] maxRelativeError = new double[nbKValues];
      double[] sumRelativeError = new double[nbKValues];
      double[] sumAbsoluteReductionVsH3 = new double[nbKValues];// réduction absolue du nb de sommets traités vs H3
      double[] sumRelativeReductionVsH3 = new double[nbKValues];// réduction relative (%) vs H3
      Arrays.fill(minRelativeError, Double.MAX_VALUE);


      RandomGenerator rng = new Random(SEED);

      System.out.printf(Locale.US, "%n[%d/%d] %s%n",
          experimentIndex + 1, EXPERIMENTS.length, params.description());

      for (int instanceIndex = 0; instanceIndex < N; instanceIndex++) {
        GenerationResult generation = generateGrid(mazeGenerator, params, rng);
        GridGraph2D maze = generation.maze();
        PositiveWeightFunction weights = generation.weights();

        // ----- Heuristiques optimistes : on les exécute toutes sur la même instance -----
        SolveStats[] optimisticStats = new SolveStats[nbOptimisticHeuristics];
        for (int heuristicIndex = 0; heuristicIndex < nbOptimisticHeuristics; heuristicIndex++) {
          optimisticStats[heuristicIndex] = SolveStats.from(solveMaze(optimisticSolvers[heuristicIndex], maze, weights));
        }
        int processedVerticesByH0 = optimisticStats[H0_INDEX].processedVertices();
        for (int heuristicIndex = 0; heuristicIndex < nbOptimisticHeuristics; heuristicIndex++) {
          SolveStats stats = optimisticStats[heuristicIndex];
          sumLength[heuristicIndex] += stats.pathLength();
          sumProcessedVertices[heuristicIndex] += stats.processedVertices();
          sumReductionVsH0[heuristicIndex] += 100.0 * (processedVerticesByH0 - stats.processedVertices()) / processedVerticesByH0;
          sumUsefulExpansionRate[heuristicIndex] += (double) stats.pathSize() / stats.processedVertices();
        }

        // H0 et H3 sont admissibles : leur longueur est la longueur optimale de référence.
        int optimalLength = optimisticStats[H0_INDEX].pathLength();
        int processedVerticesByH3 = optimisticStats[H3_INDEX].processedVertices();

        //Étude de H4 sur la même instance (K=0 ≡ H0, K=1 ≡ H3 : réutilisation pour éviter un re-solve)
        for (int kIndex = 0; kIndex < nbKValues; kIndex++) {
          double k = K_VALUES[kIndex];
          SolveStats stats;
          if (k == 0.0) {
            stats = optimisticStats[H0_INDEX];
          } else if (k == 1.0) {
            stats = optimisticStats[H3_INDEX];
          } else {
            stats = SolveStats.from(solveMaze(new AStar(AStar.Heuristic.K_MANHATTAN, k), maze, weights));
          }

          double relativeErrorPercent = 100.0 * (stats.pathLength() - optimalLength) / optimalLength;
          if (stats.pathLength() == optimalLength) optimalSolutionCount[kIndex]++;
          sumLengthH4[kIndex] += stats.pathLength();
          sumRelativeError[kIndex] += relativeErrorPercent;
          minRelativeError[kIndex] = Math.min(minRelativeError[kIndex], relativeErrorPercent);
          maxRelativeError[kIndex] = Math.max(maxRelativeError[kIndex], relativeErrorPercent);
          sumAbsoluteReductionVsH3[kIndex] += processedVerticesByH3 - stats.processedVertices();
          sumRelativeReductionVsH3[kIndex] += 100.0 * (processedVerticesByH3 - stats.processedVertices()) / processedVerticesByH3;
        }
      }


      // Affichage : heuristiques optimistes
      System.out.println("  Heuristiques optimistes (moyennes sur N instances) :");
      System.out.printf(Locale.US, "  %-16s %16s %18s %20s %16s%n",
          "Heuristique", "Longueur moy.", "Sommets traites", "Reduction/H0 (%)", "Tau exp. moy.");
      for (int heuristicIndex = 0; heuristicIndex < nbOptimisticHeuristics; heuristicIndex++) {
        System.out.printf(Locale.US, "  %-16s %16.3f %18.3f %20.3f %16.4f%n",
            optimisticLabels[heuristicIndex],
            sumLength[heuristicIndex] / N,
            sumProcessedVertices[heuristicIndex] / N,
            sumReductionVsH0[heuristicIndex] / N,
            sumUsefulExpansionRate[heuristicIndex] / N);
      }

      // Affichage : étude de H4
      System.out.println("  Heuristique H4 (K-Manhattan) :");
      System.out.printf(Locale.US, "  %-6s %9s %16s %11s %11s %11s %18s %18s%n",
          "K", "% optim", "Longueur moy.", "Err min%", "Err moy%", "Err max%", "Reduc/H3 (abs)", "Reduc/H3 (%)");
      for (int kIndex = 0; kIndex < nbKValues; kIndex++) {
        System.out.printf(Locale.US, "  %-6.2f %9.2f %16.3f %11.4f %11.4f %11.4f %18.3f %18.3f%n",
            K_VALUES[kIndex],
            100.0 * optimalSolutionCount[kIndex] / N,
            sumLengthH4[kIndex] / N,
            minRelativeError[kIndex],
            sumRelativeError[kIndex] / N,
            maxRelativeError[kIndex],
            sumAbsoluteReductionVsH3[kIndex] / N,
            sumRelativeReductionVsH3[kIndex] / N);
      }
    }
  }

  /**
   * Métriques extraites du résultat d'une résolution, pour une instance donnée.
   *
   * @param pathLength        Longueur (somme des poids) du chemin trouvé.
   * @param processedVertices Nombre de sommets retirés de la file de priorité et dont les voisins ont été relâchés.
   * @param pathSize          Nombre de sommets composant le chemin trouvé.
   */
  private record SolveStats(int pathLength, int processedVertices, int pathSize) {
    static SolveStats from(MazeSolver.Result result) {
      return new SolveStats(
          result.metadata().get(Keys.LENGTH),
          result.metadata().get(Keys.NB_PROCESSED_VERTICES),
          result.path().size()
      );
    }
  }

  /**
   * Exécute un solveur sur un labyrinthe donné, entre la source et la destination fixées,
   * avec un étiquetage de distances neuf.
   *
   * @param solver  Solveur à exécuter.
   * @param maze    Labyrinthe à résoudre.
   * @param weights Fonction de pondération associée.
   * @return Le résultat de la résolution (chemin + métadonnées).
   */
  private static MazeSolver.Result solveMaze(MazeSolver solver, GridGraph2D maze, PositiveWeightFunction weights) {
    return solver.solve(maze, weights, SRC, DST, new DistanceLabelling(maze.nbVertices()));
  }

  /**
   * Résultat de la méthode {@link #generateGrid}, fournit un labyrinthe et une fonction de pondération associée.
   *
   * @param maze    labyrinthe généré
   * @param weights Fonction de pondération associée
   */
  private record GenerationResult(GridGraph2D maze, PositiveWeightFunction weights) {}

  /**
   * Génère un labyrinthe en forme de grille avec un générateur donné et des réglages spécifiques pour le relief et
   * l'ouverture (i.e. densité de murs) du labyrinthe.
   *
   * @param generator Générateur de labyrinthe.
   * @param params    Paramètres de réglage du relief et de l'ouverture du labyrinthe.
   * @param rng       Générateur de nombres aléatoires.
   * @return Un {@link GenerationResult} contenant le labyrinthe et la fonction de pondération associée.
   */
  private static GenerationResult generateGrid(MazeGenerator generator, Params params, RandomGenerator rng) {
    GridGraph maze = new GridGraph(SIDE);

    MazeBuilder builder = new GridMazeBuilder(TOPOLOGY, maze);
    generator.generate(builder, 0);

    MazeTuner tuner = new MazeTuner()
        .setRandomGenerator(rng)
        .setReliefDensityFactor(params.reliefDensityFactor())
        .setWallRemovalProbability(params.wallRemovalProbability())
        .setReliefRadiusRatio(params.reliefRadiusRatio())
        .setReliefSummitsPerRange(params.reliefSummitsPerRange())
        .setReliefMaxSummitWeight(params.reliefMaxSummitWeight());

    tuner.removeWalls(TOPOLOGY, maze);
    int[] weights = tuner.generateRelief(SIDE, SIDE);
    PositiveWeightFunction wf = new ShenaniganWeightFunction(weights, tuner.getReliefMinWeight());

    return new GenerationResult(maze, wf);
  }
}
