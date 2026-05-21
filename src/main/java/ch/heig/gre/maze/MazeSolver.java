package ch.heig.gre.maze;

import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;

import java.util.List;

/**
 * Solver de labyrinthes
 */
@FunctionalInterface
public interface MazeSolver {
  /**
   * Résultat du solveur de labyrinthes.
   *
   * @param path Liste de sommets du graphe représentant l'itinéraire trouvé entre la source et la destination du
   *             labyrinthe. La liste doit être ordonnée du sommet source au sommet destination, et doit inclure les deux
   *             extrémités (éventuellement confondues). La liste peut ou non être modifiable.
   * @param metadata Métadonnées associées à la résolution du labyrinthe (ex. longueur de la chaîne, nombre de sommets
   *                 traités, etc.). Les métadonnées sont optionnelles et peuvent être vides.
   */
  record Result(List<Integer> path, Metadata metadata) {}

  /**
   * <p>Détermine un itinéraire entre deux cellules d'un labyrinthe en forme de grille rectangulaire, dont les arêtes
   * sont pondérées par des poids strictement positifs.</p>
   *
   * <p>L'itinéraire retourné est la liste ordonnée de tous les sommets parcourus depuis {@code source}
   * jusqu'à {@code destination} (tous deux inclus).</p>
   *
   * <p>Le paramètre d'entrée-sortie {@code distances} est utilisé pour stocker les distances des sommets traités à la
   * source (ou à la destination), et doit être mis à jour par l'implémentation. Toutes les distances de l'instance
   * fournie sont initialisées à {@link java.lang.Integer#MAX_VALUE}.</p>
   *
   * <p>Des paramètres incohérents (ex. graphe non connexe, {@code distances} mal initialisé, etc.) peuvent entraîner
   * des comportements indéterminés et ne sont pas à gérer explicitement par l'implémentation.</p>
   *
   * @param grid        Un {@link GridGraph2D} représentant le labyrinthe.
   * @param weights     Fonction de pondération des arêtes du labyrinthe (poids strictement positifs).
   * @param source      Sommet de départ.
   * @param destination Sommet de destination.
   * @param distances   Distances des sommets à la source ou destination (entrée-sortie).
   * @return Un {@link Result} contenant l'itinéraire trouvé et des métadonnées associées à la résolution du labyrinthe.
   *
   * @throws NullPointerException     si {@code grid}, {@code weights} ou {@code distances} sont {@code null}.
   * @throws IllegalArgumentException si {@code source} ou  {@code destination} ne sont pas des sommets de
   *                                  {@code graph}.
   */
  Result solve(GridGraph2D grid,
               PositiveWeightFunction weights,
               int source,
               int destination,
               VertexLabelling<Integer> distances);
}
