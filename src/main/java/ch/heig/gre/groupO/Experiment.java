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

  public static void main(String[] args) {
    DfsGenerator generator = new DfsGenerator();

    // Les quatre heuristiques optimistes (admissibles), dans l'ordre de dominance attendu.
    AStar[] optimistic = {
        new AStar(AStar.Heuristic.DIJKSTRA),
        new AStar(AStar.Heuristic.INFINITY_NORM),
        new AStar(AStar.Heuristic.EUCLIDEAN_NORM),
        new AStar(AStar.Heuristic.MANHATTAN),
    };
    String[] optimisticNames = {"H0 (Dijkstra)", "H1 (L-inf)", "H2 (L2)", "H3 (Manhattan)"};

    System.out.printf("Grille %dx%d, source=%d, destination=%d, N=%d%n",
        SIDE, SIDE, SRC, DST, N);

    for (int e = 0; e < EXPERIMENTS.length; e++) {
      Params params = EXPERIMENTS[e];

      // --- Accumulateurs pour les heuristiques optimistes (4 algorithmes) ---
      double[] sumLength = new double[4];
      double[] sumProcessed = new double[4];
      double[] sumReductionH0 = new double[4]; // réduction % du nb de sommets traités vs H0
      double[] sumTau = new double[4];         // taux d'expansion utile

      // --- Accumulateurs pour l'étude de H4 ---
      int nbK = K_VALUES.length;
      int[] countOptimal = new int[nbK];
      double[] sumLengthK = new double[nbK];
      double[] minErr = new double[nbK];
      double[] maxErr = new double[nbK];
      double[] sumErr = new double[nbK];
      double[] sumRedAbsH3 = new double[nbK]; // réduction absolue du nb de sommets traités vs H3
      double[] sumRedRelH3 = new double[nbK]; // réduction relative (%) vs H3
      Arrays.fill(minErr, Double.MAX_VALUE);

      RandomGenerator rng = new Random(SEED);

      System.out.printf("%n[%d/%d] %s%n", e + 1, EXPERIMENTS.length, params.description());

      for (int i = 0; i < N; i++) {
        GenerationResult gen = generateGrid(generator, params, rng);
        GridGraph2D maze = gen.maze();
        PositiveWeightFunction wf = gen.weights();

        // Exécution des 4 heuristiques optimistes sur la même instance.
        int[] len = new int[4];
        int[] proc = new int[4];
        int[] pathSz = new int[4];
        for (int a = 0; a < 4; a++) {
          MazeSolver.Result r = run(optimistic[a], maze, wf);
          len[a] = r.metadata().get(Keys.LENGTH);
          proc[a] = r.metadata().get(Keys.NB_PROCESSED_VERTICES);
          pathSz[a] = r.path().size();
        }
        for (int a = 0; a < 4; a++) {
          sumLength[a] += len[a];
          sumProcessed[a] += proc[a];
          sumReductionH0[a] += 100.0 * (proc[0] - proc[a]) / proc[0];
          sumTau[a] += (double) pathSz[a] / proc[a];
        }

        // H0 et H3 étant admissibles, len[0] est la longueur optimale de référence.
        int optimalLength = len[0];
        int processedH3 = proc[3];

        // Étude de H4 sur la même instance. K=0 réutilise H0, K=1 réutilise H3.
        for (int k = 0; k < nbK; k++) {
          int length;
          int processed;
          if (K_VALUES[k] == 0.0) {
            length = len[0];
            processed = proc[0];
          } else if (K_VALUES[k] == 1.0) {
            length = len[3];
            processed = proc[3];
          } else {
            MazeSolver.Result r = run(new AStar(AStar.Heuristic.K_MANHATTAN, K_VALUES[k]), maze, wf);
            length = r.metadata().get(Keys.LENGTH);
            processed = r.metadata().get(Keys.NB_PROCESSED_VERTICES);
          }

          double err = 100.0 * (length - optimalLength) / optimalLength;
          if (length == optimalLength) countOptimal[k]++;
          sumLengthK[k] += length;
          sumErr[k] += err;
          minErr[k] = Math.min(minErr[k], err);
          maxErr[k] = Math.max(maxErr[k], err);
          sumRedAbsH3[k] += (processedH3 - processed);
          sumRedRelH3[k] += 100.0 * (processedH3 - processed) / processedH3;
        }
      }

      // --- Affichage : heuristiques optimistes ---
      System.out.println("  Heuristiques optimistes (moyennes sur N instances) :");
      System.out.printf("  %-16s %16s %18s %20s %16s%n",
          "Heuristique", "Longueur moy.", "Sommets traites", "Reduction/H0 (%)", "Tau exp. moy.");
      for (int a = 0; a < 4; a++) {
        System.out.printf("  %-16s %16.3f %18.3f %20.3f %16.4f%n",
            optimisticNames[a],
            sumLength[a] / N,
            sumProcessed[a] / N,
            sumReductionH0[a] / N,
            sumTau[a] / N);
      }

      // --- Affichage : étude de H4 ---
      System.out.println("  Heuristique H4 (K-Manhattan) :");
      System.out.printf("  %-6s %9s %16s %11s %11s %11s %18s %18s%n",
          "K", "% optim", "Longueur moy.", "Err min%", "Err moy%", "Err max%", "Reduc/H3 (abs)", "Reduc/H3 (%)");
      for (int k = 0; k < nbK; k++) {
        System.out.printf("  %-6.2f %9.2f %16.3f %11.4f %11.4f %11.4f %18.3f %18.3f%n",
            K_VALUES[k],
            100.0 * countOptimal[k] / N,
            sumLengthK[k] / N,
            minErr[k],
            sumErr[k] / N,
            maxErr[k],
            sumRedAbsH3[k] / N,
            sumRedRelH3[k] / N);
      }
    }
  }

  /**
   * Exécute un solveur sur un labyrinthe donné, entre la source et la destination fixées,
   * avec un étiquetage de distances neuf.
   *
   * @param solver Solveur à exécuter.
   * @param maze   Labyrinthe à résoudre.
   * @param wf     Fonction de pondération associée.
   * @return Le résultat de la résolution (chemin + métadonnées).
   */
  private static MazeSolver.Result run(MazeSolver solver, GridGraph2D maze, PositiveWeightFunction wf) {
    return solver.solve(maze, wf, SRC, DST, new DistanceLabelling(maze.nbVertices()));
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
